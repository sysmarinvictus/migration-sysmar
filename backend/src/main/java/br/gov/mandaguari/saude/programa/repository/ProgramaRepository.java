package br.gov.mandaguari.saude.programa.repository;

import br.gov.mandaguari.saude.programa.domain.Programa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProgramaRepository extends JpaRepository<Programa, String> {

    @Query("""
            select p from Programa p
            where (:q is null or lower(p.id) like lower(concat('%', :q, '%'))
                                or lower(p.nome) like lower(concat('%', :q, '%')))
              and (:grupoId is null or p.grupoId = :grupoId)
            """)
    Page<Programa> search(@Param("q") String q, @Param("grupoId") Integer grupoId, Pageable pageable);

    @Query("select p from Programa p where lower(p.id) like lower(concat('%', :q, '%')) "
            + "or lower(p.nome) like lower(concat('%', :q, '%')) order by p.id")
    List<Programa> lookup(@Param("q") String q, Pageable pageable);

    /** Delete guard: a program referenced by either permission matrix cannot be removed. */
    @Query(value = "select exists(select 1 from sau_prfcon where prfprgcod = :id "
            + "union all select 1 from sau_usucon where prgcod = :id)", nativeQuery = true)
    boolean isReferencedByPermission(@Param("id") String id);
}
