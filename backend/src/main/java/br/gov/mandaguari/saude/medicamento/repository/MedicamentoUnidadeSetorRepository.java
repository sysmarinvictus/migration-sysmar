package br.gov.mandaguari.saude.medicamento.repository;

import br.gov.mandaguari.saude.medicamento.domain.MedicamentoUnidadeSetor;
import br.gov.mandaguari.saude.medicamento.domain.MedicamentoUnidadeSetorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** SAU_REM_UNISETOR. */
public interface MedicamentoUnidadeSetorRepository
        extends JpaRepository<MedicamentoUnidadeSetor, MedicamentoUnidadeSetorId> {

    List<MedicamentoUnidadeSetor> findByRemCodOrderBySequencia(Integer remCod);

    /** R30: alternate-key uniqueness (RemUniSetorUniCod, RemUniSetorSetorCod, RemCod). */
    @Query("select count(x) from MedicamentoUnidadeSetor x where x.unidadeCodigo = :uniCod "
            + "and ((:setorCod is null and x.setorCodigo is null) or x.setorCodigo = :setorCod) "
            + "and x.remCod = :remCod")
    long countByUnidadeSetorRem(@Param("uniCod") Integer uniCod,
                                @Param("setorCod") Integer setorCod,
                                @Param("remCod") Integer remCod);

    @Modifying
    @Query("delete from MedicamentoUnidadeSetor x where x.remCod = :remCod")
    void deleteByRemCod(@Param("remCod") Integer remCod);
}
