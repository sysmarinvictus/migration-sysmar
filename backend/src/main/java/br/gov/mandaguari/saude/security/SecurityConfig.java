package br.gov.mandaguari.saude.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/** Stateless JWT security. Replaces GeneXus session login + EnableIntegratedSecurity=0 custom auth. */
@Configuration
@EnableMethodSecurity   // enables @PreAuthorize on controllers/services
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    /** CORS origins — comma-separated, configurable per env (prod sets CORS_ALLOWED_ORIGINS). */
    private final List<String> corsAllowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter,
                          @Value("${security.cors.allowed-origins:http://localhost:5173}") List<String> corsAllowedOrigins) {
        this.jwtFilter = jwtFilter;
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())                       // stateless API, no cookies
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Unauthenticated (no/invalid token) → 401; authenticated-but-forbidden → 403 (default
            // access-denied handler). Without this, the anonymous principal fails @PreAuthorize and
            // the default Http403ForbiddenEntryPoint returns 403 for missing-auth too.
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/login", "/auth/refresh").permitAll()
                // Allow the servlet error dispatch through: an authenticated-but-forbidden request
                // forwards to /error, where the JWT filter is skipped (shouldNotFilterErrorDispatch).
                // Without this it is re-secured as anonymous and returns 401 instead of 403.
                .requestMatchers("/error").permitAll()
                .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**",
                                 "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var cfg = new CorsConfiguration();
        // Lock to the frontend origin(s). Default is the dev frontend; prod overrides via
        // CORS_ALLOWED_ORIGINS (comma-separated). Restrictive by design — a wrong/empty value just
        // blocks cross-origin calls (fail-closed), it does not widen access.
        cfg.setAllowedOrigins(corsAllowedOrigins);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    /** Password hashing for the new app. NOTE: legacy SAU_USU hashing scheme is an open question
     *  (Wave 0 auth slice) — migration may require a one-time rehash-on-login bridge. */
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
