package br.gov.mandaguari.saude.autorizacao.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Endpoint-facing authorization gate that lets us wire the fine-grained per-program engine
 * ({@link PermissionResolver}, bean {@code rbac}) onto controllers <b>without</b> risking a lock-out
 * during the migration. It has two modes, selected by {@code rbac.enforcement}:
 *
 * <ul>
 *   <li><b>{@code shadow}</b> (default) — the legacy <b>coarse role</b> still decides the outcome
 *       (no behavior change), but the RBAC verdict is computed in parallel and any
 *       <i>divergence</i> (coarse allows / RBAC denies, or vice-versa) is logged. This is how we
 *       confirm the live {@code SAU_PRFCON}/{@code SAU_USUCON} matrix is populated for a program code
 *       <i>before</i> trusting it. Zero lock-out risk.</li>
 *   <li><b>{@code enforce}</b> — the pure {@link PermissionResolver} verdict decides (deny-by-default,
 *       faithful to the GeneXus {@code pisauthorized} engine). The coarse role is ignored.</li>
 * </ul>
 *
 * <p>Registered as bean {@code authz} so endpoints guard with, e.g.
 * {@code @PreAuthorize("@authz.can(authentication, 'SAU_ESP', 'CON', 'SAUDE_CADASTRO')")}. The
 * {@code coarseRole} argument is the role the endpoint enforced <i>before</i> wiring — it is what
 * shadow mode keeps enforcing, so shadow is a faithful no-op for any endpoint regardless of which
 * coarse role it used.
 */
@Component("authz")
public class AuthorizationGate {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationGate.class);

    /** {@code shadow} = enforce coarse role, log RBAC divergence; {@code enforce} = pure RBAC verdict. */
    enum Mode { SHADOW, ENFORCE }

    private final PermissionResolver rbac;
    private final Mode mode;

    public AuthorizationGate(PermissionResolver rbac,
                             @Value("${rbac.enforcement:shadow}") String enforcement) {
        this.rbac = rbac;
        this.mode = "enforce".equalsIgnoreCase(enforcement) ? Mode.ENFORCE : Mode.SHADOW;
    }

    /**
     * @param auth       the current authentication (principal name = UsuCod).
     * @param program    the GeneXus program code / {@code pisauthorized} key (e.g. {@code "SAU_ESP"}).
     * @param mode       one of {@code CON|INC|ALT|EXC}.
     * @param coarseRole the legacy role enforced before wiring (e.g. {@code "SAUDE_CADASTRO"}).
     * @return whether the request is authorized under the active enforcement mode.
     */
    public boolean can(Authentication auth, String program, String mode, String coarseRole) {
        boolean rbacVerdict = rbac.can(auth, program, mode);

        if (this.mode == Mode.ENFORCE) {
            return rbacVerdict;
        }

        // SHADOW: legacy coarse role decides; log divergence so we can validate the live matrix.
        boolean coarseVerdict = hasRole(auth, coarseRole);
        if (coarseVerdict != rbacVerdict) {
            log.warn("RBAC shadow divergence: principal={} program={} mode={} coarse({})={} rbac={} — "
                            + "matrix likely missing rows for this program before 'enforce' flip",
                    principal(auth), program, mode, coarseRole, coarseVerdict, rbacVerdict);
        } else {
            log.debug("RBAC shadow agree: principal={} program={} mode={} verdict={}",
                    principal(auth), program, mode, coarseVerdict);
        }
        return coarseVerdict;
    }

    private static boolean hasRole(Authentication auth, String coarseRole) {
        if (auth == null || coarseRole == null) return false;
        String authority = "ROLE_" + coarseRole;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (authority.equals(ga.getAuthority())) return true;
        }
        return false;
    }

    private static String principal(Authentication auth) {
        return auth == null ? "<none>" : auth.getName();
    }
}
