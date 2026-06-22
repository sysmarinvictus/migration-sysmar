package br.gov.mandaguari.saude.unidade.repository;

import br.gov.mandaguari.saude.unidade.domain.HiperdiaProfissional;
import br.gov.mandaguari.saude.unidade.domain.HiperdiaProfissionalId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HiperdiaProfissionalRepository
        extends JpaRepository<HiperdiaProfissional, HiperdiaProfissionalId> {

    List<HiperdiaProfissional> findByUniCod(Integer uniCod);

    @Modifying
    @Query("delete from HiperdiaProfissional h where h.uniCod = :uniCod")
    void deleteByUniCod(@Param("uniCod") Integer uniCod);
}
