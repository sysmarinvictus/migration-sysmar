package br.gov.mandaguari.saude.security;

import br.gov.mandaguari.saude.common.error.DomainExceptions;
import br.gov.mandaguari.saude.security.AuthDtos.LoginRequest;
import br.gov.mandaguari.saude.security.AuthDtos.RefreshRequest;
import br.gov.mandaguari.saude.security.AuthDtos.TokenResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Issues JWTs. Authenticates against the configured {@link UserDetailsService}.
 * In Wave 0 this is backed by SAU_USU/SYS_PES (password scheme TBD — see SecurityConfig note);
 * in dev/test a stub user source is used ({@link DevUserDetailsService}).
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserDetailsService users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    /** Enforces enabled / non-locked / non-expired status (SAU_USU R5 blocked, not-migrated disabled). */
    private final AccountStatusUserDetailsChecker statusChecker = new AccountStatusUserDetailsChecker();

    public AuthController(UserDetailsService users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        UserDetails u;
        try {
            u = users.loadUserByUsername(req.username());
        } catch (Exception e) {
            throw new DomainExceptions.BusinessRule("auth.invalid", "Usuário ou senha inválidos");
        }
        if (!encoder.matches(req.password(), u.getPassword())) {
            throw new DomainExceptions.BusinessRule("auth.invalid", "Usuário ou senha inválidos");
        }
        // SAU_USU R5: the blocked/disabled gate is checked AFTER the password (legacy rejects a blocked
        // user only once the password is correct). Without this, a blocked user with a migrated bcrypt
        // password — or a not-migrated user — would still receive a valid JWT (the JWT filter is stateless
        // and never re-checks). loadUserByUsername sets accountLocked (UsuBloq=1) and disabled (UsuKey present).
        requireUsableAccount(u);
        List<String> roles = u.getAuthorities().stream()
                .map(a -> a.getAuthority().replaceFirst("^ROLE_", "")).toList();
        return TokenResponse.bearer(jwt.issueAccessToken(u.getUsername(), roles),
                jwt.issueRefreshToken(u.getUsername()), u.getUsername(), roles);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
        try {
            Claims c = jwt.parse(req.refreshToken());
            if (!"refresh".equals(c.get("typ", String.class))) {
                throw new DomainExceptions.BusinessRule("auth.invalid", "Token de refresh inválido");
            }
            UserDetails u = users.loadUserByUsername(c.getSubject());
            requireUsableAccount(u);   // a user blocked since the token was issued must not refresh (R5)
            List<String> roles = u.getAuthorities().stream()
                    .map(a -> a.getAuthority().replaceFirst("^ROLE_", "")).toList();
            return TokenResponse.bearer(jwt.issueAccessToken(u.getUsername(), roles),
                    jwt.issueRefreshToken(u.getUsername()), u.getUsername(), roles);
        } catch (JwtException e) {
            throw new DomainExceptions.BusinessRule("auth.invalid", "Token de refresh inválido");
        }
    }

    /**
     * Rejects a blocked (R5), not-migrated (UsuKey present → disabled) or expired account with a clear
     * message. {@link AccountStatusUserDetailsChecker} throws the matching status exception; we translate
     * to a {@code BusinessRule}. Distinct messages here are intentional (legacy shows "Usuário Bloqueado!"
     * after a correct password) — generic-failure / no-enumeration (R3) applies only to the unknown-user
     * and wrong-password paths above, which both return the same "Usuário ou senha inválidos".
     */
    private void requireUsableAccount(UserDetails u) {
        try {
            statusChecker.check(u);
        } catch (LockedException e) {
            throw new DomainExceptions.BusinessRule("auth.blocked", "Usuário bloqueado");
        } catch (DisabledException e) {
            throw new DomainExceptions.BusinessRule("auth.reset",
                    "Senha não migrada para o novo sistema — redefina sua senha");
        } catch (AccountStatusException e) {   // expired / credentials-expired → generic
            throw new DomainExceptions.BusinessRule("auth.invalid", "Usuário ou senha inválidos");
        }
    }
}
