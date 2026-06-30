package br.gov.mandaguari.saude.pessoa.repository;

import br.gov.mandaguari.saude.pessoa.domain.Pessoa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Read/search over the person supertype SYS_PES. No mutation here — persons are written via subtypes. */
public interface PessoaRepository extends JpaRepository<Pessoa, Long> {

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
