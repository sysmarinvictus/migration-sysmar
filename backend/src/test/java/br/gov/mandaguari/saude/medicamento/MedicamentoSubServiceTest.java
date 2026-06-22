package br.gov.mandaguari.saude.medicamento;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.medicamento.domain.*;
import br.gov.mandaguari.saude.medicamento.dto.MedicamentoSubDtos.*;
import br.gov.mandaguari.saude.medicamento.repository.*;
import br.gov.mandaguari.saude.medicamento.service.MedicamentoSubService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MedicamentoSubServiceTest {

    @Mock MedicamentoRepository remRepo;
    @Mock MedicamentoUnidadeRepository unidadeRepo;
    @Mock MedicamentoEan13Repository ean13Repo;
    @Mock MedicamentoUnidadeSetorRepository uniSetorRepo;
    @Mock MedicamentoPosologiaRepository posologiaRepo;
    @Mock AuditService audit;

    MedicamentoSubService service;

    @BeforeEach
    void setup() {
        service = new MedicamentoSubService(remRepo, unidadeRepo, ean13Repo, uniSetorRepo, posologiaRepo, audit);
        given(remRepo.findById(any())).willReturn(Optional.of(med(1, false)));
        given(uniSetorRepo.findByRemCodOrderBySequencia(1)).willReturn(List.of()); // R36 sync no-op
        given(unidadeRepo.findByRemCodOrderByRemUniCod(1)).willReturn(List.of());
    }

    // R32
    @Test void addUnidadeRejectsNonExistentUnidade() {
        given(remRepo.unidadeExists(9)).willReturn(false);
        assertThatThrownBy(() -> service.addUnidade(1, new UnidadeCriarRequest(9, null, (short) 1)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Não existe Unidade");
    }

    // duplicate unidade
    @Test void addUnidadeRejectsDuplicate() {
        given(remRepo.unidadeExists(7)).willReturn(true);
        given(unidadeRepo.existsById(new MedicamentoUnidadeId(1, 7))).willReturn(true);
        assertThatThrownBy(() -> service.addUnidade(1, new UnidadeCriarRequest(7, null, (short) 1)))
                .isInstanceOf(Conflict.class);
    }

    // R27
    @Test void removeUnidadeBlockedByRemlot() {
        given(unidadeRepo.existsById(new MedicamentoUnidadeId(1, 7))).willReturn(true);
        given(remRepo.isRem1ReferencedByRemlot(1, 7)).willReturn(true);
        assertThatThrownBy(() -> service.removeUnidade(1, 7)).isInstanceOf(Conflict.class);
    }

    // R28
    @Test void addUnidadeSetorRejectsNonExistentUnidade() {
        given(remRepo.unidadeExists(9)).willReturn(false);
        assertThatThrownBy(() -> service.addUnidadeSetor(1, new UnidadeSetorCriarRequest(9, 0, null, (short) 1)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Unidade e Setor");
    }

    // R29
    @Test void addUnidadeSetorRejectsNonExistentPair() {
        given(remRepo.unidadeExists(7)).willReturn(true);
        given(remRepo.unidadeSetorExists(7, 3)).willReturn(false);
        assertThatThrownBy(() -> service.addUnidadeSetor(1, new UnidadeSetorCriarRequest(7, 3, null, (short) 1)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Unidade e Setor");
    }

    // R30
    @Test void addUnidadeSetorRejectsDuplicate() {
        given(remRepo.unidadeExists(7)).willReturn(true);
        given(uniSetorRepo.countByUnidadeSetorRem(7, null, 1)).willReturn(1L);
        assertThatThrownBy(() -> service.addUnidadeSetor(1, new UnidadeSetorCriarRequest(7, null, null, (short) 1)))
                .isInstanceOf(Conflict.class);
    }

    // R14: sequence assigned from RemUniSetorSeqUlt+1; parent counter incremented
    @Test void addUnidadeSetorAssignsSequenceFromCounter() {
        Medicamento parent = med(1, false); parent.setUltimaSequenciaUnidadeSetor(5);
        given(remRepo.findById(1)).willReturn(Optional.of(parent));
        given(remRepo.unidadeExists(7)).willReturn(true);
        given(uniSetorRepo.countByUnidadeSetorRem(7, null, 1)).willReturn(0L);
        given(uniSetorRepo.save(any())).willAnswer(i -> i.getArgument(0));

        UnidadeSetorResponse res = service.addUnidadeSetor(1, new UnidadeSetorCriarRequest(7, null, null, (short) 1));

        assertThat(res.sequencia()).isEqualTo(6);                 // R14: 5 + 1
        assertThat(parent.getUltimaSequenciaUnidadeSetor()).isEqualTo(6);
        verify(remRepo).save(parent);
    }

    // R45
    @Test void addPosologiaRejectedWhenUsarPosologiaOff() {
        given(remRepo.findById(1)).willReturn(Optional.of(med(1, false))); // usarPosologia=0
        assertThatThrownBy(() -> service.addPosologia(1, new PosologiaCriarRequest(3)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("não utiliza posologia");
    }

    // R34
    @Test void addPosologiaRejectsNonExistentPosologia() {
        given(remRepo.findById(1)).willReturn(Optional.of(med(1, true))); // usarPosologia=1
        given(remRepo.posologiaExists(3)).willReturn(false);
        assertThatThrownBy(() -> service.addPosologia(1, new PosologiaCriarRequest(3)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("posologia");
    }

    // R35
    @Test void addPosologiaRejectsDuplicate() {
        given(remRepo.findById(1)).willReturn(Optional.of(med(1, true)));
        given(remRepo.posologiaExists(3)).willReturn(true);
        given(posologiaRepo.existsById(new MedicamentoPosologiaId(1, 3))).willReturn(true);
        assertThatThrownBy(() -> service.addPosologia(1, new PosologiaCriarRequest(3))).isInstanceOf(Conflict.class);
    }

    // R23-R26: cascade order REMPOSO → REM2 → REM_UNISETOR → REM1
    @Test void cascadeDeleteCallsAllSubRepos() {
        service.cascadeDeleteForMedicamento(1);
        verify(posologiaRepo).deleteByRemCod(1);
        verify(ean13Repo).deleteByRemCod(1);
        verify(uniSetorRepo).deleteByRemCod(1);
        verify(unidadeRepo).deleteByRemCod(1);
    }

    // R33
    @Test void addEan13RejectsDuplicate() {
        given(ean13Repo.existsById(new MedicamentoEan13Id(1, 789L))).willReturn(true);
        assertThatThrownBy(() -> service.addEan13(1, new Ean13CriarRequest(789L))).isInstanceOf(Conflict.class);
    }

    private static Medicamento med(Integer id, Boolean usarPosologia) {
        Medicamento m = new Medicamento();
        m.setId(id);
        m.setNome("MED " + id);
        m.setUsarPosologia(usarPosologia);
        return m;
    }
}
