package br.gov.mandaguari.saude.distrito.repository;

import br.gov.mandaguari.saude.distrito.domain.Distrito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DistritoRepository extends JpaRepository<Distrito, Short> {

    /** R6: system-assigned PK — returns current max or 0 if table empty. */
    @Query("select coalesce(max(d.codigo), 0) from Distrito d")
    Integer findMaxCodigo();

    Page<Distrito> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    @Query("""
            select d from Distrito d
            where lower(coalesce(d.nome,'')) like lower(concat('%',:q,'%'))
            order by d.nome
            """)
    List<Distrito> lookup(@Param("q") String q, Pageable pageable);

    /** R5: delete guard — SAU_UNI references this distrito via UniDisCod. */
    @Query(value = "select count(*) > 0 from SAU_UNI where UniDisCod = :cod", nativeQuery = true)
    boolean isReferencedByUnidade(@Param("cod") Short cod);

    /** R2: FK existence check for SAU_TIPLOG. */
    @Query(value = "select count(*) > 0 from SAU_TIPLOG where TipLogCod = :cod", nativeQuery = true)
    boolean tipLogExists(@Param("cod") Integer cod);

    /** R3: FK existence check for SAU_BAI. */
    @Query(value = "select count(*) > 0 from SAU_BAI where BaiCod = :cod", nativeQuery = true)
    boolean bairroExists(@Param("cod") Integer cod);
}
