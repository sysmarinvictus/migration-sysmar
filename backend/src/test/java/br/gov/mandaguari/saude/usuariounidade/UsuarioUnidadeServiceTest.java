package br.gov.mandaguari.saude.usuariounidade;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.usuariounidade.domain.UsuarioUnidade;
import br.gov.mandaguari.saude.usuariounidade.domain.UsuarioUnidadeId;
import br.gov.mandaguari.saude.usuariounidade.dto.UsuarioUnidadeDtos.UsuarioUnidadeUpsertRequest;
import br.gov.mandaguari.saude.usuariounidade.repository.UsuarioUnidadeRepository;
import br.gov.mandaguari.saude.usuariounidade.service.UsuarioUnidadeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Unit tests for the mined SAU_USUUNI rules (R1-R6, R10). Repo + audit mocked. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioUnidadeServiceTest {

    static final int USU = 7, UNI = 3;

    @Mock UsuarioUnidadeRepository repo;
    @Mock AuditService audit;

    UsuarioUnidadeService service() { return new UsuarioUnidadeService(repo, audit); }

    static UsuarioUnidadeUpsertRequest reqAllNull() { return new UsuarioUnidadeUpsertRequest(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null); }
    static UsuarioUnidadeUpsertRequest reqAllTrue() { return new UsuarioUnidadeUpsertRequest(null, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true); }

    private void stubOk() {
        when(repo.usuarioExists(USU)).thenReturn(true);
        when(repo.unidadeExists(UNI)).thenReturn(true);
        when(repo.especialidadeExists(anyInt())).thenReturn(true);
        when(repo.existsById(any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test void createRejectsUnknownUsuCod() { // R1
        when(repo.usuarioExists(USU)).thenReturn(false);
        assertThatThrownBy(() -> service().create(USU, UNI, reqAllNull())).isInstanceOf(NotFound.class);
    }

    @Test void createRejectsUnknownUniCod() { // R2
        when(repo.usuarioExists(USU)).thenReturn(true);
        when(repo.unidadeExists(UNI)).thenReturn(false);
        assertThatThrownBy(() -> service().create(USU, UNI, reqAllNull()))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Unidade não existe");
    }

    @Test void especialidadeOptionalButMustExistWhenSet() { // R3
        when(repo.usuarioExists(USU)).thenReturn(true);
        when(repo.unidadeExists(UNI)).thenReturn(true);
        when(repo.existsById(any())).thenReturn(false);
        when(repo.especialidadeExists(9)).thenReturn(false);
        var req = new UsuarioUnidadeUpsertRequest(9, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        assertThatThrownBy(() -> service().create(USU, UNI, req))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Especialidade");
    }

    @Test void createRejectsDuplicate() { // R4
        stubOk();
        when(repo.existsById(new UsuarioUnidadeId(USU, UNI))).thenReturn(true);
        assertThatThrownBy(() -> service().create(USU, UNI, reqAllNull()))
                .isInstanceOf(Conflict.class).hasMessageContaining("já possui acesso");
    }

    @Test void createDefaultsUnsetFlagsToNull() { // R5 (nothing blocked/granted by default)
        stubOk();
        service().create(USU, UNI, reqAllNull());
        verify(repo).save(argThat((UsuarioUnidade e) ->
                e.getBloqueioFarmacia() == null && e.getBloqueioAgenda() == null && e.getPermiteBnafar() == null));
    }

    @Test void createPersistsAllFlagsWhenSet() { // full mapping round-trip
        stubOk();
        service().create(USU, UNI, reqAllTrue());
        verify(repo).save(argThat((UsuarioUnidade e) -> Boolean.TRUE.equals(e.getBloqueioTabela()) && Boolean.TRUE.equals(e.getBloqueioCadastro()) && Boolean.TRUE.equals(e.getBloqueioAmbulatorio()) && Boolean.TRUE.equals(e.getPermiteConsultaDireta()) && Boolean.TRUE.equals(e.getBloqueioProntuarioConsulta()) && Boolean.TRUE.equals(e.getBloqueioProntuarioOdonto()) && Boolean.TRUE.equals(e.getBloqueioResultadoExame()) && Boolean.TRUE.equals(e.getBloqueioEsus()) && Boolean.TRUE.equals(e.getBloqueioCaps()) && Boolean.TRUE.equals(e.getBloqueioNutricao()) && Boolean.TRUE.equals(e.getBloqueioFarmacia()) && Boolean.TRUE.equals(e.getPermiteBnafar()) && Boolean.TRUE.equals(e.getBloqueioAlmoxarifado()) && Boolean.TRUE.equals(e.getBloqueioRequisicao()) && Boolean.TRUE.equals(e.getBloqueioBeneficio()) && Boolean.TRUE.equals(e.getBloqueioTransporte()) && Boolean.TRUE.equals(e.getBloqueioVacina()) && Boolean.TRUE.equals(e.getBloqueioAgenda()) && Boolean.TRUE.equals(e.getBloqueioAgendaManual()) && Boolean.TRUE.equals(e.getBloqueioAgendaExterna()) && Boolean.TRUE.equals(e.getBloqueioAgendaEspecial()) && Boolean.TRUE.equals(e.getPermiteAgendaAuditor()) && Boolean.TRUE.equals(e.getBloqueioLaboratorio()) && Boolean.TRUE.equals(e.getBloqueioHospital()) && Boolean.TRUE.equals(e.getBloqueioVigilancia()) && Boolean.TRUE.equals(e.getBloqueioAgravo()) && Boolean.TRUE.equals(e.getBloqueioCms()) && Boolean.TRUE.equals(e.getBloqueioOuvidoria()) && Boolean.TRUE.equals(e.getBloqueioImpressao()) && Boolean.TRUE.equals(e.getBloqueioExportacao()) && Boolean.TRUE.equals(e.getBloqueioParametro()) && Boolean.TRUE.equals(e.getBloqueioRelatorio()) && Boolean.TRUE.equals(e.getBloqueioRelatorioTabela()) && Boolean.TRUE.equals(e.getBloqueioRelatorioCadastro()) && Boolean.TRUE.equals(e.getBloqueioRelatorioAmbulatorio()) && Boolean.TRUE.equals(e.getBloqueioRelatorioEsus()) && Boolean.TRUE.equals(e.getBloqueioRelatorioCaps()) && Boolean.TRUE.equals(e.getBloqueioRelatorioNutricao()) && Boolean.TRUE.equals(e.getBloqueioRelatorioVacina()) && Boolean.TRUE.equals(e.getBloqueioRelatorioFarmacia()) && Boolean.TRUE.equals(e.getBloqueioRelatorioAlmoxarifado()) && Boolean.TRUE.equals(e.getBloqueioRelatorioRequisicao()) && Boolean.TRUE.equals(e.getBloqueioRelatorioBeneficio()) && Boolean.TRUE.equals(e.getBloqueioRelatorioTransporte()) && Boolean.TRUE.equals(e.getBloqueioRelatorioAgenda()) && Boolean.TRUE.equals(e.getBloqueioRelatorioLaboratorio()) && Boolean.TRUE.equals(e.getBloqueioRelatorioHospital()) && Boolean.TRUE.equals(e.getBloqueioRelatorioVigilancia()) && Boolean.TRUE.equals(e.getBloqueioRelatorioAgravo()) && Boolean.TRUE.equals(e.getBloqueioRelatorioOuvidoria()) && Boolean.TRUE.equals(e.getBloqueioRelatorioExportacao()) && Boolean.TRUE.equals(e.getBloqueioGrafico()) && Boolean.TRUE.equals(e.getPermiteAgendaAuditorPcd()) && Boolean.TRUE.equals(e.getPermiteSoaBnafar())));
    }

    @Test void createAudits() { // R10
        stubOk();
        service().create(USU, UNI, reqAllNull());
        verify(audit).record(eq("CREATE"), eq("SAU_USUUNI"), eq("7/3"));
    }

    @Test void deleteIsUnconditionalAndAudits() { // R6 + R10
        when(repo.findById(new UsuarioUnidadeId(USU, UNI))).thenReturn(Optional.of(new UsuarioUnidade()));
        service().delete(USU, UNI);
        verify(repo).delete(any());
        verify(audit).record(eq("DELETE"), eq("SAU_USUUNI"), eq("7/3"));
    }

    @Test void updateNotFound() {
        when(repo.findById(new UsuarioUnidadeId(USU, 99))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().update(USU, 99, reqAllNull())).isInstanceOf(NotFound.class);
    }

    @Test void listRejectsUnknownUser() { // R1
        when(repo.usuarioExists(USU)).thenReturn(false);
        assertThatThrownBy(() -> service().list(USU)).isInstanceOf(NotFound.class);
    }
}
