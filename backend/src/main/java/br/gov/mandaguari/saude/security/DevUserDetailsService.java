package br.gov.mandaguari.saude.security;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * TEMPORARY dev/test user source. Active only in 'local' and 'test' profiles.
 *
 * <p>REPLACE in the Wave-0 auth slice with a SAU_USU/SYS_PES-backed {@link UserDetailsService}.
 * Open question: the legacy password hashing scheme in SAU_USU — a rehash-on-first-login bridge is
 * likely needed. This stub provides a single admin so the app + integration tests are runnable.
 */
@Service
@Profile({"local", "test"})
public class DevUserDetailsService implements UserDetailsService {

    private final UserDetails admin;

    public DevUserDetailsService(PasswordEncoder encoder) {
        this.admin = User.withUsername("admin")
                .password(encoder.encode("admin123"))   // dev only — never a real credential
                .roles("ADMIN", "SAUDE_CADASTRO")
                .build();
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        if (admin.getUsername().equals(username)) return admin;
        throw new UsernameNotFoundException(username);
    }
}
