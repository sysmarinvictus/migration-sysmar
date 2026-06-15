package br.gov.mandaguari.saude.tipomedicamento.repository;

import br.gov.mandaguari.saude.tipomedicamento.domain.TipoMedicamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TipoMedicamentoRepository extends JpaRepository<TipoMedicamento, Integer> {

    Page<TipoMedicamento> findByDescricaoContainingIgnoreCase(String descricao, Pageable pageable);

    /** Lookup (autocomplete) — replaces hpromptsau_tiprem. */
    @Query("select t from TipoMedicamento t where lower(coalesce(t.descricao, '')) like lower(concat('%', :q, '%')) order by t.descricao")
    List<TipoMedicamento> lookup(@Param("q") String q, Pageable pageable);

    /**
     * R3: block delete when a Medicamento references this type. SAU_REM is not yet migrated (Wave 3),
     * so this is a native count against the legacy table; replace with a derived count once the
     * medicamento slice exists. Referencing column per Conversion.xml: SAU_REM.TipRemCod.
     */
    @Query(value = "select count(*) > 0 from SAU_REM where TipRemCod = :cod", nativeQuery = true)
    boolean isReferencedByMedicamento(@Param("cod") Integer cod);
}
