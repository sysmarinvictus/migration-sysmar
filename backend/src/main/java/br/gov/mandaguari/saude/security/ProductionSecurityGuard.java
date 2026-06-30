package br.gov.mandaguari.saude.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;

/**
 * Fail-closed startup backstop for non-dev environments. Active in every profile EXCEPT {@code local}
 * and {@code test} (i.e. the default/production path). Aborts startup if either dev-only security
 * artifact leaked into a real deployment:
 * <ul>
 *   <li>the configured JWT signing key looks like a development/placeholder value (would let anyone with
 *       repo access forge tokens), or is too short for HS256 (&lt; 32 bytes); or</li>
 *   <li>the {@link DevUserDetailsService} admin/admin123 backdoor bean is present.</li>
 * </ul>
 *
 * <p>Defence-in-depth behind the primary control (no {@code local} default profile in application.yml):
 * even if someone explicitly activates a misconfigured profile, the app refuses to boot rather than
 * silently exposing the backdoor. {@code DevUserDetailsService} is {@code @Profile({"local","test"})},
 * so under any other profile its bean is absent and this guard simply confirms that.
 */
@Component
@Profile("!local & !test")
public class ProductionSecurityGuard {

    /** Substrings that mark a non-production / placeholder signing key (matches the dev-local fallback). */
    private static final String[] PLACEHOLDER_MARKERS = {"insecure", "change-me", "dev-local"};
    private static final int MIN_KEY_BYTES = 32;   // HS256 minimum

    private final String configuredJwtKey;
    private final ApplicationContext ctx;

    public ProductionSecurityGuard(@Value("${security.jwt.secret}") String configuredJwtKey,
                                   ApplicationContext ctx) {
        this.configuredJwtKey = configuredJwtKey;
        this.ctx = ctx;
    }

    @PostConstruct
    void assertNotRunningWithDevSecurity() {
        String key = configuredJwtKey == null ? "" : configuredJwtKey;
        String lower = key.toLowerCase();
        for (String marker : PLACEHOLDER_MARKERS) {
            if (lower.contains(marker)) {
                throw new IllegalStateException(
                        "Refusing to start: a development/placeholder JWT signing key is in use outside the "
                      + "'local' profile. Set a strong, random JWT_SECRET (>= 32 bytes) for this environment.");
            }
        }
        if (key.getBytes(StandardCharsets.UTF_8).length < MIN_KEY_BYTES) {
            throw new IllegalStateException(
                    "Refusing to start: JWT_SECRET is shorter than " + MIN_KEY_BYTES + " bytes (weak HS256 key).");
        }
        if (ctx.getBeanNamesForType(DevUserDetailsService.class).length > 0) {
            throw new IllegalStateException(
                    "Refusing to start: the dev login backdoor (DevUserDetailsService) is active outside "
                  + "'local'/'test'. Do not run a real environment with a dev profile.");
        }
    }
}
