package br.gov.mandaguari.saude.unidade.repository;

import br.gov.mandaguari.saude.unidade.domain.NutricionistaProfissional;
import br.gov.mandaguari.saude.unidade.domain.NutricionistaProfissionalId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NutricionistaProfissionalRepository
        extends JpaRepository<NutricionistaProfissional, NutricionistaProfissionalId> {

    List<NutricionistaProfissional> findByUniCod(Integer uniCod);

    @Modifying
    @Query("delete from NutricionistaProfissional n where n.uniCod = :uniCod")
    void deleteByUniCod(@Param("uniCod") Integer uniCod);
}
