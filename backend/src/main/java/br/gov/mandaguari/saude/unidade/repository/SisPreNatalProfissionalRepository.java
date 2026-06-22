package br.gov.mandaguari.saude.unidade.repository;

import br.gov.mandaguari.saude.unidade.domain.SisPreNatalProfissional;
import br.gov.mandaguari.saude.unidade.domain.SisPreNatalProfissionalId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SisPreNatalProfissionalRepository
        extends JpaRepository<SisPreNatalProfissional, SisPreNatalProfissionalId> {

    List<SisPreNatalProfissional> findByUniCod(Integer uniCod);

    @Modifying
    @Query("delete from SisPreNatalProfissional s where s.uniCod = :uniCod")
    void deleteByUniCod(@Param("uniCod") Integer uniCod);
}
