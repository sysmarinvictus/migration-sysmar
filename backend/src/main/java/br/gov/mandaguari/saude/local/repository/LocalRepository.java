package br.gov.mandaguari.saude.local.repository;

import br.gov.mandaguari.saude.local.domain.Local;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LocalRepository extends JpaRepository<Local, Integer> {

    Page<Local> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    /** Lookup (autocomplete) — replaces hpromptsau_loc. */
    @Query("select l from Local l where lower(coalesce(l.nome, '')) like lower(concat('%', :q, '%')) order by l.nome")
    List<Local> lookup(@Param("q") String q, Pageable pageable);

    /**
     * R4: resolve the município name/UF/IBGE from SYS_MUN (cross-table, raw-id — SYS_MUN is an
     * un-migrated system table; no Municipio entity yet). Empty Optional ⇒ the FK doesn't exist,
     * which the service treats as the legacy "Não existe 'Municipio'." rejection.
     */
    @Query(value = "select MunNom as nome, MunUF as uf, MunIBGE as ibge from SYS_MUN where MunCod = :cod",
            nativeQuery = true)
    Optional<MunicipioInfo> findMunicipio(@Param("cod") Integer cod);

    /** Projection of the derived SYS_MUN display fields. */
    interface MunicipioInfo {
        String getNome();
        String getUf();
        String getIbge();
    }
}
