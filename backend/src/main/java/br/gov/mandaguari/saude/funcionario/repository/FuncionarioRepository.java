package br.gov.mandaguari.saude.funcionario.repository;

import br.gov.mandaguari.saude.funcionario.domain.Funcionario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * SAU_FUN repository. Person fields live in the un-migrated SYS_PES supertype → read via native
 * projection and written back via native UPDATE (no SYS_PES JPA entity). Nullable filter params use the
 * {@code CAST(:p AS <type>)} pattern to avoid PostgreSQL 42P18 on {@code :p IS NULL} (same as SAU_PRO).
 * Physical names confirmed against live {@code saude-mandaguari}.
 */
public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    // ── List / search (nome = SYS_PES.PesNom LIKE substring) ─────────────────────────────────────
    @Query(value = """
            SELECT fun.* FROM SAU_FUN fun
            JOIN SYS_PES pes ON pes.PesCod = fun.FunPesCod
            WHERE (CAST(:id AS BIGINT) IS NULL OR fun.FunPesCod = CAST(:id AS BIGINT))
              AND (CAST(:nome AS VARCHAR) IS NULL OR lower(pes.PesNom) LIKE lower(concat('%', CAST(:nome AS VARCHAR), '%')))
              AND (CAST(:cpfCnpj AS VARCHAR) IS NULL OR pes.PesCPFCNPJ = CAST(:cpfCnpj AS VARCHAR))
              AND (CAST(:situacao AS SMALLINT) IS NULL OR fun.FunSit = CAST(:situacao AS SMALLINT))
            """,
            countQuery = """
            SELECT COUNT(*) FROM SAU_FUN fun
            JOIN SYS_PES pes ON pes.PesCod = fun.FunPesCod
            WHERE (CAST(:id AS BIGINT) IS NULL OR fun.FunPesCod = CAST(:id AS BIGINT))
              AND (CAST(:nome AS VARCHAR) IS NULL OR lower(pes.PesNom) LIKE lower(concat('%', CAST(:nome AS VARCHAR), '%')))
              AND (CAST(:cpfCnpj AS VARCHAR) IS NULL OR pes.PesCPFCNPJ = CAST(:cpfCnpj AS VARCHAR))
              AND (CAST(:situacao AS SMALLINT) IS NULL OR fun.FunSit = CAST(:situacao AS SMALLINT))
            """,
            nativeQuery = true)
    Page<Funcionario> search(@Param("id") Long id,
                             @Param("nome") String nome,
                             @Param("cpfCnpj") String cpfCnpj,
                             @Param("situacao") Short situacao,
                             Pageable pageable);

    /** Autocomplete (funcionário selector) — {id, nome} joined from SYS_PES. */
    @Query(value = """
            SELECT fun.FunPesCod AS id, pes.PesNom AS nome FROM SAU_FUN fun
            JOIN SYS_PES pes ON pes.PesCod = fun.FunPesCod
            WHERE (:q = '' OR lower(pes.PesNom) LIKE lower(concat('%', CAST(:q AS VARCHAR), '%')))
            ORDER BY pes.PesNom
            """, nativeQuery = true)
    List<LookupRow> lookup(@Param("q") String q, Pageable pageable);

    /** Slim lookup projection. */
    interface LookupRow {
        Long getId();
        String getNome();
    }

    // ── Person read projection (SYS_PES) ─────────────────────────────────────────────────────────
    @Query(value = """
            SELECT pes.PesNom     AS nome,
                   pes.PesCPFCNPJ AS cpfCnpj,
                   pes.PesFon     AS telefone,
                   pes.PesCel     AS celular
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

    // ── Delete guards (R13/R14) — native EXISTS ──────────────────────────────────────────────────
    /** R13: funcionário is linked to a system user. */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_USU WHERE FunPesCod = :id)", nativeQuery = true)
    boolean hasSystemUser(@Param("id") Long id);

    /** R14: funcionário referenced on controlled-substance prescriptions (SAU_RECESP, Portaria 344/98). */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_RECESP WHERE FunPesCod = :id)", nativeQuery = true)
    boolean hasControlledPrescription(@Param("id") Long id);
}
