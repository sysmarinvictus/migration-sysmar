package br.gov.mandaguari.saude.impedimento.repository;

import br.gov.mandaguari.saude.impedimento.domain.Impedimento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface ImpedimentoRepository extends JpaRepository<Impedimento, Integer> {

    /** R4: next PK via MAX+1 — GeneXus psau_inc_imp pattern (no sequence on SAU_IMP). */
    @Query(value = "SELECT MAX(ImpCod) FROM SAU_IMP", nativeQuery = true)
    Optional<Integer> findMaxCodigo();

    @Query(value = """
            SELECT i.* FROM SAU_IMP i
            WHERE (:profissionalId IS NULL OR i.ProPesCod = :profissionalId)
              AND (:especialidadeId IS NULL OR i.EspCod = :especialidadeId)
              AND (CAST(:dataInicioFrom AS DATE) IS NULL OR i.ImpDatIni >= CAST(:dataInicioFrom AS DATE))
              AND (CAST(:dataFimAte AS DATE) IS NULL OR i.ImpDatFim <= CAST(:dataFimAte AS DATE))
            """,
            countQuery = """
            SELECT COUNT(*) FROM SAU_IMP i
            WHERE (:profissionalId IS NULL OR i.ProPesCod = :profissionalId)
              AND (:especialidadeId IS NULL OR i.EspCod = :especialidadeId)
              AND (CAST(:dataInicioFrom AS DATE) IS NULL OR i.ImpDatIni >= CAST(:dataInicioFrom AS DATE))
              AND (CAST(:dataFimAte AS DATE) IS NULL OR i.ImpDatFim <= CAST(:dataFimAte AS DATE))
            """,
            nativeQuery = true)
    Page<Impedimento> findByFilters(
            @Param("profissionalId") Long profissionalId,
            @Param("especialidadeId") Integer especialidadeId,
            @Param("dataInicioFrom") LocalDate dataInicioFrom,
            @Param("dataFimAte") LocalDate dataFimAte,
            Pageable pageable);

    /** Filter by profissional name — requires JOIN to SYS_PES (via SAU_PRO). */
    @Query(value = """
            SELECT i.* FROM SAU_IMP i
            JOIN SAU_PRO p ON p.ProPesCod = i.ProPesCod
            JOIN SYS_PES s ON s.PesCod = p.ProPesCod
            WHERE (:profissionalNome IS NULL OR lower(s.PesNom) LIKE lower(concat('%',:profissionalNome,'%')))
              AND (:profissionalId IS NULL OR i.ProPesCod = :profissionalId)
              AND (:especialidadeId IS NULL OR i.EspCod = :especialidadeId)
              AND (CAST(:dataInicioFrom AS DATE) IS NULL OR i.ImpDatIni >= CAST(:dataInicioFrom AS DATE))
              AND (CAST(:dataFimAte AS DATE) IS NULL OR i.ImpDatFim <= CAST(:dataFimAte AS DATE))
            """,
            countQuery = """
            SELECT COUNT(*) FROM SAU_IMP i
            JOIN SAU_PRO p ON p.ProPesCod = i.ProPesCod
            JOIN SYS_PES s ON s.PesCod = p.ProPesCod
            WHERE (:profissionalNome IS NULL OR lower(s.PesNom) LIKE lower(concat('%',:profissionalNome,'%')))
              AND (:profissionalId IS NULL OR i.ProPesCod = :profissionalId)
              AND (:especialidadeId IS NULL OR i.EspCod = :especialidadeId)
              AND (CAST(:dataInicioFrom AS DATE) IS NULL OR i.ImpDatIni >= CAST(:dataInicioFrom AS DATE))
              AND (CAST(:dataFimAte AS DATE) IS NULL OR i.ImpDatFim <= CAST(:dataFimAte AS DATE))
            """,
            nativeQuery = true)
    Page<Impedimento> findByFiltersWithNome(
            @Param("profissionalNome") String profissionalNome,
            @Param("profissionalId") Long profissionalId,
            @Param("especialidadeId") Integer especialidadeId,
            @Param("dataInicioFrom") LocalDate dataInicioFrom,
            @Param("dataFimAte") LocalDate dataFimAte,
            Pageable pageable);

    /** Display name from SYS_PES via SAU_PRO (SAU_PRO not yet migrated — raw JOIN). */
    @Query(value = "SELECT s.PesNom FROM SYS_PES s JOIN SAU_PRO p ON p.ProPesCod = s.PesCod WHERE p.ProPesCod = :cod",
            nativeQuery = true)
    Optional<String> findProfissionalNome(@Param("cod") Long cod);

    /** R8: situação (ProSit) from SAU_PRO. */
    @Query(value = "SELECT ProSit FROM SAU_PRO WHERE ProPesCod = :cod", nativeQuery = true)
    Optional<Integer> findProfissionalSituacao(@Param("cod") Long cod);

    /** CBO description via SAU_ESP → SAU_CBOR (CborCod is CHAR(6) in live DB). */
    @Query(value = "SELECT c.CborDes FROM SAU_CBOR c JOIN SAU_ESP e ON e.EspCborCod = c.CborCod WHERE e.EspCod = :espCod",
            nativeQuery = true)
    Optional<String> findCborDescricao(@Param("espCod") Integer espCod);

    /** R7/R8: profissional must exist in SAU_PRO (and code must be non-zero). */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_PRO WHERE ProPesCod = :cod)", nativeQuery = true)
    boolean profissionalExists(@Param("cod") Long cod);

    /** R10: especialidade must exist. */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_ESP WHERE EspCod = :cod)", nativeQuery = true)
    boolean especialidadeExists(@Param("cod") Integer cod);

    /** R11: profissional+especialidade pair must exist in SAU_PROESP. */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_PROESP WHERE ProPesCod = :proCod AND EspCod = :espCod)",
            nativeQuery = true)
    boolean proEspExists(@Param("proCod") Long proCod, @Param("espCod") Integer espCod);
}
