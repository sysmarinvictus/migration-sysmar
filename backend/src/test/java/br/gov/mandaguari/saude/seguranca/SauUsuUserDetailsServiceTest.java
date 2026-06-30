package br.gov.mandaguari.saude.seguranca;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.seguranca.domain.Usuario;
import br.gov.mandaguari.saude.seguranca.repository.UsuarioRepository;
import br.gov.mandaguari.saude.seguranca.service.AuthProperties;
import br.gov.mandaguari.saude.seguranca.service.SauUsuUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the real SAU_USU-backed {@link SauUsuUserDetailsService} — the keystone login path
 * (R1/R2/R5/R7, OQ7/OQ8). Verifies the bridge "migrated" marker, the blocked gate, the coarse authority
 * mapping (SYSMAR→ROLE_SUPERUSER, admin-profile elevation), generic-failure on ambiguous/absent logins
 * (no user enumeration, R3/OQ10), and the best-effort last-access stamp + LOGIN audit (OQ5).
 *
 * <p>Pure Mockito (no Spring context) — runs under the {@code test} profile without activating the bean,
 * so the existing 542-test suite (dev-stub auth) is unaffected.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SauUsuUserDetailsServiceTest {

    /** A real bcrypt hash standing in for a bridged UsuSen (UsuKey NULL). */
    static final String BCRYPT = new BCryptPasswordEncoder().encode("qualquer");

    @Mock UsuarioRepository repo;
    @Mock AuditService audit;
    final AuthProperties props = new AuthProperties();   // default adminProfileMatch = "ADMIN"

    SauUsuUserDetailsService service() {
        return new SauUsuUserDetailsService(repo, audit, props);
    }

    /** Builds a SAU_USU row. chaveSenha==null ⇒ migrated (bcrypt); non-null ⇒ not migrated. */
    private Usuario user(int id, String login, String senha, String chave, int bloq,
                         Integer perfil, boolean sysmar) {
        Usuario u = new Usuario();
        u.setUsuCod(id);
        u.setLogin(login);
        u.setNome("Usuario Sintetico");
        u.setSenha(senha);
        u.setChaveSenha(chave);
        u.setBloqueado((short) bloq);
        u.setPerfilId(perfil);
        u.setSuperusuario(sysmar);
        return u;
    }

    private static List<String> roles(UserDetails ud) {
        return ud.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    // ── R1/Resolved — migrated (bcrypt, UsuKey NULL) user authenticates ─────────
    @Test
    void migratedUser_isEnabledWithHashedPasswordAndBaselineRole() {
        Usuario u = user(10, "joao", BCRYPT, null, 0, null, false);
        when(repo.findByLogin("joao")).thenReturn(Optional.of(u));

        UserDetails ud = service().loadUserByUsername("joao");

        assertThat(ud.getUsername()).isEqualTo("10");            // sub = UsuCod (OQ7)
        assertThat(ud.getPassword()).isEqualTo(BCRYPT);          // bcrypt handed to the matcher
        assertThat(ud.isEnabled()).isTrue();
        assertThat(ud.isAccountNonLocked()).isTrue();
        assertThat(roles(ud)).contains("ROLE_SAUDE_CADASTRO");
    }

    // ── R7 / OQ5 — successful load stamps last access + writes LOGIN audit ──────
    @Test
    void migratedUser_stampsLastAccessAndAuditsLogin() {
        Usuario u = user(10, "joao", BCRYPT, null, 0, null, false);
        when(repo.findByLogin("joao")).thenReturn(Optional.of(u));

        service().loadUserByUsername("joao");

        assertThat(u.getUltimoAcesso()).isNotNull();             // R7 best-effort stamp
        verify(repo).save(u);
        verify(audit).record(eq("LOGIN"), eq("SAU_USU"), eq(10)); // OQ5
    }

    // ── Resolved — not migrated (UsuKey present) ⇒ disabled, no audit ──────────
    @Test
    void notMigratedUser_isDisabledAndNotAuditedOrStamped() {
        Usuario u = user(11, "maria", "legacyCipher", "perUserKey1234567890", 0, null, false);
        when(repo.findByLogin("maria")).thenReturn(Optional.of(u));

        UserDetails ud = service().loadUserByUsername("maria");

        assertThat(ud.isEnabled()).isFalse();                   // "redefina sua senha"
        assertThat(ud.getPassword()).doesNotContain("legacyCipher"); // never hand legacy cipher to matcher
        verify(repo, never()).save(any());
        verify(audit, never()).record(eq("LOGIN"), eq("SAU_USU"), org.mockito.ArgumentMatchers.any());
    }

    // ── R5 — blocked (UsuBloq=1) ⇒ account locked, even when migrated ──────────
    @Test
    void blockedUser_isAccountLockedAndNotAudited() {
        Usuario u = user(12, "bloq", BCRYPT, null, 1, null, false);
        when(repo.findByLogin("bloq")).thenReturn(Optional.of(u));

        UserDetails ud = service().loadUserByUsername("bloq");

        assertThat(ud.isAccountNonLocked()).isFalse();          // R5
        verify(audit, never()).record(eq("LOGIN"), eq("SAU_USU"), org.mockito.ArgumentMatchers.any());
    }

    // ── R2 / OQ2 — UsuSysmar=true ⇒ ROLE_SUPERUSER (+admin+cadastro), bypasses all ─
    @Test
    void superuserFlag_grantsSuperuserAndAdminAndCadastro() {
        Usuario u = user(1, "root", BCRYPT, null, 0, null, true);
        when(repo.findByLogin("root")).thenReturn(Optional.of(u));

        List<String> roles = roles(service().loadUserByUsername("root"));

        assertThat(roles).contains("ROLE_SUPERUSER", "ROLE_SAUDE_ADMIN", "ROLE_SAUDE_CADASTRO");
        verify(repo, never()).findPerfilNome(org.mockito.ArgumentMatchers.any()); // short-circuits profile read
    }

    // ── OQ8 — admin-looking SAU_PRF profile ⇒ elevation to ADMIN ───────────────
    @Test
    void adminProfile_elevatesToAdminRoles() {
        Usuario u = user(20, "gestor", BCRYPT, null, 0, 5, false);
        when(repo.findByLogin("gestor")).thenReturn(Optional.of(u));
        when(repo.findPerfilNome(5)).thenReturn(Optional.of("ADMINISTRADOR GERAL"));

        List<String> roles = roles(service().loadUserByUsername("gestor"));

        assertThat(roles).contains("ROLE_SAUDE_CADASTRO", "ROLE_SAUDE_ADMIN", "ROLE_ADMIN");
        assertThat(roles).doesNotContain("ROLE_SUPERUSER");
    }

    @Test
    void nonAdminProfile_staysCadastroOnly() {
        Usuario u = user(21, "atendente", BCRYPT, null, 0, 6, false);
        when(repo.findByLogin("atendente")).thenReturn(Optional.of(u));
        when(repo.findPerfilNome(6)).thenReturn(Optional.of("ATENDENTE"));

        List<String> roles = roles(service().loadUserByUsername("atendente"));

        assertThat(roles).contains("ROLE_SAUDE_CADASTRO");
        assertThat(roles).doesNotContain("ROLE_SAUDE_ADMIN", "ROLE_ADMIN", "ROLE_SUPERUSER");
    }

    // ── OQ8 — SAU_PRF read failure ⇒ no admin elevation (defensive) ────────────
    @Test
    void perfilLookupFailure_doesNotElevate() {
        Usuario u = user(22, "x", BCRYPT, null, 0, 9, false);
        when(repo.findByLogin("x")).thenReturn(Optional.of(u));
        when(repo.findPerfilNome(9)).thenThrow(new RuntimeException("sau_prf absent"));

        List<String> roles = roles(service().loadUserByUsername("x"));

        assertThat(roles).containsExactly("ROLE_SAUDE_CADASTRO");
    }

    // ── R3 / OQ10 — ambiguous login (duplicate rows) ⇒ generic failure ─────────
    @Test
    void ambiguousLogin_throwsUsernameNotFound_noEnumeration() {
        when(repo.findByLogin("dup")).thenThrow(new IncorrectResultSizeDataAccessException(1));
        assertThatThrownBy(() -> service().loadUserByUsername("dup"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // ── absent login ⇒ UsernameNotFoundException ───────────────────────────────
    @Test
    void unknownLogin_throwsUsernameNotFound() {
        when(repo.findByLogin("ghost")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // ── refresh path — numeric principal resolves by UsuCod ────────────────────
    @Test
    void numericPrincipal_resolvesByUsuCod() {
        Usuario u = user(30, "joao", BCRYPT, null, 0, null, false);
        when(repo.findByLogin("30")).thenReturn(Optional.empty());
        when(repo.findById(30)).thenReturn(Optional.of(u));

        UserDetails ud = service().loadUserByUsername("30");

        assertThat(ud.getUsername()).isEqualTo("30");
        assertThat(ud.isEnabled()).isTrue();
    }
}
