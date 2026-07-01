package br.gov.mandaguari.saude.profissionalespecialidade.repository;

import br.gov.mandaguari.saude.profissionalespecialidade.domain.ProfissionalEspecialidade;
import br.gov.mandaguari.saude.profissionalespecialidade.domain.ProfissionalEspecialidadeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Read/maintain the professional↔specialty association SAU_PROESP. Composite PK via {@link ProfissionalEspecialidadeId}. */
public interface ProfissionalEspecialidadeRepository
        extends JpaRepository<ProfissionalEspecialidade, ProfissionalEspecialidadeId> {

    /** All specialties of one professional, ordered by especialidade id. */
    List<ProfissionalEspecialidade> findByProfissionalIdOrderByEspecialidadeId(Long profissionalId);

    /** R1: profissional must exist in SAU_PRO. */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_PRO WHERE ProPesCod = :cod)", nativeQuery = true)
    boolean profissionalExists(@Param("cod") Long cod);

    /** R2: especialidade must exist in SAU_ESP. */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_ESP WHERE EspCod = :cod)", nativeQuery = true)
    boolean especialidadeExists(@Param("cod") Integer cod);

    /** R5 delete-guard: an Impedimento (SAU_IMP) for this (profissional, especialidade) blocks the delete. */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM SAU_IMP WHERE ProPesCod = :proCod AND EspCod = :espCod)",
            nativeQuery = true)
    boolean impedimentoExists(@Param("proCod") Long proCod, @Param("espCod") Integer espCod);
}
