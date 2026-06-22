package br.gov.mandaguari.saude.medicamento.repository;

import br.gov.mandaguari.saude.medicamento.domain.MedicamentoEan13;
import br.gov.mandaguari.saude.medicamento.domain.MedicamentoEan13Id;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** SAU_REM2. */
public interface MedicamentoEan13Repository extends JpaRepository<MedicamentoEan13, MedicamentoEan13Id> {

    List<MedicamentoEan13> findByRemCodOrderByEan13(Integer remCod);

    @Modifying
    @Query("delete from MedicamentoEan13 x where x.remCod = :remCod")
    void deleteByRemCod(@Param("remCod") Integer remCod);
}
