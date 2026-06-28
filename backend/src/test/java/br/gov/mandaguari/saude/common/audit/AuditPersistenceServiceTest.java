package br.gov.mandaguari.saude.common.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuditPersistenceService} — R6 time-based composite-PK collision handling.
 * The repository is mocked so we can force a duplicate-PK and observe the +1µs retry without a DB.
 */
@ExtendWith(MockitoExtension.class)
class AuditPersistenceServiceTest {

    @Mock LogAuditoriaRepository repository;
    @InjectMocks AuditPersistenceService service;

    private LogAuditoria rowAt(LocalDateTime when) {
        LogAuditoria r = new LogAuditoria();
        r.setEmpresaCodigo(1);
        r.setUsuarioCodigo(7);
        r.setChaveRegistro("100");
        r.setOperacao("INS");
        r.setTabela("SAU_ESP");
        r.setDataHora(when);
        return r;
    }

    // R6 — a single collision nudges dataHora by +1µs and the second save succeeds (2 attempts).
    @Test
    void retriesOnPkCollisionNudgingTimestampByOneMicrosecond() {
        LocalDateTime t0 = LocalDateTime.of(2026, 6, 22, 10, 0, 0, 0);
        when(repository.save(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new DataIntegrityViolationException("dup pk"))  // attempt 1 collides
                .thenReturn(null);                                          // attempt 2 succeeds

        LogAuditoria row = rowAt(t0);
        assertThatCode(() -> service.persist(row)).doesNotThrowAnyException();

        // Two save attempts on the same (re-stamped) row instance.
        verify(repository, times(2)).save(row);
        // After the retry the persisted timestamp advanced exactly +1µs from the original.
        assertThat(row.getDataHora()).isEqualTo(t0.plusNanos(1_000));
    }

    // R6 — exhausting the bounded retries (5) finally surfaces the exception to AuditService's
    // catch-all (which swallows it so the caller is never broken — see AuditServiceTest). Here we
    // assert it gives up after exactly MAX_PK_COLLISION_RETRIES attempts rather than looping forever.
    @Test
    void exhaustingRetriesGivesUpAfterBoundedAttempts() {
        LocalDateTime t0 = LocalDateTime.of(2026, 6, 22, 10, 0, 0, 0);
        when(repository.save(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new DataIntegrityViolationException("dup pk")); // every attempt collides

        LogAuditoria row = rowAt(t0);
        assertThatThrownBy(() -> service.persist(row))
                .isInstanceOf(DataIntegrityViolationException.class);

        verify(repository, times(5)).save(row);                 // bounded: exactly 5 attempts
        // 5 attempts are stamped t0, t0+1µs … t0+4µs (the last attempt is stamped before it throws).
        assertThat(row.getDataHora()).isEqualTo(t0.plusNanos(4_000));
    }
}
