package br.gov.mandaguari.saude.autorizacao;

import br.gov.mandaguari.saude.autorizacao.service.AuthorizationGate;
import br.gov.mandaguari.saude.autorizacao.service.PermissionResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the endpoint authorization gate. The contract: in {@code shadow} mode the legacy
 * coarse role decides (RBAC only observed), in {@code enforce} mode the RBAC verdict decides. This is
 * what makes wiring RBAC onto endpoints safe during the migration (no lock-out until we flip).
 */
@ExtendWith(MockitoExtension.class)
class AuthorizationGateTest {

    static final String PRG = "SAU_ESP";
    static final String ROLE = "SAUDE_CADASTRO";

    @Mock PermissionResolver rbac;

    private AuthorizationGate shadow() { return new AuthorizationGate(rbac, "shadow"); }
    private AuthorizationGate enforce() { return new AuthorizationGate(rbac, "enforce"); }

    private Authentication auth(String principal, String... roles) {
        var authorities = List.of(roles).stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    // ---- shadow mode: coarse role decides, RBAC is only observed ----

    @Test
    void shadow_coarseRoleGrants_evenWhenRbacDenies() {
        when(rbac.can(any(Authentication.class), eq(PRG), eq("CON"))).thenReturn(false); // empty matrix
        Authentication a = auth("1", ROLE);
        assertThat(shadow().can(a, PRG, "CON", ROLE)).isTrue();   // coarse role wins → no lock-out
        verify(rbac).can(a, PRG, "CON");                          // RBAC still consulted (for divergence log)
    }

    @Test
    void shadow_noCoarseRoleDenies_evenWhenRbacGrants() {
        when(rbac.can(any(Authentication.class), eq(PRG), eq("EXC"))).thenReturn(true);
        Authentication a = auth("1", "SOME_OTHER_ROLE");
        assertThat(shadow().can(a, PRG, "EXC", ROLE)).isFalse();  // faithful to the pre-wiring behavior
    }

    // ---- enforce mode: RBAC decides, coarse role ignored ----

    @Test
    void enforce_rbacGrants_regardlessOfCoarseRole() {
        when(rbac.can(any(Authentication.class), eq(PRG), eq("ALT"))).thenReturn(true);
        Authentication a = auth("1"); // no coarse role at all
        assertThat(enforce().can(a, PRG, "ALT", ROLE)).isTrue();
    }

    @Test
    void enforce_rbacDenies_evenWithCoarseRole() {
        when(rbac.can(any(Authentication.class), eq(PRG), eq("EXC"))).thenReturn(false);
        Authentication a = auth("1", ROLE); // has coarse role, but RBAC denies
        assertThat(enforce().can(a, PRG, "EXC", ROLE)).isFalse();
    }

    // ---- defaulting: unknown/blank enforcement value falls back to shadow (fail-safe) ----

    @Test
    void unknownEnforcementValue_defaultsToShadow() {
        lenient().when(rbac.can(any(Authentication.class), anyString(), anyString())).thenReturn(false);
        AuthorizationGate gate = new AuthorizationGate(rbac, "bogus");
        assertThat(gate.can(auth("1", ROLE), PRG, "CON", ROLE)).isTrue(); // behaves like shadow
    }
}
