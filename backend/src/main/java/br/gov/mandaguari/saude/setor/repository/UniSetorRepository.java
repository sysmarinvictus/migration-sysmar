package br.gov.mandaguari.saude.setor.repository;

import br.gov.mandaguari.saude.setor.domain.UniSetor;
import br.gov.mandaguari.saude.setor.domain.UniSetorId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UniSetorRepository extends JpaRepository<UniSetor, UniSetorId> {

    /** List all setores for a given unidade; supports nome filter. */
    Page<UniSetor> findAllByUniCod(Integer uniCod, Pageable pageable);

    Page<UniSetor> findAllByUniCodAndNomeContainingIgnoreCase(
            Integer uniCod, String nome, Pageable pageable);

    @Query("""
            select s from UniSetor s
            where s.uniCod = :uniCod
              and lower(coalesce(s.nome,'')) like lower(concat('%',:q,'%'))
            order by s.nome
            """)
    List<UniSetor> lookup(@Param("uniCod") Integer uniCod, @Param("q") String q, Pageable pageable);

    /** R12: delete guard — SAU_PAR5 references this (UniCod, SetorCod) via ParSalUniCod/ParSalSetorCod. */
    @Query(value = "select count(*) > 0 from SAU_PAR5 where ParSalUniCod = :u and ParSalSetorCod = :s",
           nativeQuery = true)
    boolean isReferencedByPar5(@Param("u") Integer uniCod, @Param("s") Integer setorCod);

    /** R13: delete guard — SAU_USUUNI1 references this sector via UniUsuCod/UsuSetorCod. */
    @Query(value = "select count(*) > 0 from SAU_USUUNI1 where UniUsuCod = :u and UsuSetorCod = :s",
           nativeQuery = true)
    boolean isReferencedByUsuUni1(@Param("u") Integer uniCod, @Param("s") Integer setorCod);

    /** R14: delete guard — SAU_REMLOT references this sector via RemUniCod/RemSetorCod. */
    @Query(value = "select count(*) > 0 from SAU_REMLOT where RemUniCod = :u and RemSetorCod = :s",
           nativeQuery = true)
    boolean isReferencedByRemLot(@Param("u") Integer uniCod, @Param("s") Integer setorCod);

    /** R15: delete guard — SAU_REM_UNISETOR references this sector via RemUniSetorUniCod/RemUniSetorSetorCod. */
    @Query(value = """
            select count(*) > 0 from SAU_REM_UNISETOR
            where RemUniSetorUniCod = :u and RemUniSetorSetorCod = :s
            """, nativeQuery = true)
    boolean isReferencedByRemUnisetor(@Param("u") Integer uniCod, @Param("s") Integer setorCod);
}
