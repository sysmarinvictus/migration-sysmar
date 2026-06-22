package br.gov.mandaguari.saude.profissional.repository;

import br.gov.mandaguari.saude.profissional.domain.Profissional;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * SAU_PRO repository. Person fields live in the un-migrated SYS_PES supertype, so they are read via
 * native projection and written back via native UPDATE (no SYS_PES JPA entity yet).
 *
 * <p>Nullable filter params use the {@code CAST(:p AS <type>)} pattern to avoid PostgreSQL error
 * 42P18 ("could not determine data type of parameter") on {@code :p IS NULL} (same fix as the
 * impedimento repo). Physical table/column names confirmed against live {@code saude-mandaguari}.
 */
public interface ProfissionalRepository
        extends JpaRepository<Profissional, Long>, JpaSpecificationExecutor<Profissional> {

    // ── List / search (R16: PesNom literal LIKE substring, NOT soundex) ──────────────────────────
    @Query(value = """
            SELECT pro.* FROM SAU_PRO pro
            JOIN SYS_PES pes ON pes.PesCod = pro.ProPesCod
            WHERE (CAST(:id AS BIGINT) IS NULL OR pro.ProPesCod = CAST(:id AS BIGINT))
              AND (CAST(:nome AS VARCHAR) IS NULL OR lower(pes.PesNom) LIKE lower(concat('%', CAST(:nome AS VARCHAR), '%')))
              AND (CAST(:cpfCnpj AS VARCHAR) IS NULL OR pes.PesCPFCNPJ = CAST(:cpfCnpj AS VARCHAR))
              AND (CAST(:numeroCns AS VARCHAR) IS NULL OR pro.ProPesNumCns = CAST(:numeroCns AS VARCHAR))
              AND (CAST(:externo AS SMALLINT) IS NULL OR pro.ProExt = CAST(:externo AS SMALLINT))
              AND (CAST(:situacao AS SMALLINT) IS NULL OR pro.ProSit = CAST(:situacao AS SMALLINT))
            """,
            countQuery = """
            SELECT COUNT(*) FROM SAU_PRO pro
            JOIN SYS_PES pes ON pes.PesCod = pro.ProPesCod
            WHERE (CAST(:id AS BIGINT) IS NULL OR pro.ProPesCod = CAST(:id AS BIGINT))
              AND (CAST(:nome AS VARCHAR) IS NULL OR lower(pes.PesNom) LIKE lower(concat('%', CAST(:nome AS VARCHAR), '%')))
              AND (CAST(:cpfCnpj AS VARCHAR) IS NULL OR pes.PesCPFCNPJ = CAST(:cpfCnpj AS VARCHAR))
              AND (CAST(:numeroCns AS VARCHAR) IS NULL OR pro.ProPesNumCns = CAST(:numeroCns AS VARCHAR))
              AND (CAST(:externo AS SMALLINT) IS NULL OR pro.ProExt = CAST(:externo AS SMALLINT))
              AND (CAST(:situacao AS SMALLINT) IS NULL OR pro.ProSit = CAST(:situacao AS SMALLINT))
            """,
            nativeQuery = true)
    Page<Profissional> search(@Param("id") Long id,
                              @Param("nome") String nome,
                              @Param("cpfCnpj") String cpfCnpj,
                              @Param("numeroCns") String numeroCns,
                              @Param("externo") Short externo,
                              @Param("situacao") Short situacao,
                              Pageable pageable);

    // ── Person read projection (SYS_PES) ─────────────────────────────────────────────────────────
    @Query(value = """
            SELECT pes.PesNom        AS nome,
                   pes.PesCPFCNPJ    AS cpfCnpj,
                   pes.PesRGIE       AS rgIe,
                   pes.PesSex        AS sexo,
                   pes.PesNasDat     AS dataNascimento,
                   pes.PesEnd        AS endereco,
                   pes.PesEndNum     AS enderecoNumero,
                   pes.PesEndCom     AS enderecoComplemento,
                   pes.PesCEP        AS cep,
                   pes.PesBaiCod     AS bairroCod,
                   pes.PesMunCod     AS municipioCod,
                   pes.PesFon        AS telefone,
                   pes.PesCel        AS celular
            FROM SYS_PES pes WHERE pes.PesCod = :id
            """, nativeQuery = true)
    Optional<PersonProjection> findPerson(@Param("id") Long id);

    /** R1: the referenced person must already exist in SYS_PES. */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SYS_PES WHERE PesCod = :id)", nativeQuery = true)
    boolean personExists(@Param("id") Long id);

    /** R2: write back the person's editable fields (name/cpf/phones) to SYS_PES on confirm. */
    @Modifying
    @Query(value = """
            UPDATE SYS_PES SET PesNom = :nome, PesCPFCNPJ = :cpfCnpj, PesFon = :telefone, PesCel = :celular
            WHERE PesCod = :id
            """, nativeQuery = true)
    int updatePerson(@Param("id") Long id,
                     @Param("nome") String nome,
                     @Param("cpfCnpj") String cpfCnpj,
                     @Param("telefone") String telefone,
                     @Param("celular") String celular);

    // ── Uniqueness across people (exclude current id) ────────────────────────────────────────────
    /** R5: CNS unique across all professionals. Returns the conflicting id, if any. */
    @Query(value = """
            SELECT ProPesCod FROM SAU_PRO
            WHERE ProPesNumCns = :cns AND ProPesCod <> :selfId LIMIT 1
            """, nativeQuery = true)
    Optional<Long> findCnsOwner(@Param("cns") String cns, @Param("selfId") Long selfId);

    /** R7: CPF/CNPJ unique across all people (SYS_PES level). Returns the conflicting id, if any. */
    @Query(value = """
            SELECT PesCod FROM SYS_PES
            WHERE PesCPFCNPJ = :cpfCnpj AND PesCod <> :selfId LIMIT 1
            """, nativeQuery = true)
    Optional<Long> findCpfCnpjOwner(@Param("cpfCnpj") String cpfCnpj, @Param("selfId") Long selfId);

    // ── Delete guards (R20-R26). Tables outside the live DB are guarded via to_regclass. ─────────
    /** R20: SAU_PROESP — professional has specialties. */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_PROESP WHERE ProPesCod = :id)", nativeQuery = true)
    boolean hasSpecialty(@Param("id") Long id);

    /** R21: SAU_USU — professional is linked to a system user (usupropescod). */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_USU WHERE UsuProPesCod = :id)", nativeQuery = true)
    boolean hasSystemUser(@Param("id") Long id);

    /**
     * R22: "Uni Nut Pro Pes". TODO confirm physical table name — not present in live saude-mandaguari
     * (candidates SAU_ACONUT/SAU_CADNUT); OQ-DG.
     *
     * <p>The table may not exist, so the existence check must be evaluated <b>dynamically</b>:
     * {@code query_to_xml(sql, ...)} runs {@code sql} lazily and is only invoked when
     * {@code to_regclass(...) IS NOT NULL}, so the planner never has to resolve a missing relation
     * (a plain {@code CASE ... SELECT FROM missing_table} fails at parse time — 42P01). When the table
     * is absent the guard returns {@code false} (not referenced).
     */
    @Query(value = """
            SELECT CASE
                     WHEN to_regclass('public.uni_nut_pro_pes') IS NULL THEN false
                     ELSE (xpath('/row/c/text()',
                            query_to_xml(
                              format('SELECT count(*) AS c FROM uni_nut_pro_pes WHERE ProPesCod = %s', :id),
                              false, true, '')))[1]::text::int > 0
                   END
            """, nativeQuery = true)
    boolean hasUniNut(@Param("id") Long id);

    /** R23: SISPRENATAL. TODO confirm — table absent from live DB (OQ-DG); dynamically guarded. */
    @Query(value = """
            SELECT CASE
                     WHEN to_regclass('public.sisprenatal') IS NULL THEN false
                     ELSE (xpath('/row/c/text()',
                            query_to_xml(
                              format('SELECT count(*) AS c FROM sisprenatal WHERE ProPesCod = %s', :id),
                              false, true, '')))[1]::text::int > 0
                   END
            """, nativeQuery = true)
    boolean hasSisprenatal(@Param("id") Long id);

    /** R24: HIPERDIA. TODO confirm — table absent from live DB (OQ-DG); dynamically guarded. */
    @Query(value = """
            SELECT CASE
                     WHEN to_regclass('public.hiperdia') IS NULL THEN false
                     ELSE (xpath('/row/c/text()',
                            query_to_xml(
                              format('SELECT count(*) AS c FROM hiperdia WHERE ProPesCod = %s', :id),
                              false, true, '')))[1]::text::int > 0
                   END
            """, nativeQuery = true)
    boolean hasHiperdia(@Param("id") Long id);

    /** R25: SAU_UNI — professional assigned in any of 4 roles (autorizador/auditor/diretor/médico). */
    @Query(value = """
            SELECT EXISTS(SELECT 1 FROM SAU_UNI
                          WHERE UniProPesAutCod = :id OR UniProPesAudCod = :id
                             OR UniProPesDirCod = :id OR UniProPesRespCod = :id)
            """, nativeQuery = true)
    boolean hasUnidadeRole(@Param("id") Long id);

    /** R26: SAU_RECESP — professional has controlled-substance prescriptions (Portaria 344/98). */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_RECESP WHERE RecEspProPesCod = :id)", nativeQuery = true)
    boolean hasControlledPrescription(@Param("id") Long id);
}
