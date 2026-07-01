package br.gov.mandaguari.saude.paciente;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.paciente.domain.Paciente;
import br.gov.mandaguari.saude.paciente.dto.PacienteDtos.PacienteWriteRequest;
import br.gov.mandaguari.saude.paciente.repository.PacienteRepository;
import br.gov.mandaguari.saude.paciente.service.PacienteService;
import br.gov.mandaguari.saude.pessoa.domain.Pessoa;
import br.gov.mandaguari.saude.pessoa.repository.PessoaRepository;
import br.gov.mandaguari.saude.profissional.service.SoundexService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Unit tests for the patient-specific mined rules (SAU_PAC). Repos + audit mocked; real SoundexService. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PacienteServiceTest {

    static final String CNS_OK = "700000000000005";
    static final String CPF_OK = "11144477735";

    @Mock PacienteRepository repo;
    @Mock PessoaRepository pessoaRepo;
    @Mock AuditService audit;
    final SoundexService soundex = new SoundexService();

    PacienteService service() { return new PacienteService(repo, pessoaRepo, soundex, audit); }

    private void stubCreate() {
        when(pessoaRepo.findMaxPesCod()).thenReturn(Optional.of(100L));
        when(pessoaRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(pessoaRepo.findCpfOwners(any(), any(), any())).thenReturn(List.of());
        when(repo.findCnsOwnerAmongPatients(any(), anyLong())).thenReturn(Optional.empty());
        when(repo.unidadeExists(anyInt())).thenReturn(true);
        when(repo.unidadePermiteCadastroSemCpf(anyInt())).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    // ---- create happy + defaults ----

    @Test void createsPatientWithDefaults() { // R6 obito=0, R7 situacao=1, PK MAX+1
        stubCreate();
        var resp = service().create(valid().build());
        assertThat(resp.id()).isEqualTo(101L);
        assertThat(resp.obito()).isEqualTo(0);
        assertThat(resp.situacao()).isEqualTo(1);
        // person (tipoPessoa=1) + patient both saved; soundex derived
        verify(pessoaRepo).save(argThat((Pessoa p) -> p.getTipoPessoa() == 1 && p.getNomeSoundex() != null));
        verify(repo).save(argThat((Paciente pac) -> pac.getId() == 101L && pac.getNomeSoundex() != null
                && pac.getUsuarioInclusao() != null));
        verify(audit).record(eq("CREATE"), eq("SAU_PAC"), eq(101L));
    }

    // ---- R2 CPF-or-CNS ----

    @Test void requiresCpfOrCns() { // R2
        stubCreate();
        assertThatThrownBy(() -> service().create(valid().cns(null).cpfCnpj(null).build()))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Informe o CPF ou CNS");
    }

    @Test void unidadeExemptAllowsNoCpfNoCns() { // R2 exemption (SAU_UNI.UniCadCPF)
        stubCreate();
        when(repo.unidadePermiteCadastroSemCpf(9)).thenReturn(true);
        service().create(valid().cns(null).cpfCnpj(null).unidadeCod(9).build());
        verify(repo).save(any());
    }

    @Test void acceptsCpfOnlyNoCns() { // R2 satisfied by CPF alone
        stubCreate();
        service().create(valid().cns(null).cpfCnpj(CPF_OK).build());
        verify(repo).save(any());
    }

    // ---- CNS / CPF ----

    @Test void cnsInvalid() { // R4
        stubCreate();
        assertThatThrownBy(() -> service().create(valid().cns("123456789012345").build()))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("CNS inválido");
    }

    @Test void cnsMustBeUniqueAmongPatients() { // R3
        stubCreate();
        when(repo.findCnsOwnerAmongPatients(any(), anyLong())).thenReturn(Optional.of(777L));
        assertThatThrownBy(() -> service().create(valid().build()))
                .isInstanceOf(Conflict.class).hasMessageContaining("utilizado pelo paciente 777");
    }

    @Test void cpfInvalidWhenPresent() {
        stubCreate();
        assertThatThrownBy(() -> service().create(valid().cpfCnpj("12345678900").build()))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("CPF inválido");
    }

    @Test void cpfMustBeUniquePersonWide() {
        stubCreate();
        when(pessoaRepo.findCpfOwners(any(), any(), any())).thenReturn(List.of(555L));
        assertThatThrownBy(() -> service().create(valid().cpfCnpj(CPF_OK).build()))
                .isInstanceOf(Conflict.class).hasMessageContaining("cadastro 555");
    }

    // ---- other patient rules ----

    @Test void unidadeMustExistWhenSet() { // R5
        stubCreate();
        when(repo.unidadeExists(9)).thenReturn(false);
        assertThatThrownBy(() -> service().create(valid().unidadeCod(9).build()))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Não existe Unidade");
    }

    @Test void rendaFamiliarRange() { // R8
        stubCreate();
        assertThatThrownBy(() -> service().create(valid().rendaFamiliar(9).build()))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Renda Familiar fora do intervalo");
    }

    @Test void nomeSocialRequiredWhenUsaNomeSocial() { // R13
        stubCreate();
        assertThatThrownBy(() -> service().create(valid().usaNomeSocial(true).nomeSocial(null).build()))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Nome Social");
    }

    @Test void nomeRequired() { // inherited name quality
        stubCreate();
        assertThatThrownBy(() -> service().create(valid().nome(" ").build()))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Nome");
    }

    // ---- delete guard (R14/R15) ----

    @Test void blocksDeleteWhenControlledPrescriptionExists() { // R14 Portaria 344/98
        when(repo.findById(5L)).thenReturn(Optional.of(new Paciente()));
        when(repo.referencedByReceituarioControleEspecial(5L)).thenReturn(true);
        assertThatThrownBy(() -> service().delete(5L))
                .isInstanceOf(Conflict.class).hasMessageContaining("Controle Especial");
        verify(repo, never()).delete(any());
    }

    @Test void deleteRemovesOnlyPacienteKeepsPerson() { // R15 + audit
        when(repo.findById(5L)).thenReturn(Optional.of(new Paciente()));
        when(repo.referencedByReceituarioControleEspecial(5L)).thenReturn(false);
        service().delete(5L);
        verify(repo).delete(any());
        verify(pessoaRepo, never()).delete(any());     // SYS_PES person is preserved
        verify(audit).record(eq("DELETE"), eq("SAU_PAC"), eq(5L));
    }

    // ---- reads ----

    @Test void getAuditsPhiRead() {
        when(repo.findById(5L)).thenReturn(Optional.of(new Paciente()));
        when(pessoaRepo.findById(5L)).thenReturn(Optional.of(new Pessoa()));
        service().get(5L);
        verify(audit).record(eq("READ"), eq("SAU_PAC"), eq(5L));
    }

    @Test void getNotFound() {
        when(repo.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().get(9L)).isInstanceOf(NotFound.class);
    }

    // --- builder ---

    private static Builder valid() { return new Builder(); }

    static final class Builder {
        String nome = "Maria Silva", nomeSocial, nomeMae = "Ana Silva", nomePai, cpfCnpj, cns = CNS_OK,
                rg, sexo = "F", cep, endereco, numero, complemento, telefone, celular, email, cboCod,
                prontuario, alergia, historicoDoencas, meioTransporte, cnh;
        Boolean usaNomeSocial = false, inconsciente, situacaoRua, surtoPsiquiatrico, beneficioSocial;
        LocalDate dataNascimento = LocalDate.of(1990, 1, 1);
        Integer bairroCod, municipioCod, etniaCod, paisCod, unidadeCod, obito, rendaFamiliar, situacao;
        Long numeroIdentificacao;

        Builder nome(String v) { this.nome = v; return this; }
        Builder nomeSocial(String v) { this.nomeSocial = v; return this; }
        Builder usaNomeSocial(Boolean v) { this.usaNomeSocial = v; return this; }
        Builder cpfCnpj(String v) { this.cpfCnpj = v; return this; }
        Builder cns(String v) { this.cns = v; return this; }
        Builder unidadeCod(Integer v) { this.unidadeCod = v; return this; }
        Builder rendaFamiliar(Integer v) { this.rendaFamiliar = v; return this; }

        PacienteWriteRequest build() {
            return new PacienteWriteRequest(nome, nomeSocial, usaNomeSocial, nomeMae, nomePai, cpfCnpj, cns,
                    rg, dataNascimento, sexo, cep, endereco, numero, complemento, bairroCod, municipioCod,
                    telefone, celular, email, etniaCod, paisCod, cboCod, unidadeCod, prontuario,
                    numeroIdentificacao, alergia, historicoDoencas, obito, inconsciente, situacaoRua,
                    surtoPsiquiatrico, rendaFamiliar, meioTransporte, beneficioSocial, cnh, situacao);
        }
    }
}
