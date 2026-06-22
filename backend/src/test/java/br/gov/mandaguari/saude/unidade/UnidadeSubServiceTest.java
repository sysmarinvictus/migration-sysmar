package br.gov.mandaguari.saude.unidade;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.unidade.domain.Sala;
import br.gov.mandaguari.saude.unidade.domain.SalaId;
import br.gov.mandaguari.saude.unidade.dto.UnidadeSubDtos.*;
import br.gov.mandaguari.saude.unidade.repository.*;
import br.gov.mandaguari.saude.unidade.service.UnidadeSubService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UnidadeSubServiceTest {

    @Mock HiperdiaProfissionalRepository hiperdiaRepo;
    @Mock SisPreNatalProfissionalRepository sisPreNatalRepo;
    @Mock NutricionistaProfissionalRepository nutricionistaRepo;
    @Mock SalaRepository salaRepo;
    @Mock AuditService audit;

    UnidadeSubService service;

    @BeforeEach
    void setup() {
        service = new UnidadeSubService(hiperdiaRepo, sisPreNatalRepo, nutricionistaRepo, salaRepo, audit);
    }

    // Duplicate (UniCod, SalaCod) must conflict (found via parity scenario #20).
    @Test
    void addSalaRejectsDuplicateSalaCod() {
        given(salaRepo.existsById(new SalaId(1, (short) 1))).willReturn(true);
        assertThatThrownBy(() -> service.addSala(1, new SalaCriarRequest((short) 1, "Sala A", "A")))
                .isInstanceOf(Conflict.class)
                .hasMessageContaining("código 1");
    }

    // R82: addSala auto-stamps SalaDatAlt/SalaUsuLogin (never trusts the caller).
    @Test
    void addSalaStampsDateAndUser() {
        given(salaRepo.existsById(any())).willReturn(false);
        given(salaRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
        SalaResponse res = service.addSala(1, new SalaCriarRequest((short) 5, "Triagem", "A"));
        assertThat(res.dataAlteracao()).isNotNull();
        assertThat(res.usuarioLogin()).isNotBlank(); // "sistema" when no auth context
    }

    // R57: Hiperdia requires dataInclusao.
    @Test
    void addHiperdiaRequiresDataInclusao() {
        assertThatThrownBy(() -> service.addHiperdia(1,
                new HiperdiaCriarRequest(500L, null, null, null, (short) 1, null)))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("Data de Inclusão");
    }

    // R59: Hiperdia status defaults to ATIVO (1) when omitted.
    @Test
    void addHiperdiaDefaultsStatusToAtivo() {
        given(hiperdiaRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
        HiperdiaResponse res = service.addHiperdia(1,
                new HiperdiaCriarRequest(500L, LocalDate.of(2026, 1, 1), null, null, null, null));
        assertThat(res.status()).isEqualTo((short) 1);
    }
}
