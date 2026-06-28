package br.gov.mandaguari.saude.common.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Persists audit rows to {@code SAU_LOG} in a SEPARATE transaction so that an audit-store failure
 * can never roll back (or break) the business transaction that triggered it.
 *
 * <p><b>Why a distinct bean:</b> {@code @Transactional(REQUIRES_NEW)} must be invoked through a
 * Spring proxy from a different bean ({@link AuditService}) for the new-transaction semantics to
 * apply — a self-invocation would be ignored.
 *
 * <p>This method intentionally swallows ALL exceptions (logs a WARN) — see {@link AuditService} for
 * the fail-safe contract. The only retry is for the time-based composite-PK collision (R6).
 */
@Component
public class AuditPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(AuditPersistenceService.class);

    /** R6: bounded retry on duplicate composite PK, nudging the timestamp forward each attempt. */
    private static final int MAX_PK_COLLISION_RETRIES = 5;

    private final LogAuditoriaRepository repository;

    public AuditPersistenceService(LogAuditoriaRepository repository) {
        this.repository = repository;
    }

    /**
     * INSERTs one audit row in its own transaction. On a duplicate-PK collision (two events in the
     * same microsecond for the same tenant/user/key) the timestamp is advanced by +1µs and the
     * insert retried up to {@link #MAX_PK_COLLISION_RETRIES} times (R6 — no surrogate column,
     * Phase-1 rule). A new transaction is started for each attempt so a failed INSERT does not
     * poison the next one.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persist(LogAuditoria row) {
        DataIntegrityViolationException last = null;
        LocalDateTime when = row.getDataHora();
        for (int attempt = 0; attempt < MAX_PK_COLLISION_RETRIES; attempt++) {
            try {
                row.setDataHora(when);
                row.setPersisted(false);                 // force pure INSERT each attempt
                repository.save(row);
                return;
            } catch (DataIntegrityViolationException ex) {
                last = ex;
                when = when.plusNanos(1_000);            // +1 microsecond (R6)
            }
        }
        // Exhausted retries — surface to AuditService's catch-all (still must not break business tx).
        throw (last != null) ? last
                : new DataIntegrityViolationException("audit PK collision retries exhausted");
    }
}
