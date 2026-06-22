package br.gov.mandaguari.saude.unidade.repository;

import br.gov.mandaguari.saude.unidade.domain.Sala;
import br.gov.mandaguari.saude.unidade.domain.SalaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SalaRepository extends JpaRepository<Sala, SalaId> {

    List<Sala> findByUniCodOrderBySalaCod(Integer uniCod);

    @Modifying
    @Query("delete from Sala s where s.uniCod = :uniCod")
    void deleteByUniCod(@Param("uniCod") Integer uniCod);
}
