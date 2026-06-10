package br.gov.mandaguari.saude.conselhoclasse.repository;

import br.gov.mandaguari.saude.conselhoclasse.domain.ConselhoClasse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConselhoClasseRepository extends JpaRepository<ConselhoClasse, Short> {

    /** List/search by sigla OR nome (both nullable → coalesce). */
    @Query("""
            select c from ConselhoClasse c
            where lower(coalesce(c.sigla, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(c.nome,  '')) like lower(concat('%', :q, '%'))
            """)
    Page<ConselhoClasse> search(@Param("q") String q, Pageable pageable);

    /** Lookup (autocomplete) — replaces hpromptsau_concla. */
    @Query("""
            select c from ConselhoClasse c
            where lower(coalesce(c.sigla, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(c.nome,  '')) like lower(concat('%', :q, '%'))
            order by c.sigla
            """)
    List<ConselhoClasse> lookup(@Param("q") String q, Pageable pageable);

    /**
     * R3: block delete when a Profissional references this council. SAU_PRO is not yet migrated
     * (Wave 4), so this is a native count against the legacy table; replace with a derived count
     * once the profissional slice exists. Referencing column per Conversion.xml: SAU_PRO.ConClaCod.
     */
    @Query(value = "select count(*) > 0 from SAU_PRO where ConClaCod = :cod", nativeQuery = true)
    boolean isReferencedByProfissional(@Param("cod") Short cod);
}
