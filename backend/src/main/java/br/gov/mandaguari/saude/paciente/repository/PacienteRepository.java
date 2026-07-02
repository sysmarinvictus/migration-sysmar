package br.gov.mandaguari.saude.paciente.repository;

import br.gov.mandaguari.saude.paciente.domain.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * SAU_PAC repository. Patient person fields live in SYS_PES, so search/lookup join it; the full person
 * read/write goes through the {@code Pessoa} entity in the service. Nullable filter params use the
 * {@code CAST(:p AS <type>)} pattern (PG 42P18 fix, same as impedimento/profissional).
 */
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    /**
     * Search by person name / mother name / CPF / CNS (SYS_PES) or prontuário (SAU_PAC). Returns a projection.
     * CPF is matched on the DIGITS of {@code PesCPFCNPJ} ({@code regexp_replace(...,'[^0-9]','')}) because the
     * column stores CPF heterogeneously (mostly formatted "412.867.079-00", some raw); the service passes raw
     * digits, so both sides are normalized. Found by SAU_PAC parity (D1); see parity/paciente/PARITY-REPORT.md.
     */
    @Query(value = """
            SELECT pac.PacPesCod AS id, pes.PesNom AS nome, pes.PesNomMae AS nomeMae,
                   pac.PacProNum AS prontuario, pes.PesCPFCNPJ AS cpfCnpj, pes.PesNumCns AS cns,
                   pes.PesNasDat AS dataNascimento, pac.PacSit AS situacao, pac.PacObi AS obito
            FROM SAU_PAC pac JOIN SYS_PES pes ON pes.PesCod = pac.PacPesCod
            WHERE (CAST(:nome AS VARCHAR) IS NULL OR lower(pes.PesNom) LIKE lower(concat('%', CAST(:nome AS VARCHAR), '%')))
              AND (CAST(:nomeMae AS VARCHAR) IS NULL OR lower(pes.PesNomMae) LIKE lower(concat('%', CAST(:nomeMae AS VARCHAR), '%')))
              AND (CAST(:prontuario AS VARCHAR) IS NULL OR pac.PacProNum LIKE concat(CAST(:prontuario AS VARCHAR), '%'))
              AND (CAST(:cpf AS VARCHAR) IS NULL OR regexp_replace(pes.PesCPFCNPJ, '[^0-9]', '', 'g') LIKE concat(CAST(:cpf AS VARCHAR), '%'))
              AND (CAST(:cns AS VARCHAR) IS NULL OR pes.PesNumCns LIKE concat(CAST(:cns AS VARCHAR), '%'))
            """,
            countQuery = """
            SELECT COUNT(*) FROM SAU_PAC pac JOIN SYS_PES pes ON pes.PesCod = pac.PacPesCod
            WHERE (CAST(:nome AS VARCHAR) IS NULL OR lower(pes.PesNom) LIKE lower(concat('%', CAST(:nome AS VARCHAR), '%')))
              AND (CAST(:nomeMae AS VARCHAR) IS NULL OR lower(pes.PesNomMae) LIKE lower(concat('%', CAST(:nomeMae AS VARCHAR), '%')))
              AND (CAST(:prontuario AS VARCHAR) IS NULL OR pac.PacProNum LIKE concat(CAST(:prontuario AS VARCHAR), '%'))
              AND (CAST(:cpf AS VARCHAR) IS NULL OR regexp_replace(pes.PesCPFCNPJ, '[^0-9]', '', 'g') LIKE concat(CAST(:cpf AS VARCHAR), '%'))
              AND (CAST(:cns AS VARCHAR) IS NULL OR pes.PesNumCns LIKE concat(CAST(:cns AS VARCHAR), '%'))
            """,
            nativeQuery = true)
    Page<PacienteListProjection> search(@Param("nome") String nome, @Param("nomeMae") String nomeMae,
                                        @Param("prontuario") String prontuario, @Param("cpf") String cpf,
                                        @Param("cns") String cns, Pageable pageable);

    /** Autocomplete by name/CNS (for prescription screens). */
    @Query(value = """
            SELECT pac.PacPesCod AS id, pes.PesNom AS nome, pes.PesNomMae AS nomeMae,
                   pac.PacProNum AS prontuario, pes.PesCPFCNPJ AS cpfCnpj, pes.PesNumCns AS cns,
                   pes.PesNasDat AS dataNascimento, pac.PacSit AS situacao, pac.PacObi AS obito
            FROM SAU_PAC pac JOIN SYS_PES pes ON pes.PesCod = pac.PacPesCod
            WHERE lower(pes.PesNom) LIKE lower(concat('%', CAST(:q AS VARCHAR), '%'))
               OR pes.PesNumCns LIKE concat(CAST(:q AS VARCHAR), '%')
            ORDER BY pes.PesNom
            """, nativeQuery = true)
    List<PacienteListProjection> lookup(@Param("q") String q, Pageable pageable);

    /** R3: CNS unique among PATIENTS (join SAU_PAC→SYS_PES). Returns a conflicting patient id, if any. */
    @Query(value = """
            SELECT pac.PacPesCod FROM SAU_PAC pac JOIN SYS_PES pes ON pes.PesCod = pac.PacPesCod
            WHERE pes.PesNumCns = :cns AND pac.PacPesCod <> :selfId LIMIT 1
            """, nativeQuery = true)
    Optional<Long> findCnsOwnerAmongPatients(@Param("cns") String cns, @Param("selfId") Long selfId);

    /** R5: unidade exists in SAU_UNI. */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_UNI WHERE UniCod = :cod)", nativeQuery = true)
    boolean unidadeExists(@Param("cod") Integer cod);

    /** R2 exemption: does the unidade allow CPF-less patient registration (SAU_UNI.UniCadCPF)? */
    @Query(value = "SELECT COALESCE((SELECT UniCadCPF FROM SAU_UNI WHERE UniCod = :cod), false)", nativeQuery = true)
    boolean unidadePermiteCadastroSemCpf(@Param("cod") Integer cod);

    /** R14 delete-guard (Portaria 344/98): patient referenced by a controlled-substance prescription (SAU_RECESP). */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_RECESP WHERE PacPesCod = :id)", nativeQuery = true)
    boolean referencedByReceituarioControleEspecial(@Param("id") Long id);
}
