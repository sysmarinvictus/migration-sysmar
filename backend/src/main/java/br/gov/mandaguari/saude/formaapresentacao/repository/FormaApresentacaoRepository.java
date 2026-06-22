package br.gov.mandaguari.saude.formaapresentacao.repository;

import br.gov.mandaguari.saude.formaapresentacao.domain.FormaApresentacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FormaApresentacaoRepository extends JpaRepository<FormaApresentacao, Integer> {

    /** R1: next AprRemCod from the dedicated sequence (seeded from MAX+1 in V6). */
    @Query(value = "select nextval('seq_sau_aprrem_cod')", nativeQuery = true)
    Integer nextCodigo();

    Page<FormaApresentacao> findByDescricaoContainingIgnoreCase(String descricao, Pageable pageable);

    @Query("select f from FormaApresentacao f where lower(coalesce(f.descricao,'')) like lower(concat('%',:q,'%')) order by f.descricao")
    List<FormaApresentacao> lookup(@Param("q") String q, Pageable pageable);

    /** R7: delete-guard — block when a Medicamento references this AprRemCod. */
    @Query(value = "select count(*) > 0 from SAU_REM where AprRemCod = :id", nativeQuery = true)
    boolean isReferencedByMedicamento(@Param("id") Integer id);
}
