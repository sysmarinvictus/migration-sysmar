package br.gov.mandaguari.saude.especialidade.repository;

import br.gov.mandaguari.saude.especialidade.domain.Especialidade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EspecialidadeRepository extends JpaRepository<Especialidade, Integer> {

    Page<Especialidade> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    /** Lookup (autocomplete) — replaces hpromptsau_esp. */
    @Query("select e from Especialidade e where lower(e.nome) like lower(concat('%', :q, '%')) order by e.nome")
    List<Especialidade> lookup(@Param("q") String q, Pageable pageable);

    /** R3: derive the CBO description from SAU_CBOR (cross-wave FK, raw-id; no Cbor entity yet). */
    @Query(value = "select CborDes from SAU_CBOR where CborCod = :cod", nativeQuery = true)
    Optional<String> findCborDescricao(@Param("cod") String cod);

    /** R3: existence check for the CBO FK. */
    @Query(value = "select exists(select 1 from SAU_CBOR where CborCod = :cod)", nativeQuery = true)
    boolean cborExists(@Param("cod") String cod);

    /** R4: block delete when referenced by a profissional (SAU_PROESP). */
    @Query(value = "select exists(select 1 from SAU_PROESP where EspCod = :cod)", nativeQuery = true)
    boolean isReferencedByProfissional(@Param("cod") Integer cod);
}
