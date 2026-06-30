package br.gov.mandaguari.saude.perfil.repository;

import br.gov.mandaguari.saude.perfil.domain.Perfil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PerfilRepository extends JpaRepository<Perfil, Integer> {

    Page<Perfil> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    /** Lookup (autocomplete) — replaces hpromptsau_prf ("Selecionar Perfil"). */
    @Query("select p from Perfil p where lower(p.nome) like lower(concat('%', :q, '%')) order by p.nome")
    List<Perfil> lookup(@Param("q") String q, Pageable pageable);

    /** R1: next PrfCod = MAX+1 (legacy psau_inc_prf). Single-row aggregate (no full-table load). */
    @Query("select coalesce(max(p.id), 0) from Perfil p")
    int findMaxId();

    /** R4: block delete when any system user references this profile (SAU_USU.UsuPrfCod). */
    @Query(value = "select exists(select 1 from sau_usu where usuprfcod = :id)", nativeQuery = true)
    boolean isReferencedByUsuario(@Param("id") Integer id);

    /** R5: block delete when this profile is the 'social professional' default in SAU_PAR4. */
    @Query(value = "select exists(select 1 from sau_par4 where parprosocprfcod = :id)", nativeQuery = true)
    boolean isReferencedBySocialProfileParam(@Param("id") Integer id);

    /** R6: cascade — deleting a profile removes its per-program permission rows (SAU_PRFCON). */
    @Modifying
    @Query(value = "delete from sau_prfcon where prfcod = :id", nativeQuery = true)
    int deletePrfconByPrfCod(@Param("id") Integer id);
}
