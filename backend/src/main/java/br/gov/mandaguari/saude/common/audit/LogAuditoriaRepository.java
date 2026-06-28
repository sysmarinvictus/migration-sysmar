package br.gov.mandaguari.saude.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/**
 * Append-only repository for the audit trail.
 *
 * <p><b>Deliberately NOT a {@code JpaRepository}/{@code CrudRepository}</b> — those expose
 * {@code delete*}/{@code save}-as-update operations that would violate the append-only invariant
 * (R11). It extends the narrow Spring Data {@link Repository} marker and declares ONLY a
 * {@code save} (pure INSERT, see {@link LogAuditoria#isNew()}) plus read queries for the admin
 * viewer. No {@code @Modifying} UPDATE/DELETE is ever defined here.
 *
 * <p>Read query uses the {@code CAST(:param AS timestamp)} pattern for the nullable date-range
 * filters — PostgreSQL otherwise raises {@code 42P18 (could not determine data type)} when a bound
 * parameter is NULL on both sides of an {@code IS NULL OR ...} guard.
 */
@org.springframework.stereotype.Repository
public interface LogAuditoriaRepository extends Repository<LogAuditoria, LogAuditoriaId> {

    /** Pure INSERT (entity is always {@code isNew()}); the only write operation exposed. */
    LogAuditoria save(LogAuditoria log);

    @Query(value = """
            SELECT l.* FROM SAU_LOG l
            WHERE (:tabela IS NULL OR l.logtab = :tabela)
              AND (:usuarioCodigo IS NULL OR l.logusucod = :usuarioCodigo)
              AND (:chaveRegistro IS NULL OR trim(l.logkey) = trim(:chaveRegistro))
              AND (CAST(:dataHoraFrom AS timestamp) IS NULL OR l.logdat >= CAST(:dataHoraFrom AS timestamp))
              AND (CAST(:dataHoraTo   AS timestamp) IS NULL OR l.logdat <= CAST(:dataHoraTo   AS timestamp))
            """,
            countQuery = """
            SELECT COUNT(*) FROM SAU_LOG l
            WHERE (:tabela IS NULL OR l.logtab = :tabela)
              AND (:usuarioCodigo IS NULL OR l.logusucod = :usuarioCodigo)
              AND (:chaveRegistro IS NULL OR trim(l.logkey) = trim(:chaveRegistro))
              AND (CAST(:dataHoraFrom AS timestamp) IS NULL OR l.logdat >= CAST(:dataHoraFrom AS timestamp))
              AND (CAST(:dataHoraTo   AS timestamp) IS NULL OR l.logdat <= CAST(:dataHoraTo   AS timestamp))
            """,
            nativeQuery = true)
    Page<LogAuditoria> findByFilters(
            @Param("tabela") String tabela,
            @Param("usuarioCodigo") Integer usuarioCodigo,
            @Param("dataHoraFrom") LocalDateTime dataHoraFrom,
            @Param("dataHoraTo") LocalDateTime dataHoraTo,
            @Param("chaveRegistro") String chaveRegistro,
            Pageable pageable);
}
