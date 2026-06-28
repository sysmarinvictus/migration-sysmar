package br.gov.mandaguari.saude.common.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link AuditService}: the SAU_LOG write-path contract (R1-R9) and the fail-safe /
 * toggle behavior. The persistence bean is mocked — these assert the mapping and the guarantees the
 * business path relies on, not the actual INSERT (that's {@code LogAuditoriaRepositoryIT}).
 *
 * <p>Deterministic: no clock assertion (dataHora is server-now, an implementation detail); we only
 * assert the mapped business fields. Synthetic actor ids only — no PHI.
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock AuditPersistenceService persistence;
    @Captor ArgumentCaptor<LogAuditoria> rowCaptor;

    private AuditProperties props() {
        AuditProperties p = new AuditProperties();
        p.setEmpresaCodigo(7);            // synthetic tenant
        p.setDefaultUsuarioCodigo(99);    // synthetic system fallback actor
        return p;
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "x", List.of()));
    }

    // R1 — one row per call, with the real-data fields mapped (op/table/key/actor/tenant).
    @Test
    void emitsOneAuditRowPerCommittedTransaction() {
        AuditProperties props = props();
        authenticateAs("42");                       // numeric principal → actor 42
        AuditService svc = new AuditService(persistence, props);

        svc.record("CREATE", "SAU_BAI", 1756);

        verify(persistence, times(1)).persist(rowCaptor.capture());
        LogAuditoria row = rowCaptor.getValue();
        assertThat(row.getOperacao()).isEqualTo("INS");
        assertThat(row.getTabela()).isEqualTo("SAU_BAI");
        assertThat(row.getChaveRegistro()).isEqualTo("1756");
        assertThat(row.getEmpresaCodigo()).isEqualTo(7);     // tenant from config (R2)
        assertThat(row.getUsuarioCodigo()).isEqualTo(42);    // actor from security context (R2)
        assertThat(row.getUsuarioCodigoRef()).isEqualTo(42); // legacy duplicates LogUsuCod (R1/OQ7)
        assertThat(row.getDataHora()).isNotNull();           // R3: server now, PK member
    }

    // R4 — action → logope mapping; 3-char pass-through.
    @ParameterizedTest
    @CsvSource({
            "CREATE,INS",
            "INSERT,INS",
            "INS,INS",
            "UPDATE,UPD",
            "UPD,UPD",
            "DELETE,DLT",
            "DEL,DLT",
            "DLT,DLT",
            "VIEW,VIE",   // >3 chars → clamped to 3
            "DSP,DSP",    // 3-char pass-through unchanged
    })
    void mapsActionToLogOpe(String action, String expected) {
        assertThat(AuditService.toLogOpe(action)).isEqualTo(expected);
    }

    // R7 — prof/unit/patient ids are NULL (honest unknown), never the legacy hardcoded 0.
    @Test
    void recordsRealActorAndUnitIdsNotZeroPlaceholders() {
        authenticateAs("42");
        AuditService svc = new AuditService(persistence, props());

        svc.record("UPDATE", "SAU_ESP", 1);

        verify(persistence).persist(rowCaptor.capture());
        LogAuditoria row = rowCaptor.getValue();
        assertThat(row.getProfissionalCodigo()).isNull();
        assertThat(row.getUnidadeCodigo()).isNull();
        assertThat(row.getPacienteCodigo()).isNull();
    }

    // R8 (LGPD) — no names / history / situacao stored; numeric vs non-numeric principal → actor.
    @Test
    void auditRowContainsNoPatientOrProfessionalNames() {
        authenticateAs("alice");   // NON-numeric principal: the username must never become a name/actor
        AuditService svc = new AuditService(persistence, props());

        svc.record("CREATE", "SAU_ESP", 5);

        verify(persistence).persist(rowCaptor.capture());
        LogAuditoria row = rowCaptor.getValue();
        // No PHI / free-text columns populated.
        assertThat(row.getNomePaciente()).isNull();
        assertThat(row.getNomeProfissional()).isNull();
        assertThat(row.getHistorico()).isNull();
        assertThat(row.getSituacao()).isNull();
        // The username string is never used as a name nor as the key.
        assertThat(row.getChaveRegistro()).isEqualTo("5").doesNotContain("alice");
        // Non-numeric principal → actor falls back to the configured default (never the username).
        assertThat(row.getUsuarioCodigo()).isEqualTo(99);
        assertThat(row.getUsuarioCodigoRef()).isEqualTo(99);
    }

    // Fail-safe — a persistence failure must NOT propagate (business op unaffected).
    @Test
    void persistenceFailureNeverBreaksBusinessPath() {
        authenticateAs("42");
        doThrow(new DataIntegrityViolationException("boom"))
                .when(persistence).persist(org.mockito.ArgumentMatchers.any());
        AuditService svc = new AuditService(persistence, props());

        assertThatCode(() -> svc.record("CREATE", "SAU_BAI", 1))
                .doesNotThrowAnyException();
        verify(persistence).persist(org.mockito.ArgumentMatchers.any());
    }

    // Toggle — audit.persist.enabled=false → log-only, persistence bean not invoked.
    @Test
    void persistDisabledSkipsPersistenceBean() {
        AuditProperties props = props();
        props.getPersist().setEnabled(false);
        authenticateAs("42");
        AuditService svc = new AuditService(persistence, props);

        svc.record("CREATE", "SAU_BAI", 1);

        verify(persistence, never()).persist(org.mockito.ArgumentMatchers.any());
    }
}
