package br.gov.mandaguari.saude.security;

import br.gov.mandaguari.saude.common.error.DomainExceptions;
import br.gov.mandaguari.saude.security.AuthDtos.LoginRequest;
import br.gov.mandaguari.saude.security.AuthDtos.RefreshRequest;
import br.gov.mandaguari.saude.security.AuthDtos.TokenResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
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
            List<String> roles = u.getAuthorities().stream()
                    .map(a -> a.getAuthority().replaceFirst("^ROLE_", "")).toList();
            return TokenResponse.bearer(jwt.issueAccessToken(u.getUsername(), roles),
                    jwt.issueRefreshToken(u.getUsername()), u.getUsername(), roles);
        } catch (JwtException e) {
            throw new DomainExceptions.BusinessRule("auth.invalid", "Token de refresh inválido");
        }
    }
}
