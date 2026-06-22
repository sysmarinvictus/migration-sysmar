package br.gov.mandaguari.saude.medicamento.repository;

import br.gov.mandaguari.saude.medicamento.domain.MedicamentoPosologia;
import br.gov.mandaguari.saude.medicamento.domain.MedicamentoPosologiaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** SAU_REMPOSO. */
public interface MedicamentoPosologiaRepository
        extends JpaRepository<MedicamentoPosologia, MedicamentoPosologiaId> {

    List<MedicamentoPosologia> findByRemCodOrderByPosologiaCodigo(Integer remCod);

    long countByRemCod(Integer remCod); // R13: posology-row count (display-only counter)

    @Modifying
    @Query("delete from MedicamentoPosologia x where x.remCod = :remCod")
    void deleteByRemCod(@Param("remCod") Integer remCod);
}
