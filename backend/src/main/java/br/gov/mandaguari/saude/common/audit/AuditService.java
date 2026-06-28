package br.gov.mandaguari.saude.common.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Records audit events for sensitive operations — replaces the GeneXus {@code SAU_LOG} write path
 * ({@code psau_inc_log}) and the {@code usu_login} audit attribute.
 *
 * <p>Two effects per call, in order:
 * <ol>
 *   <li>a structured slf4j AUDIT line (unchanged from Phase 1 — callers rely on it);</li>
 *   <li>an INSERT into the physical {@code SAU_LOG} table (Wave-0 / this slice), when
 *       {@code audit.persist.enabled} (default true).</li>
 * </ol>
 *
 * <p><b>Fail-safe (non-negotiable):</b> persistence runs in a SEPARATE transaction
 * ({@link AuditPersistenceService}) and ALL exceptions are caught and logged as WARN — an audit
 * failure (including SAU_LOG being absent) NEVER rolls back or breaks the business operation. The
 * existing slice integration tests therefore keep passing even if the audit insert cannot run.
 *
 * <p><b>LGPD (R8):</b> only op/table/key/actor/timestamp/tenant + real prof/unit ids are stored.
 * Patient/professional NAMES and the free-text history are never written (those columns stay NULL).
 * The acting principal's username string is never persisted — only its numeric id, when numeric.
 */
@Service
public class AuditService {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditPersistenceService persistence;
    private final AuditProperties props;

    public AuditService(AuditPersistenceService persistence, AuditProperties props) {
        this.persistence = persistence;
        this.props = props;
    }

    /**
     * Records one audit event. Signature unchanged from Phase 1 so all existing callers
     * ({@code SAU_ESP}, {@code SAU_IMP}, {@code SAU_PRO}, …) work without modification.
     *
     * @param action  business action (CREATE/UPDATE/DELETE/VIEW/…); mapped to logope INS/UPD/DLT
     * @param entity  program/transaction name (e.g. {@code SAU_ESP}) → logtab (R5), NOT the SQL name
     * @param entityId affected record key → logkey (stringified, fit to CHAR(50))
     */
    public void record(String action, String entity, Object entityId) {
        // Effect 1 — structured log line (Phase-1 behavior, callers depend on it).
        AUDIT.info("action={} entity={} id={} actor={}", action, entity, entityId, currentActor());

        // Effect 2 — persist to SAU_LOG, fail-safe + toggleable.
        if (!props.getPersist().isEnabled()) {
            return; // log-only mode → original behavior.
        }
        try {
            persistence.persist(buildRow(action, entity, entityId));
        } catch (RuntimeException ex) {
            // R-fail-safe: never rethrow — the business transaction must succeed regardless.
            log.warn("audit persist to SAU_LOG failed (business op unaffected): action={} entity={} id={} cause={}",
                    action, entity, entityId, ex.toString());
        }
    }

    /** Maps a call into a {@link LogAuditoria} row (R1/R4/R5/R7/R8). */
    private LogAuditoria buildRow(String action, String entity, Object entityId) {
        int actor = currentActorCode();

        LogAuditoria row = new LogAuditoria();
        row.setEmpresaCodigo(props.getEmpresaCodigo());      // R2: tenant from config (OQ3)
        row.setDataHora(LocalDateTime.now());                // R3: server now (µs precision); PK member
        row.setUsuarioCodigo(actor);                         // R2: actor from security context
        row.setUsuarioCodigoRef(actor);                      // R1/OQ7: legacy duplicates LogUsuCod
        row.setChaveRegistro(toLogKey(entityId));            // R5: PK stringified, fit to CHAR(50)
        row.setOperacao(toLogOpe(action));                   // R4: INS/UPD/DLT
        row.setTabela(toLogTab(entity));                     // R5: program name, upper, <=31

        // R7 fix: leave prof/unit/patient ids NULL (honest "unknown") instead of the legacy hardcoded
        // 0. Richer real ids would require per-call context (out of scope) — future enhancement.
        row.setProfissionalCodigo(null);
        row.setUnidadeCodigo(null);

        // R8 / LGPD: never store names or free text.
        row.setNomePaciente(null);
        row.setPacienteCodigo(null);
        row.setNomeProfissional(null);
        row.setHistorico(null);
        row.setSituacao(null);
        return row;
    }

    /** R4: action → logope. CREATE→INS, UPDATE→UPD, DELETE→DLT, VIEW→VIEW(→clamp 3); 3-char pass-through. */
    static String toLogOpe(String action) {
        if (action == null) return null;
        String a = action.trim().toUpperCase(Locale.ROOT);
        return switch (a) {
            case "CREATE", "INSERT", "INS" -> "INS";
            case "UPDATE", "UPD" -> "UPD";
            case "DELETE", "DLT", "DEL" -> "DLT";
            default -> a.length() > 3 ? a.substring(0, 3) : a; // pass-through, clamp to col length(3)
        };
    }

    /** R5: entity/program name → logtab (uppercase, <=31 chars). */
    static String toLogTab(String entity) {
        if (entity == null) return "";
        String t = entity.trim().toUpperCase(Locale.ROOT);
        return t.length() > 31 ? t.substring(0, 31) : t;
    }

    /** R5: stringify the affected key, trimmed, clamped to CHAR(50). */
    static String toLogKey(Object entityId) {
        String s = (entityId == null) ? "" : entityId.toString().trim();
        return s.length() > 50 ? s.substring(0, 50) : s;
    }

    /** Resolve numeric actor id (logusucod) from the principal; else the configured default (R2). */
    private int currentActorCode() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String name = auth.getName();
            if (name != null && name.matches("\\d+")) {
                try {
                    return Integer.parseInt(name);
                } catch (NumberFormatException ignored) {
                    // overflow / non-int → fall through to default
                }
            }
        }
        return props.getDefaultUsuarioCodigo(); // LGPD: never store the username string
    }

    /** Display-only actor for the slf4j line (Phase-1 behavior preserved). */
    private static String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
    }

    /** Binds {@link AuditProperties} without requiring app-wide {@code @ConfigurationPropertiesScan}. */
    @Configuration
    @EnableConfigurationProperties(AuditProperties.class)
    static class AuditConfig {}
}
