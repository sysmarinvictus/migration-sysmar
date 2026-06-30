package br.gov.mandaguari.saude.security;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The production startup backstop must REFUSE to boot a real environment that carries dev-only security
 * artifacts (placeholder/short JWT key, or the admin/admin123 backdoor bean). Fail-closed.
 */
class ProductionSecurityGuardTest {

    /** A 40-byte random-looking key with no placeholder markers. */
    static final String STRONG_KEY = "9f2c7a14e0b84d6fa1c3e5079b2d8f46aa17c3e5";

    private ApplicationContext ctxWithoutDevBean() {
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getBeanNamesForType(DevUserDetailsService.class)).thenReturn(new String[0]);
        return ctx;
    }

    private void run(String key, ApplicationContext ctx) {
        new ProductionSecurityGuard(key, ctx).assertNotRunningWithDevSecurity();
    }

    @Test
    void rejectsDevPlaceholderSecret() {
        assertThatThrownBy(() -> run("dev-local-insecure-secret-change-me-please-32b", ctxWithoutDevBean()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("placeholder");
    }

    @Test
    void rejectsShortSecret() {
        assertThatThrownBy(() -> run("tooshort", ctxWithoutDevBean()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("shorter");
    }

    @Test
    void rejectsWhenDevBackdoorBeanPresent() {
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getBeanNamesForType(DevUserDetailsService.class)).thenReturn(new String[]{"devUserDetailsService"});
        assertThatThrownBy(() -> run(STRONG_KEY, ctx))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("backdoor");
    }

    @Test
    void allowsStrongSecretWithoutDevBean() {
        assertThatCode(() -> run(STRONG_KEY, ctxWithoutDevBean())).doesNotThrowAnyException();
    }
}
