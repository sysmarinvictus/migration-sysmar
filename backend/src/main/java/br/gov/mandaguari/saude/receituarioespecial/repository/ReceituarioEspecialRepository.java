package br.gov.mandaguari.saude.receituarioespecial.repository;

import br.gov.mandaguari.saude.receituarioespecial.domain.ReceituarioEspecial;
import br.gov.mandaguari.saude.receituarioespecial.domain.ReceituarioEspecialId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * SAU_RECESP master repository. The patient name lives in SYS_PES, so search joins SAU_PAC→SYS_PES.
 * Nullable filter params use the {@code CAST(:p AS <type>)} pattern (PG 42P18 fix, as in paciente/).
 * No DB-level FKs exist live → parent links are validated by the existence queries below (R8-R11).
 */
public interface ReceituarioEspecialRepository
        extends JpaRepository<ReceituarioEspecial, ReceituarioEspecialId> {

    /** Search by patient name (SYS_PES) and/or unit; newest first. */
    @Query(value = """
            SELECT r.RecEspUniCod AS unidadeCodigo, r.RecEspCod AS numero, r.RecEspDat AS data,
                   r.PacPesCod AS pacienteCodigo, pes.PesNom AS pacienteNome,
                   r.RecEspProPesCod AS prescritorCodigo
            FROM SAU_RECESP r
            LEFT JOIN SAU_PAC pac ON pac.PacPesCod = r.PacPesCod
            LEFT JOIN SYS_PES pes ON pes.PesCod = pac.PacPesCod
            WHERE (CAST(:nome AS VARCHAR) IS NULL OR lower(pes.PesNom) LIKE lower(concat('%', CAST(:nome AS VARCHAR), '%')))
              AND (CAST(:unidade AS INTEGER) IS NULL OR r.RecEspUniCod = CAST(:unidade AS INTEGER))
              AND (CAST(:paciente AS BIGINT) IS NULL OR r.PacPesCod = CAST(:paciente AS BIGINT))
            ORDER BY r.RecEspUniCod, r.RecEspCod DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM SAU_RECESP r
            LEFT JOIN SAU_PAC pac ON pac.PacPesCod = r.PacPesCod
            LEFT JOIN SYS_PES pes ON pes.PesCod = pac.PacPesCod
            WHERE (CAST(:nome AS VARCHAR) IS NULL OR lower(pes.PesNom) LIKE lower(concat('%', CAST(:nome AS VARCHAR), '%')))
              AND (CAST(:unidade AS INTEGER) IS NULL OR r.RecEspUniCod = CAST(:unidade AS INTEGER))
              AND (CAST(:paciente AS BIGINT) IS NULL OR r.PacPesCod = CAST(:paciente AS BIGINT))
            """,
            nativeQuery = true)
    Page<ReceituarioEspecialListProjection> search(@Param("nome") String nome,
                                                   @Param("unidade") Integer unidade,
                                                   @Param("paciente") Long paciente, Pageable pageable);

    /** R1: highest RecEspCod already allocated for the unit (for the MAX+1 sequential allocator). */
    @Query(value = "SELECT MAX(RecEspCod) FROM SAU_RECESP WHERE RecEspUniCod = :uni", nativeQuery = true)
    Optional<Long> findMaxCodigoForUnit(@Param("uni") Integer uni);

    /** R8: unidade exists (SAU_UNI). */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_UNI WHERE UniCod = :cod)", nativeQuery = true)
    boolean unidadeExists(@Param("cod") Integer cod);

    /** R9: funcionário exists (SAU_FUN). */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_FUN WHERE FunPesCod = :cod)", nativeQuery = true)
    boolean funcionarioExists(@Param("cod") Long cod);

    /** R5/R10: patient exists (SAU_PAC). */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_PAC WHERE PacPesCod = :cod)", nativeQuery = true)
    boolean pacienteExists(@Param("cod") Long cod);

    /** R6/R11: prescriber exists (SAU_PRO). */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_PRO WHERE ProPesCod = :cod)", nativeQuery = true)
    boolean prescritorExists(@Param("cod") Long cod);

    /** R12/R13/R15/R16: patient status + CNS + name + birth for the prescription rules/derivations. */
    @Query(value = """
            SELECT pac.PacSit AS situacao, pes.PesNumCns AS cns, pes.PesNom AS nome,
                   pes.PesNomSoc AS nomeSocial, pes.PesUsaNomSoc AS usaNomeSocial, pes.PesNasDat AS dataNascimento
            FROM SAU_PAC pac JOIN SYS_PES pes ON pes.PesCod = pac.PacPesCod
            WHERE pac.PacPesCod = :cod
            """, nativeQuery = true)
    Optional<PatientInfoProjection> findPatientInfo(@Param("cod") Long cod);
}
