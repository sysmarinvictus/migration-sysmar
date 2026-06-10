package br.gov.mandaguari.saude.common.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Records audit events for sensitive operations — replaces GeneXus {@code SAU_LOG} writes and the
 * {@code usu_login} audit attribute. LGPD: log the acting user, action, entity, and id ONLY —
 * never patient names/CPF/CNS/diagnoses (those are PHI; see {@code phi_fields} in each SLICE-SPEC).
 *
 * <p>Phase 1 emits a structured audit log line. TODO: also persist to the legacy {@code SAU_LOG}
 * table (or a dedicated append-only audit table) once its schema is mapped (Wave 0 / sau_log slice).
 */
@Service
public class AuditService {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    public void record(String action, String entity, Object entityId) {
        AUDIT.info("action={} entity={} id={} actor={}", action, entity, entityId, currentActor());
    }

    private static String currentActor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
    }
}
