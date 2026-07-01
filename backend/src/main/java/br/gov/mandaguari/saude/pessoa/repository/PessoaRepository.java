package br.gov.mandaguari.saude.pessoa.repository;

import br.gov.mandaguari.saude.pessoa.domain.Pessoa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Read/search + write-support over the person supertype SYS_PES (SAU_PESF cadastro). */
public interface PessoaRepository extends JpaRepository<Pessoa, Long> {

    // --- SAU_PESF write support ---

    /** Next PesCod = MAX(PesCod)+1 — verbatim port of psau_inc_pes.java:52-69 (loop-lock in legacy). */
    @Query(value = "SELECT MAX(PesCod) FROM SYS_PES", nativeQuery = true)
    Optional<Long> findMaxPesCod();

    /** R53 delete-guard: person is referenced by a Profissional (SAU_PRO). */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_PRO WHERE ProPesCod = :cod)", nativeQuery = true)
    boolean referencedByProfissional(@Param("cod") Long cod);

    /** R54 delete-guard: person is referenced by a Funcionário (SAU_FUN). */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_FUN WHERE FunPesCod = :cod)", nativeQuery = true)
    boolean referencedByFuncionario(@Param("cod") Long cod);

    /** R55 delete-guard: person is referenced by a Paciente (SAU_PAC — Wave 6, raw table probe). */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_PAC WHERE PacPesCod = :cod)", nativeQuery = true)
    boolean referencedByPaciente(@Param("cod") Long cod);

    /** R12: tipo de logradouro exists (SAU_TIPLOG). */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_TIPLOG WHERE TipLogCod = :cod)", nativeQuery = true)
    boolean tipoLogradouroExists(@Param("cod") Integer cod);

    /** R15: bairro exists (SAU_BAI). */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_BAI WHERE BaiCod = :cod)", nativeQuery = true)
    boolean bairroExists(@Param("cod") Integer cod);

    /** R16/R33: município exists (SYS_MUN). */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SYS_MUN WHERE MunCod = :cod)", nativeQuery = true)
    boolean municipioExists(@Param("cod") Integer cod);

    /** R25: etnia exists (SAU_ETN). */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_ETN WHERE EtnCod = :cod)", nativeQuery = true)
    boolean etniaExists(@Param("cod") Integer cod);

    /** R34: país exists (SAU_PAIS). */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_PAIS WHERE PaisCod = :cod)", nativeQuery = true)
    boolean paisExists(@Param("cod") Integer cod);

    /** R46: ocupação CBO exists (SAU_CBOR, CborCod CHAR6). */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_CBOR WHERE CborCod = :cod)", nativeQuery = true)
    boolean cborExists(@Param("cod") String cod);

    /** R47: órgão emissor exists (SAU_ORGEMI). */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_ORGEMI WHERE OrgEmiCod = :cod)", nativeQuery = true)
    boolean orgaoEmissorExists(@Param("cod") Integer cod);


    /** Search by registry/social name (LIKE), CPF or CNS. Native + CAST(:p AS VARCHAR) to avoid PG 42P18
     *  on the nullable params; char(N) columns are space-padded → LIKE prefix on cpf/cns. */
    @Query(value = """
            SELECT pes.* FROM SYS_PES pes
            WHERE (CAST(:nome AS VARCHAR) IS NULL
                   OR lower(pes.PesNom)    LIKE lower(concat('%', CAST(:nome AS VARCHAR), '%'))
                   OR lower(pes.PesNomSoc) LIKE lower(concat('%', CAST(:nome AS VARCHAR), '%')))
              AND (CAST(:cpf AS VARCHAR) IS NULL OR pes.PesCPFCNPJ LIKE concat(CAST(:cpf AS VARCHAR), '%'))
              AND (CAST(:cns AS VARCHAR) IS NULL OR pes.PesNumCns  LIKE concat(CAST(:cns AS VARCHAR), '%'))
            """,
            countQuery = """
            SELECT count(*) FROM SYS_PES pes
            WHERE (CAST(:nome AS VARCHAR) IS NULL
                   OR lower(pes.PesNom)    LIKE lower(concat('%', CAST(:nome AS VARCHAR), '%'))
                   OR lower(pes.PesNomSoc) LIKE lower(concat('%', CAST(:nome AS VARCHAR), '%')))
              AND (CAST(:cpf AS VARCHAR) IS NULL OR pes.PesCPFCNPJ LIKE concat(CAST(:cpf AS VARCHAR), '%'))
              AND (CAST(:cns AS VARCHAR) IS NULL OR pes.PesNumCns  LIKE concat(CAST(:cns AS VARCHAR), '%'))
            """,
            nativeQuery = true)
    Page<Pessoa> search(@Param("nome") String nome, @Param("cpf") String cpf,
                        @Param("cns") String cns, Pageable pageable);

    /** Autocomplete person-resolution. */
    @Query("select p from Pessoa p where lower(p.nome) like lower(concat('%', :q, '%')) "
            + "or lower(p.nomeSocial) like lower(concat('%', :q, '%')) order by p.nome")
    List<Pessoa> lookup(@Param("q") String q, Pageable pageable);

    /** R17: CPF/CNPJ uniqueness PERSON-WIDE (across all subtypes). Returns conflicting ids (excl. self). */
    @Query("select p.id from Pessoa p where p.cpfCnpj like concat(:cpf, '%') and p.id <> :selfId")
    List<Long> findCpfOwners(@Param("cpf") String cpf, @Param("selfId") Long selfId, Pageable limit);
}
