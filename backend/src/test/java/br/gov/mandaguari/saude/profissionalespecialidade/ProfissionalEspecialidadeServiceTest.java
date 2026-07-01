package br.gov.mandaguari.saude.profissionalespecialidade;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.profissionalespecialidade.domain.ProfissionalEspecialidade;
import br.gov.mandaguari.saude.profissionalespecialidade.domain.ProfissionalEspecialidadeId;
import br.gov.mandaguari.saude.profissionalespecialidade.dto.ProfissionalEspecialidadeDtos.*;
import br.gov.mandaguari.saude.profissionalespecialidade.repository.ProfissionalEspecialidadeRepository;
import br.gov.mandaguari.saude.profissionalespecialidade.service.ProfissionalEspecialidadeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for mined SAU_PROESP rules (see SLICE-SPEC SAU_PROESP). Repository + audit are mocked.
 * Rule refs match the SLICE-SPEC citations (sau_proesp_impl.java).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfissionalEspecialidadeServiceTest {

    static final long PRO = 100L;
    static final int ESP = 1;

    @Mock ProfissionalEspecialidadeRepository repo;
    @Mock AuditService audit;

    ProfissionalEspecialidadeService service() {
        return new ProfissionalEspecialidadeService(repo, audit);
    }

    private EspecialidadeCreateRequest validCreate() {
        return new EspecialidadeCreateRequest(ESP, false, (short) 0, (short) 0, (short) 0);
    }

    // R1 — profissional must exist in SAU_PRO
    @Test
    void requiresAndValidatesProfissional() {
        when(repo.profissionalExists(PRO)).thenReturn(false);
        assertThatThrownBy(() -> service().add(PRO, validCreate()))
                .isInstanceOf(NotFound.class)
                .hasMessageContaining("Profissional");
    }

    // R1 — profissional code zero is rejected before any lookup
    @Test
    void rejectsProfissionalZero() {
        assertThatThrownBy(() -> service().add(0L, validCreate()))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("Profissional");
    }

    // R2 — especialidade required (zero/null rejected)
    @Test
    void requiresEspecialidade() {
        when(repo.profissionalExists(PRO)).thenReturn(true);
        var req = new EspecialidadeCreateRequest(0, false, null, null, null);
        assertThatThrownBy(() -> service().add(PRO, req))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("Especialidade");
    }

    // R2 — especialidade must exist in SAU_ESP
    @Test
    void validatesEspecialidadeExists() {
        when(repo.profissionalExists(PRO)).thenReturn(true);
        when(repo.especialidadeExists(ESP)).thenReturn(false);
        assertThatThrownBy(() -> service().add(PRO, validCreate()))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("não encontrada");
    }

    // R3 — situação defaults to 1 (Ativo) on insert
    @Test
    void defaultsSituacaoToAtivo() {
        stubValidAdd();
        service().add(PRO, validCreate());
        verify(repo).save(argThat(pe -> pe.getSituacao() != null && pe.getSituacao() == 1));
    }

    // R4 — prioritário defaults to 0 when not requested
    @Test
    void prioritarioDefaultsFalse() {
        stubValidAdd();
        var req = new EspecialidadeCreateRequest(ESP, null, null, null, null);
        service().add(PRO, req);
        verify(repo).save(argThat(pe -> pe.getPrioritario() != null && pe.getPrioritario() == 0));
    }

    // R4 — prioritário=true maps to flag 1
    @Test
    void prioritarioTrueMapsToOne() {
        stubValidAdd();
        var req = new EspecialidadeCreateRequest(ESP, true, null, null, null);
        service().add(PRO, req);
        verify(repo).save(argThat(pe -> pe.getPrioritario() != null && pe.getPrioritario() == 1));
    }

    // R7 — composite PK unique: cannot add the same specialty twice
    @Test
    void rejectsDuplicatePair() {
        when(repo.profissionalExists(PRO)).thenReturn(true);
        when(repo.especialidadeExists(ESP)).thenReturn(true);
        when(repo.existsById(new ProfissionalEspecialidadeId(PRO, ESP))).thenReturn(true);
        assertThatThrownBy(() -> service().add(PRO, validCreate()))
                .isInstanceOf(Conflict.class)
                .hasMessageContaining("já possui");
    }

    // R8 — audit on add
    @Test
    void auditsOnAdd() {
        stubValidAdd();
        service().add(PRO, validCreate());
        verify(audit).record(eq("CREATE"), eq("SAU_PROESP"), eq("100/1"));
    }

    // R5 — delete blocked when an Impedimento exists for the pair
    @Test
    void blocksDeleteWhenImpedimentoExists() {
        when(repo.findById(new ProfissionalEspecialidadeId(PRO, ESP))).thenReturn(Optional.of(entity()));
        when(repo.impedimentoExists(PRO, ESP)).thenReturn(true);
        assertThatThrownBy(() -> service().remove(PRO, ESP))
                .isInstanceOf(Conflict.class)
                .hasMessageContaining("Impedimento");
        verify(repo, never()).delete(any());
        verify(audit, never()).record(eq("DELETE"), any(), any());
    }

    // R5/R8 — delete succeeds and audits when no impedimento
    @Test
    void deletesAndAuditsWhenNoImpedimento() {
        when(repo.findById(new ProfissionalEspecialidadeId(PRO, ESP))).thenReturn(Optional.of(entity()));
        when(repo.impedimentoExists(PRO, ESP)).thenReturn(false);
        service().remove(PRO, ESP);
        verify(repo).delete(any());
        verify(audit).record(eq("DELETE"), eq("SAU_PROESP"), eq("100/1"));
    }

    // update — not found
    @Test
    void updateThrowsWhenNotFound() {
        when(repo.findById(new ProfissionalEspecialidadeId(PRO, 999))).thenReturn(Optional.empty());
        var req = new EspecialidadeUpdateRequest(true, (short) 1, null, null, null);
        assertThatThrownBy(() -> service().update(PRO, 999, req))
                .isInstanceOf(NotFound.class);
    }

    private void stubValidAdd() {
        when(repo.profissionalExists(PRO)).thenReturn(true);
        when(repo.especialidadeExists(ESP)).thenReturn(true);
        when(repo.existsById(new ProfissionalEspecialidadeId(PRO, ESP))).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private ProfissionalEspecialidade entity() {
        ProfissionalEspecialidade pe = new ProfissionalEspecialidade();
        pe.setProfissionalId(PRO);
        pe.setEspecialidadeId(ESP);
        pe.setSituacao((short) 1);
        pe.setPrioritario((short) 0);
        return pe;
    }
}
