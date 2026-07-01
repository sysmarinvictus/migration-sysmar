package br.gov.mandaguari.saude.parametro;

import br.gov.mandaguari.saude.common.audit.AuditProperties;
import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.parametro.domain.Parametro;
import br.gov.mandaguari.saude.parametro.dto.ParametroDtos.*;
import br.gov.mandaguari.saude.parametro.repository.ParametroRepository;
import br.gov.mandaguari.saude.parametro.service.ParametroService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Unit tests for the mined SAU_PAR rules (day-count validation R1/R2 + update mapping + audit). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ParametroServiceTest {

    static final int EMP = 1;

    @Mock ParametroRepository repo;
    @Mock AuditProperties tenant;
    @Mock AuditService audit;

    ParametroService service() { return new ParametroService(repo, tenant, audit); }

    private void stubTenant() {
        when(tenant.getEmpresaCodigo()).thenReturn(EMP);
        Parametro p = new Parametro();
        p.setEmpresaCod(EMP);
        when(repo.findById(EMP)).thenReturn(Optional.of(p));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private ParametroGeralUpdateRequest validGeral() {
        return new ParametroGeralUpdateRequest(true, 30, 90, 60, 30, 60,
                "SMS", "Rua X", "87100000", "(44) 3232-1000", "(44) 3232-1001", "sms@x.gov.br", false, true);
    }

    @Test void getReadsSingleton() {
        stubTenant();
        assertThat(service().get().empresaCod()).isEqualTo(EMP);
    }

    @Test void getThrowsWhenNotConfigured() {
        when(tenant.getEmpresaCodigo()).thenReturn(EMP);
        when(repo.findById(EMP)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().get()).isInstanceOf(NotFound.class);
    }

    @Test void inatividadeDiasRequired() { // R1
        stubTenant();
        var req = new ParametroGeralUpdateRequest(true, 30, 90, 60, 0, 60, "SMS", null, null, null, null, null, false, false);
        assertThatThrownBy(() -> service().updateGeral(req))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Informe a quantidade de dias");
    }

    @Test void senhaDiasRequired() { // R1
        stubTenant();
        var req = new ParametroGeralUpdateRequest(true, 30, 90, 60, 30, null, "SMS", null, null, null, null, null, false, false);
        assertThatThrownBy(() -> service().updateGeral(req))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Informe a quantidade de dias");
    }

    @Test void inatividadeDiasMax180() { // R2
        stubTenant();
        var req = new ParametroGeralUpdateRequest(true, 30, 90, 60, 200, 60, "SMS", null, null, null, null, null, false, false);
        assertThatThrownBy(() -> service().updateGeral(req))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("superior a 180");
    }

    @Test void senhaDiasMax180() { // R2
        stubTenant();
        var req = new ParametroGeralUpdateRequest(true, 30, 90, 60, 30, 999, "SMS", null, null, null, null, null, false, false);
        assertThatThrownBy(() -> service().updateGeral(req))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("superior a 180");
    }

    @Test void updatesGeralAndAudits() {
        stubTenant();
        var resp = service().updateGeral(validGeral());
        assertThat(resp.validadeReceitaSimplesDias()).isEqualTo(30);
        assertThat(resp.secretaria()).isEqualTo("SMS");
        verify(repo).save(argThat((Parametro p) -> p.getSenhaUsuarioDias() == 60 && "SMS".equals(p.getSecretaria())));
        verify(audit).record(eq("UPDATE"), eq("SAU_PAR_GER"), eq(EMP));
    }

    @Test void updatesAmbulatorialAndAudits() {
        stubTenant();
        var req = new ParametroAmbulatorialUpdateRequest(true, false, 1, 0);
        var resp = service().updateAmbulatorial(req);
        assertThat(resp.exigeCid10Atestado()).isTrue();
        assertThat(resp.imprimeRiscoMaterno()).isEqualTo(1);
        verify(audit).record(eq("UPDATE"), eq("SAU_PAR_AMB"), eq(EMP));
    }
}
