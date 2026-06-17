package br.gov.mandaguari.saude.posologia.repository;

import br.gov.mandaguari.saude.posologia.domain.Posologia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PosologiaRepository extends JpaRepository<Posologia, Integer> {

    Page<Posologia> findByDescricaoContainingIgnoreCase(String descricao, Pageable pageable);

    /** R1: current max PK for the system-assigned MAX+1 strategy; returns 0 when table is empty. */
    @Query("select coalesce(max(p.codigo), 0) from Posologia p")
    Integer findMaxCodigo();

    /** Lookup (autocomplete) — replaces hpromptsau_remobs. */
    @Query("select p from Posologia p where lower(coalesce(p.descricao,'')) like lower(concat('%',:q,'%')) order by p.descricao")
    List<Posologia> lookup(@Param("q") String q, Pageable pageable);

    /**
     * R3: block delete when a Posologia de Medicamento record references this entry.
     * SAU_REMPOSO is Wave-3 un-migrated → native count; replace with derived count when migrated.
     * Referencing column: SAU_REMPOSO.PosoRemObsCod (Conversion.xml cursor T000G11).
     */
    @Query(value = "select count(*) > 0 from SAU_REMPOSO where PosoRemObsCod = :cod", nativeQuery = true)
    boolean isReferencedByRemposo(@Param("cod") Integer cod);

    /**
     * R4: block delete when an item in Receituário Controle Especial references this entry.
     * SAU_RECESP1 is Wave-6 / Portaria 344/98 un-migrated → native count.
     * Referencing column: SAU_RECESP1.RemObsCod (Conversion.xml cursor T000G12).
     */
    @Query(value = "select count(*) > 0 from SAU_RECESP1 where RemObsCod = :cod", nativeQuery = true)
    boolean isReferencedByRecesp1(@Param("cod") Integer cod);
}
