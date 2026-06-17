package br.gov.mandaguari.saude.tipologradouro.repository;

import br.gov.mandaguari.saude.tipologradouro.domain.TipoLogradouro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TipoLogradouroRepository extends JpaRepository<TipoLogradouro, Integer> {

    @Query("""
            select t from TipoLogradouro t
            where lower(coalesce(t.nome,'')) like lower(concat('%',:q,'%'))
               or lower(coalesce(t.sigla,'')) like lower(concat('%',:q,'%'))
            """)
    Page<TipoLogradouro> search(@Param("q") String q, Pageable pageable);

    @Query("""
            select t from TipoLogradouro t
            where lower(coalesce(t.nome,'')) like lower(concat('%',:q,'%'))
               or lower(coalesce(t.sigla,'')) like lower(concat('%',:q,'%'))
            order by t.sigla
            """)
    List<TipoLogradouro> lookup(@Param("q") String q, Pageable pageable);
}
