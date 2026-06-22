package br.gov.mandaguari.saude.medicamento.repository;

import br.gov.mandaguari.saude.medicamento.domain.MedicamentoUnidade;
import br.gov.mandaguari.saude.medicamento.domain.MedicamentoUnidadeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** SAU_REM1. */
public interface MedicamentoUnidadeRepository extends JpaRepository<MedicamentoUnidade, MedicamentoUnidadeId> {

    List<MedicamentoUnidade> findByRemCodOrderByRemUniCod(Integer remCod);

    @Modifying
    @Query("delete from MedicamentoUnidade x where x.remCod = :remCod")
    void deleteByRemCod(@Param("remCod") Integer remCod);
}
