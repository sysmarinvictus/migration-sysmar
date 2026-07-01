package br.gov.mandaguari.saude.usuariounidade.repository;

import br.gov.mandaguari.saude.usuariounidade.domain.UsuarioUnidade;
import br.gov.mandaguari.saude.usuariounidade.domain.UsuarioUnidadeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Read/maintain the SAU_USUUNI capability matrix (composite PK via UsuarioUnidadeId). */
public interface UsuarioUnidadeRepository extends JpaRepository<UsuarioUnidade, UsuarioUnidadeId> {

    /** All units a user has a capability row for. */
    List<UsuarioUnidade> findByUsuCodOrderByUniCod(Integer usuCod);

    /** R1: user must exist in SAU_USU. */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_USU WHERE UsuCod = :cod)", nativeQuery = true)
    boolean usuarioExists(@Param("cod") Integer cod);

    /** R2: unit must exist in SAU_UNI. */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_UNI WHERE UniCod = :cod)", nativeQuery = true)
    boolean unidadeExists(@Param("cod") Integer cod);

    /** R3: especialidade (auditor) must exist in SAU_ESP when set. */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_ESP WHERE EspCod = :cod)", nativeQuery = true)
    boolean especialidadeExists(@Param("cod") Integer cod);
}
