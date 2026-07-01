package br.gov.mandaguari.saude.receituarioespecial.repository;

import br.gov.mandaguari.saude.receituarioespecial.domain.ReceituarioEspecialItem;
import br.gov.mandaguari.saude.receituarioespecial.domain.ReceituarioEspecialItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** SAU_RECESP1 line-item repository (child of SAU_RECESP). Composite PK via {@link ReceituarioEspecialItemId}. */
public interface ReceituarioEspecialItemRepository
        extends JpaRepository<ReceituarioEspecialItem, ReceituarioEspecialItemId> {

    /** All lines of one prescription, in sequence order. */
    List<ReceituarioEspecialItem> findByUnidadeIdAndCodigoOrderBySequencia(Integer unidadeId, Long codigo);

    /** Replace-on-update / cascade-cleanup: remove all lines of a prescription. */
    @Modifying
    @Query("DELETE FROM ReceituarioEspecialItem i WHERE i.unidadeId = :uni AND i.codigo = :cod")
    void deleteByMaster(@Param("uni") Integer uni, @Param("cod") Long cod);

    /** R18: medication exists (SAU_REM). */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_REM WHERE RemCod = :cod)", nativeQuery = true)
    boolean medicamentoExists(@Param("cod") Integer cod);

    /** R19: catalog drug name, to default the prescription text. */
    @Query(value = "SELECT RemNom FROM SAU_REM WHERE RemCod = :cod", nativeQuery = true)
    Optional<String> medicamentoNome(@Param("cod") Integer cod);

    /** R21: posology exists (SAU_REMOBS). */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_REMOBS WHERE RemObsCod = :cod)", nativeQuery = true)
    boolean posologiaExists(@Param("cod") Integer cod);

    /** R22: posology description, to default the line observation. */
    @Query(value = "SELECT RemObsDes FROM SAU_REMOBS WHERE RemObsCod = :cod", nativeQuery = true)
    Optional<String> posologiaDescricao(@Param("cod") Integer cod);
}
