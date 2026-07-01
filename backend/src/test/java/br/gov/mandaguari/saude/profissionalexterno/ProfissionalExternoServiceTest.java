package br.gov.mandaguari.saude.profissionalexterno;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.conselhoclasse.repository.ConselhoClasseRepository;
import br.gov.mandaguari.saude.pessoa.domain.Pessoa;
import br.gov.mandaguari.saude.pessoa.repository.PessoaRepository;
import br.gov.mandaguari.saude.profissional.domain.Profissional;
import br.gov.mandaguari.saude.profissional.repository.ProfissionalRepository;
import br.gov.mandaguari.saude.profissional.service.SoundexService;
import br.gov.mandaguari.saude.profissionalexterno.dto.ProfissionalExternoDtos.ProfissionalExternoCreateRequest;
import br.gov.mandaguari.saude.profissionalexterno.service.ProfissionalExternoService;
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

/**
 * Unit tests for SAU_PESF_PROFEXT (external-professional composite create). Repos + audit mocked; the
 * real {@link SoundexService} is used. Rule refs match the SLICE-SPEC citations.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfissionalExternoServiceTest {

    static final String CNS_OK = "700000000000005";

    @Mock PessoaRepository pessoaRepo;
    @Mock ProfissionalRepository profissionalRepo;
    @Mock ConselhoClasseRepository conselhoRepo;
    @Mock AuditService audit;
    final SoundexService soundex = new SoundexService();

    ProfissionalExternoService service() {
        return new ProfissionalExternoService(pessoaRepo, profissionalRepo, conselhoRepo, soundex, audit);
    }

    private void stubHappy() {
        when(pessoaRepo.municipioExists(411420)).thenReturn(true);
        when(pessoaRepo.findMaxPesCod()).thenReturn(Optional.of(100L));
        when(pessoaRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(profissionalRepo.findCnsOwner(anyString(), anyLong())).thenReturn(Optional.empty());
        when(profissionalRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(conselhoRepo.existsById((short) 1)).thenReturn(true);
    }

    private ProfissionalExternoCreateRequest valid() {
        return new ProfissionalExternoCreateRequest("Maria Silva", CNS_OK, 411420, (short) 1, "12345", null);
    }

    @Test
    void createsExternalProfessional() { // R24/R25/R26 + R28/R29
        stubHappy();
        var resp = service().create(valid());
        assertThat(resp.id()).isEqualTo(101L);        // MAX+1
        assertThat(resp.externo()).isEqualTo((short) 1);
        assertThat(resp.situacao()).isEqualTo((short) 1);
        assertThat(resp.numeroConselho()).isEqualTo("12345");
        // person: PesTip=1, nome uppercased (R12), soundex set (R26)
        verify(pessoaRepo).save(argThat((Pessoa p) ->
                p.getId() == 101L && p.getTipoPessoa() == 1
                        && "MARIA SILVA".equals(p.getNome())
                        && p.getNomeSoundex() != null && !p.getNomeSoundex().isBlank()));
        // professional: externo=1, situacao=1, conselho + numeroCr, no certificate
        verify(profissionalRepo).save(argThat((Profissional pr) ->
                pr.getId() == 101L && pr.getExterno() == 1 && pr.getSituacao() == 1
                        && pr.getConselhoClasseCod() == (short) 1 && "12345".equals(pr.getNumeroCr())
                        && pr.getCertificado() == null));
        verify(audit).record(eq("CREATE"), eq("SAU_PESF_PROFEXT"), eq(101L));
    }

    @Test void nomeRequired() { // R3
        stubHappy();
        assertThatThrownBy(() -> service().create(new ProfissionalExternoCreateRequest(" ", CNS_OK, 411420, (short) 1, "1", null)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Nome");
    }

    @Test void cnsRequired() { // R13
        stubHappy();
        assertThatThrownBy(() -> service().create(new ProfissionalExternoCreateRequest("Maria Silva", null, 411420, (short) 1, "1", null)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("CNS");
    }

    @Test void cnsInvalid() { // R16
        stubHappy();
        assertThatThrownBy(() -> service().create(new ProfissionalExternoCreateRequest("Maria Silva", "123456789012345", 411420, (short) 1, "1", null)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("CNS inválido");
    }

    @Test void cnsMustBeUniqueAmongProfessionals() { // R17
        stubHappy();
        when(profissionalRepo.findCnsOwner(anyString(), anyLong())).thenReturn(Optional.of(555L));
        assertThatThrownBy(() -> service().create(valid()))
                .isInstanceOf(Conflict.class).hasMessageContaining("utilizado pelo cadastro 555");
    }

    @Test void municipioRequired() { // R18
        stubHappy();
        assertThatThrownBy(() -> service().create(new ProfissionalExternoCreateRequest("Maria Silva", CNS_OK, null, (short) 1, "1", null)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Município");
    }

    @Test void municipioMustExist() { // R19
        stubHappy();
        when(pessoaRepo.municipioExists(999)).thenReturn(false);
        assertThatThrownBy(() -> service().create(new ProfissionalExternoCreateRequest("Maria Silva", CNS_OK, 999, (short) 1, "1", null)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Não existe Município");
    }

    @Test void numeroConselhoRequired() { // R20
        stubHappy();
        assertThatThrownBy(() -> service().create(new ProfissionalExternoCreateRequest("Maria Silva", CNS_OK, 411420, (short) 1, " ", null)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Número do Conselho");
    }

    @Test void conselhoRequired() { // R21
        stubHappy();
        assertThatThrownBy(() -> service().create(new ProfissionalExternoCreateRequest("Maria Silva", CNS_OK, 411420, null, "1", null)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Informe o Conselho");
    }

    @Test void conselhoMustExist() { // R21/R22 (safe improvement over legacy soft lookup)
        stubHappy();
        when(conselhoRepo.existsById((short) 9)).thenReturn(false);
        assertThatThrownBy(() -> service().create(new ProfissionalExternoCreateRequest("Maria Silva", CNS_OK, 411420, (short) 9, "1", null)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Não existe Conselho");
    }

    @Test void firstPersonGetsIdOne() { // R25 empty table → 1
        stubHappy();
        when(pessoaRepo.findMaxPesCod()).thenReturn(Optional.empty());
        assertThat(service().create(valid()).id()).isEqualTo(1L);
    }

    @Test void getThrowsWhenNotFound() {
        when(pessoaRepo.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().get(9L)).isInstanceOf(NotFound.class);
    }
}
