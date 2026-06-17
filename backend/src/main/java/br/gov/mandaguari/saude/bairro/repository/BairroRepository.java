package br.gov.mandaguari.saude.bairro.repository;

import br.gov.mandaguari.saude.bairro.domain.Bairro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BairroRepository extends JpaRepository<Bairro, Integer> {

    Page<Bairro> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    /** Lookup (autocomplete). */
    @Query("""
            select b from Bairro b
            where lower(coalesce(b.nome, '')) like lower(concat('%', :q, '%'))
            order by b.nome
            """)
    List<Bairro> lookup(@Param("q") String q, Pageable pageable);

    @Query("select coalesce(max(b.codigo), 0) from Bairro b")
    Integer findMaxCodigo();

    /** R3: check duplicate nome on INSERT (case-insensitive). */
    boolean existsByNomeIgnoreCase(String nome);

    /** R3: check duplicate nome on UPDATE, excluding the record being edited. */
    boolean existsByNomeIgnoreCaseAndCodigoNot(String nome, Integer codigo);

    /**
     * R4: block delete when referenced by SYS_PES (person address). SYS_PES is Wave-0
     * un-migrated → native count until the entity exists.
     */
    @Query(value = "select count(*) > 0 from SYS_PES where PesBaiCod = :cod", nativeQuery = true)
    boolean isReferencedByPessoa(@Param("cod") Integer cod);

    /**
     * R5: block delete when referenced by SAU_DIS (district). SAU_DIS is Wave-2
     * un-migrated → native count until the entity exists.
     */
    @Query(value = "select count(*) > 0 from SAU_DIS where DisBaiCod = :cod", nativeQuery = true)
    boolean isReferencedByDistrito(@Param("cod") Integer cod);
}
