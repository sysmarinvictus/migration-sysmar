package br.gov.mandaguari.saude.seguranca.service;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.seguranca.domain.Usuario;
import br.gov.mandaguari.saude.seguranca.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Real SAU_USU-backed {@link UserDetailsService} — replaces the dev stub in non-dev profiles.
 *
 * <p><b>Profile wiring (CRITICAL):</b> active only when NOT {@code test}/{@code local}, while
 * {@link br.gov.mandaguari.saude.security.DevUserDetailsService} is active ONLY in those two profiles.
 * Exactly one {@code UserDetailsService} bean exists per profile → no ambiguous-bean failure, and the
 * existing 542-test suite (which runs under {@code test}) keeps using admin/admin123.
 *
 * <p><b>Username = UsuCod (string).</b> The existing {@code AuthController} issues the JWT with
 * {@code sub = userDetails.getUsername()}; returning the numeric UsuCod makes {@code sub=UsuCod}
 * (OQ7) and lets {@code AuditService} resolve the actor (logusucod) from the principal. Login is
 * still by login name: {@code loadUserByUsername} is also tolerant of the numeric id (refresh path).
 *
 * <p><b>Password (Resolved decisions):</b> usable only when bridged — {@code UsuKey IS NULL} →
 * {@code UsuSen} is a bcrypt hash that {@code AuthController}'s {@code encoder.matches} verifies. A
 * non-null {@code UsuKey} means "not migrated": the account is disabled so login fails (the app does
 * NOT attempt legacy decrypt — that needs the GeneXus runtime; the offline bridge handles migration).
 *
 * <p><b>Blocked (R5):</b> {@code UsuBloq == 1} → account locked.
 *
 * <p><b>Authorities (coarse, OQ7):</b> see {@link #authorities(Usuario)}.
 */
@Service
@Profile("!test & !local")
public class SauUsuUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(SauUsuUserDetailsService.class);

    private final UsuarioRepository repo;
    private final AuditService audit;
    private final AuthProperties props;

    public SauUsuUserDetailsService(UsuarioRepository repo, AuditService audit, AuthProperties props) {
        this.repo = repo;
        this.audit = audit;
        this.props = props;
    }

    @Override
    @Transactional   // R7 best-effort stamp + login audit happen in the same tx as the read
    public UserDetails loadUserByUsername(String username) {
        Usuario u = resolve(username);

        // R1/Resolved: only bridged users (bcrypt, UsuKey NULL) can authenticate here. A present UsuKey
        // means the legacy reversible password was not migrated → disable so login fails cleanly
        // ("redefina sua senha" semantics). We never attempt legacy decrypt in the app.
        boolean migrated = u.getChaveSenha() == null || u.getChaveSenha().isBlank();
        boolean blocked = u.getBloqueado() != null && u.getBloqueado() == 1;   // R5
        boolean hasPassword = u.getSenha() != null && !u.getSenha().isBlank();

        // R7: stamp last access (best-effort, non-blocking) + LOGIN audit (closes OQ5). Done on load;
        // AuthController calls loadUserByUsername exactly once per successful-or-attempted login. We only
        // stamp/audit when the account is actually usable, so failed lookups don't produce noise.
        if (migrated && hasPassword && !blocked) {
            try {
                u.setUltimoAcesso(LocalDate.now());          // R7 (best-effort)
                repo.save(u);
            } catch (RuntimeException ex) {
                log.warn("could not stamp UsuDataUltimoAcesso for usucod={} (login proceeds): {}",
                        u.getUsuCod(), ex.toString());
            }
            audit.record("LOGIN", "SAU_USU", u.getUsuCod()); // OQ5 login audit
        }

        // password is only handed to the bcrypt matcher when migrated; otherwise a sentinel that can
        // never match, plus disabled=true, so login fails regardless.
        String password = migrated && hasPassword ? u.getSenha() : "{noop}__nao_migrado__";

        return User.withUsername(String.valueOf(u.getUsuCod()))
                .password(password)
                .disabled(!migrated)            // not-migrated → "redefina sua senha"
                .accountLocked(blocked)         // R5
                .authorities(authorities(u))
                .build();
    }

    /** Login by login-name (primary) or by numeric UsuCod (refresh path, where sub=UsuCod). */
    private Usuario resolve(String username) {
        try {
            var byLogin = repo.findByLogin(username);
            if (byLogin.isPresent()) return byLogin.get();
        } catch (IncorrectResultSizeDataAccessException dup) {
            // OQ10/R3: duplicate logins exist (no DB UNIQUE) → ambiguous → generic failure, no enumeration.
            log.warn("ambiguous login '{}' (duplicate UsuLogin rows) — denying", username);
            throw new UsernameNotFoundException(username);
        }
        if (username != null && username.matches("\\d+")) {
            return repo.findById(Integer.valueOf(username))
                    .orElseThrow(() -> new UsernameNotFoundException(username));
        }
        throw new UsernameNotFoundException(username);
    }

    /**
     * Coarse authority mapping (OQ7; fine per-program Inc/Alt/Exc/Con RBAC is DEFERRED to a later
     * authorization slice). Spring {@code hasRole('X')} expects {@code ROLE_X}.
     * <ul>
     *   <li>UsuSysmar=true (R2) ⇒ ROLE_SUPERUSER + ROLE_SAUDE_ADMIN + ROLE_SAUDE_CADASTRO (grants all,
     *       so it bypasses every {@code @PreAuthorize hasRole}). NOT the magic login=='SYSMAR' path.</li>
     *   <li>otherwise a non-blocked user ⇒ ROLE_SAUDE_CADASTRO (the role every migrated slice requires);</li>
     *   <li>if the SAU_PRF profile name looks like an admin profile (configurable substring) ⇒
     *       add ROLE_SAUDE_ADMIN + ROLE_ADMIN.</li>
     * </ul>
     */
    private List<GrantedAuthority> authorities(Usuario u) {
        List<GrantedAuthority> auth = new ArrayList<>();
        if (Boolean.TRUE.equals(u.getSuperusuario())) {                 // R2
            auth.add(new SimpleGrantedAuthority("ROLE_SUPERUSER"));
            auth.add(new SimpleGrantedAuthority("ROLE_SAUDE_ADMIN"));
            auth.add(new SimpleGrantedAuthority("ROLE_SAUDE_CADASTRO"));
            return auth;
        }
        auth.add(new SimpleGrantedAuthority("ROLE_SAUDE_CADASTRO"));     // baseline for any valid user
        if (isAdminProfile(u.getPerfilId())) {                          // OQ8
            auth.add(new SimpleGrantedAuthority("ROLE_SAUDE_ADMIN"));
            auth.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return auth;
    }

    /** OQ8: read SAU_PRF (read-only) to decide admin elevation. Defensive: any error → not admin. */
    private boolean isAdminProfile(Integer perfilId) {
        if (perfilId == null || perfilId == 0) return false;
        try {
            return repo.findPerfilNome(perfilId)
                    .map(n -> n != null && n.toUpperCase().contains(props.getAdminProfileMatch().toUpperCase()))
                    .orElse(false);
        } catch (RuntimeException ex) {
            log.warn("could not read SAU_PRF for prfcod={} (no admin elevation): {}", perfilId, ex.toString());
            return false;
        }
    }

    /** Binds {@link AuthProperties} without app-wide scanning. */
    @Configuration
    @EnableConfigurationProperties(AuthProperties.class)
    static class AuthConfig {}
}
