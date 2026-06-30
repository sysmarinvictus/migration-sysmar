package br.gov.mandaguari.saude.security;

import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.security.AuthDtos.LoginRequest;
import br.gov.mandaguari.saude.security.AuthDtos.TokenResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Endpoint-level tests for the token-issuance gate at {@link AuthController#login}. Regression cover for
 * the SAU_USU review BLOCK: the login endpoint must enforce the blocked (R5) and not-migrated (disabled)
 * status AFTER a correct password, so a blocked/disabled user never receives a JWT. The JWT filter is
 * stateless and never re-checks, so this endpoint is the only enforcement point.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthControllerTest {

    static final PasswordEncoder ENC = new BCryptPasswordEncoder();
    static final String HASH = ENC.encode("correta");

    @Mock UserDetailsService users;
    @Mock JwtService jwt;

    AuthController controller() {
        return new AuthController(users, ENC, jwt);
    }

    private UserDetails account(boolean enabled, boolean nonLocked) {
        return User.withUsername("42")
                .password(HASH)
                .disabled(!enabled)
                .accountLocked(!nonLocked)
                .authorities("ROLE_SAUDE_CADASTRO")
                .build();
    }

    private void stubTokens() {
        when(jwt.issueAccessToken(anyString(), any())).thenReturn("access-token");
        when(jwt.issueRefreshToken(anyString())).thenReturn("refresh-token");
    }

    // happy path — enabled, unlocked, correct password → token issued
    @Test
    void login_issuesTokenForUsableAccount() {
        when(users.loadUserByUsername("joao")).thenReturn(account(true, true));
        stubTokens();

        TokenResponse resp = controller().login(new LoginRequest("joao", "correta"));

        assertThat(resp.accessToken()).isEqualTo("access-token");
    }

    // R5 — blocked user WITH the correct password must NOT get a token
    @Test
    void login_rejectsBlockedUserEvenWithCorrectPassword() {
        when(users.loadUserByUsername("bloq")).thenReturn(account(true, false));   // accountLocked
        assertThatThrownBy(() -> controller().login(new LoginRequest("bloq", "correta")))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("bloqueado");
        verify(jwt, never()).issueAccessToken(anyString(), any());
    }

    // not-migrated (UsuKey present → disabled) user with correct password must NOT get a token
    @Test
    void login_rejectsNotMigratedDisabledUser() {
        when(users.loadUserByUsername("legado")).thenReturn(account(false, true));  // disabled
        assertThatThrownBy(() -> controller().login(new LoginRequest("legado", "correta")))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("redefina");
        verify(jwt, never()).issueAccessToken(anyString(), any());
    }

    // wrong password → generic failure, no status leak, no token
    @Test
    void login_rejectsWrongPasswordGenerically() {
        when(users.loadUserByUsername("joao")).thenReturn(account(true, true));
        assertThatThrownBy(() -> controller().login(new LoginRequest("joao", "ERRADA")))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("Usuário ou senha inválidos");
        verify(jwt, never()).issueAccessToken(anyString(), any());
    }

    // unknown user → same generic failure (no enumeration, R3)
    @Test
    void login_unknownUserSameGenericFailure() {
        when(users.loadUserByUsername("ghost")).thenThrow(new UsernameNotFoundException("ghost"));
        assertThatThrownBy(() -> controller().login(new LoginRequest("ghost", "x")))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("Usuário ou senha inválidos");
    }
}
