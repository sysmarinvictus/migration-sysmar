package br.gov.mandaguari.saude.pessoa;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.pessoa.domain.Pessoa;
import br.gov.mandaguari.saude.pessoa.dto.PessoaDtos.PessoaCadastroRequest;
import br.gov.mandaguari.saude.pessoa.repository.PessoaRepository;
import br.gov.mandaguari.saude.pessoa.service.PessoaCadastroService;
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

/**
 * Unit tests for the mined SAU_PESF rules (person cadastro over SYS_PES). Repo + audit mocked; the real
 * {@link SoundexService} is used. Rule refs match the SLICE-SPEC citations (sau_pesf_impl.java).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PessoaCadastroServiceTest {

    static final String CNS_OK = "700000000000005";
    static final String CPF_OK = "11144477735";

    @Mock PessoaRepository repo;
    @Mock AuditService audit;
    final SoundexService soundex = new SoundexService();

    PessoaCadastroService service() {
        return new PessoaCadastroService(repo, soundex, audit);
    }

    /** Base: all lookups exist, CPF free, next id = 101, save echoes. */
    private void stubHappy() {
        when(repo.findMaxPesCod()).thenReturn(Optional.of(100L));
        when(repo.tipoLogradouroExists(any())).thenReturn(true);
        when(repo.bairroExists(any())).thenReturn(true);
        when(repo.municipioExists(any())).thenReturn(true);
        when(repo.etniaExists(any())).thenReturn(true);
        when(repo.paisExists(any())).thenReturn(true);
        when(repo.cborExists(any())).thenReturn(true);
        when(repo.orgaoEmissorExists(any())).thenReturn(true);
        when(repo.findCpfOwners(any(), any(), any())).thenReturn(List.of());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---- happy path + defaults + derivations ----

    @Test
    void createsValidPerson() {
        stubHappy();
        var resp = service().create(valid().build());
        assertThat(resp.id()).isEqualTo(101L);           // MAX+1
        assertThat(resp.tipoPessoa()).isEqualTo(2);      // R48
        assertThat(resp.dataCadastro()).isEqualTo(LocalDate.now()); // R49
        verify(audit).record(eq("CREATE"), eq("SYS_PES"), eq(101L));
    }

    @Test
    void defaultsSituacaoFamiliarToZero() { // R50
        stubHappy();
        service().create(valid().situacaoFamiliarCod(null).build());
        verify(repo).save(argThat(p -> p.getSituacaoFamiliarCod() != null && p.getSituacaoFamiliarCod() == 0));
    }

    @Test
    void derivesSoundex() { // R51/R52
        stubHappy();
        service().create(valid().nome("Joana Silva").nomeMae("Marta Souza").build());
        verify(repo).save(argThat(p -> p.getNomeSoundex() != null && !p.getNomeSoundex().isBlank()
                && p.getNomeMaeSoundex() != null && !p.getNomeMaeSoundex().isBlank()));
    }

    // ---- name rules (R1-R5) ----

    @Test void nomeRequired() { assertReject(valid().nome("  "), "Informe o Nome"); }        // R1
    @Test void nomeMinLength() { assertReject(valid().nome("Jo"), "três caracteres"); }       // R2
    @Test void nomeRequiresSurname() { assertReject(valid().nome("Maria"), "sobrenome"); }     // R3
    @Test void nomeNoDoubleSpace() { assertReject(valid().nome("Maria  Silva"), "espaçamento duplo"); } // R4
    @Test void nomeLettersOnly() { assertReject(valid().nome("Maria S1lva"), "apenas letras"); } // R5
    @Test void nomeAllowsAccents() { // R5 accent-permissive — must NOT reject
        stubHappy();
        service().create(valid().nome("José Antônio").build());
        verify(repo).save(any());
    }

    // ---- CNS / CPF (R42-R45) ----

    @Test void cnsRequired() { assertReject(valid().cns(null), "Informe o número do CNS"); }   // R42
    @Test void cnsInvalid() { assertReject(valid().cns("123456789012345"), "CNS inválido"); }  // R43
    @Test void cpfInvalidWhenPresent() { assertReject(valid().cpfCnpj("12345678900"), "CPF inválido"); } // R44
    @Test void cpfOptional() { // R44 — blank CPF is allowed
        stubHappy();
        service().create(valid().cpfCnpj(null).build());
        verify(repo).save(any());
    }
    @Test void cpfMustBeUnique() { // R45
        stubHappy();
        when(repo.findCpfOwners(any(), any(), any())).thenReturn(List.of(999L));
        assertThatThrownBy(() -> service().create(valid().cpfCnpj(CPF_OK).build()))
                .isInstanceOf(Conflict.class).hasMessageContaining("utilizado pelo cadastro 999");
    }

    // ---- birth / age (R19-R21) ----

    @Test void birthRequired() { assertReject(valid().dataNascimento(null), "Data de Nascimento"); } // R19
    @Test void birthNotFuture() { assertReject(valid().dataNascimento(LocalDate.now().plusDays(1)), "maior que a data atual"); } // R20
    @Test void ageMax130() { assertReject(valid().dataNascimento(LocalDate.now().minusYears(131)), "130 anos"); } // R21

    // ---- sexo / cor / etnia (R22-R25) ----

    @Test void sexoRequired() { assertReject(valid().sexo(" "), "Informe o Sexo"); }           // R22
    @Test void corRequired() { assertReject(valid().corCod(null), "Cor/Raça"); }               // R23
    @Test void indigenaRequiresEtnia() { assertReject(valid().corCod(5).etniaCod(null), "Código da Etnia"); } // R24
    @Test void etniaFkChecked() { // R25
        stubHappyLenient();
        when(repo.etniaExists(7)).thenReturn(false);
        assertThatThrownBy(() -> service().create(valid().corCod(5).etniaCod(7).build()))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Não existe Etnia");
    }

    // ---- nationality (R26-R35) ----

    @Test void nacionalidadeRequired() { assertReject(valid().nacionalidadeTipo(null), "Informe a Nacionalidade"); } // R26
    @Test void paisRequiredUnlessNaturalizado() { assertReject(valid().nacionalidadeTipo(1).paisCod(null), "Pais de Origem"); } // R27
    @Test void estrangeiroPaisNotBrasil() { // R28
        assertReject(valid().nacionalidadeTipo(2).paisCod(10).dataEntradaPais(LocalDate.of(2000,1,1)), "diferente do Brasil");
    }
    @Test void brasileiroPaisIsBrasil() { assertReject(valid().nacionalidadeTipo(1).paisCod(55), "deve ser o Brasil"); } // R29
    @Test void estrangeiroRequiresDataEntrada() { // R30
        assertReject(valid().nacionalidadeTipo(2).paisCod(55).dataEntradaPais(null).municipioNascCod(null), "Data de Entrada");
    }
    @Test void naturalizadoRequiresDataNat() { // R31
        assertReject(valid().nacionalidadeTipo(3).paisCod(null).municipioNascCod(null)
                .dataNaturalizacao(null).numeroPortaria("P1"), "Data de Naturalização");
    }
    @Test void naturalizadoRequiresPortaria() { // R32
        assertReject(valid().nacionalidadeTipo(3).paisCod(null).municipioNascCod(null)
                .dataNaturalizacao(LocalDate.of(2010,1,1)).numeroPortaria(" "), "Portaria de Naturalização");
    }
    @Test void brasileiroRequiresMunicipioNascimento() { // R33
        assertReject(valid().nacionalidadeTipo(1).paisCod(10).municipioNascCod(null), "município de nascimento");
    }
    @Test void paisFkChecked() { // R34
        stubHappyLenient();
        when(repo.paisExists(10)).thenReturn(false);
        assertThatThrownBy(() -> service().create(valid().build()))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Não existe Pais");
    }
    @Test void dataNaturalizacaoNotBeforeBirth() { // R35
        assertReject(valid().nacionalidadeTipo(3).paisCod(null).municipioNascCod(null)
                .numeroPortaria("P1").dataNaturalizacao(LocalDate.of(1980, 1, 1)), "anterior a data de nascimento");
    }
    @Test void dataNaturalizacaoNotFuture() { // R37
        assertReject(valid().nacionalidadeTipo(3).paisCod(null).municipioNascCod(null)
                .numeroPortaria("P1").dataNaturalizacao(LocalDate.now().plusYears(1)), "naturalização não pode ser maior");
    }
    @Test void dataEntradaNotBeforeBirth() { // R36
        assertReject(valid().nacionalidadeTipo(2).paisCod(55).municipioNascCod(null)
                .dataEntradaPais(LocalDate.of(1980, 1, 1)), "entrada não pode ser anterior");
    }
    @Test void dataEntradaNotFuture() { // R38
        assertReject(valid().nacionalidadeTipo(2).paisCod(55).municipioNascCod(null)
                .dataEntradaPais(LocalDate.now().plusYears(1)), "entrada não pode ser maior");
    }
    @Test void orgaoEmissorFkChecked() { // R47
        stubHappyLenient();
        when(repo.orgaoEmissorExists(5)).thenReturn(false);
        assertThatThrownBy(() -> service().create(valid().orgaoEmissorCod(5).build()))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Não existe Orgão Emissor");
    }

    // ---- address (R10-R16) ----

    @Test void cepRequired() { assertReject(valid().cep(" "), "Informe o CEP"); }              // R10
    @Test void tipoLogradouroRequired() { assertReject(valid().tipoLogradouroCod(null), "código do logradouro"); } // R12
    @Test void tipoLogradouroFk() { // R12
        stubHappyLenient();
        when(repo.tipoLogradouroExists(9)).thenReturn(false);
        assertThatThrownBy(() -> service().create(valid().tipoLogradouroCod(9).build()))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Não existe Tipo Logradouro");
    }
    @Test void logradouroRequired() { assertReject(valid().endereco(" "), "Informe o logradouro"); } // R13
    @Test void numeroInvalid() { assertReject(valid().enderecoNumero("A1"), "Número inválido"); } // R14
    @Test void numeroAllowsSN() { stubHappy(); service().create(valid().enderecoNumero("SN").build()); verify(repo).save(any()); } // R14
    @Test void bairroRequired() { assertReject(valid().bairroCod(null), "Informe o bairro"); }  // R15
    @Test void municipioRequired() { assertReject(valid().municipioCod(null), "Município da Pessoa"); } // R16

    // ---- contact / cbo (R17/R46) ----

    @Test void telefoneFormat() { assertReject(valid().telefone("123"), "Telefone inválido"); } // R17
    @Test void telefoneValidAccepted() { stubHappy(); service().create(valid().telefone("(44) 99999-8888").build()); verify(repo).save(any()); }
    @Test void cboFkChecked() { // R46
        stubHappyLenient();
        when(repo.cborExists("999999")).thenReturn(false);
        assertThatThrownBy(() -> service().create(valid().cboCod("999999").build()))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Não existe CBOR");
    }

    // ---- delete-guards (R53-R55) ----

    @Test void blocksDeleteWhenProfissional() { // R53
        when(repo.findById(5L)).thenReturn(Optional.of(new Pessoa()));
        when(repo.referencedByProfissional(5L)).thenReturn(true);
        assertThatThrownBy(() -> service().delete(5L)).isInstanceOf(Conflict.class).hasMessageContaining("Profissional");
        verify(repo, never()).delete(any());
    }
    @Test void blocksDeleteWhenFuncionario() { // R54
        when(repo.findById(5L)).thenReturn(Optional.of(new Pessoa()));
        when(repo.referencedByFuncionario(5L)).thenReturn(true);
        assertThatThrownBy(() -> service().delete(5L)).isInstanceOf(Conflict.class).hasMessageContaining("Funcionário");
    }
    @Test void blocksDeleteWhenPaciente() { // R55
        when(repo.findById(5L)).thenReturn(Optional.of(new Pessoa()));
        when(repo.referencedByPaciente(5L)).thenReturn(true);
        assertThatThrownBy(() -> service().delete(5L)).isInstanceOf(Conflict.class).hasMessageContaining("Paciente");
    }
    @Test void deletesWhenUnreferenced() {
        when(repo.findById(5L)).thenReturn(Optional.of(new Pessoa()));
        service().delete(5L);
        verify(repo).delete(any());
        verify(audit).record(eq("DELETE"), eq("SYS_PES"), eq(5L));
    }
    @Test void updateNotFound() {
        when(repo.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().update(9L, valid().build())).isInstanceOf(NotFound.class);
    }

    // --- helpers ---

    private void assertReject(Builder b, String msgFragment) {
        stubHappyLenient();
        assertThatThrownBy(() -> service().create(b.build()))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining(msgFragment);
    }

    /** Lenient happy stubs that don't fail if unused (for validation tests that throw before save). */
    private void stubHappyLenient() {
        lenient().when(repo.findMaxPesCod()).thenReturn(Optional.of(100L));
        lenient().when(repo.tipoLogradouroExists(any())).thenReturn(true);
        lenient().when(repo.bairroExists(any())).thenReturn(true);
        lenient().when(repo.municipioExists(any())).thenReturn(true);
        lenient().when(repo.etniaExists(any())).thenReturn(true);
        lenient().when(repo.paisExists(any())).thenReturn(true);
        lenient().when(repo.cborExists(any())).thenReturn(true);
        lenient().when(repo.orgaoEmissorExists(any())).thenReturn(true);
        lenient().when(repo.findCpfOwners(any(), any(), any())).thenReturn(List.of());
        lenient().when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Builder valid() { return new Builder(); }

    /** Mutable builder over the immutable request — all fields default to a valid brasileiro person. */
    static final class Builder {
        String nome = "Maria Silva", nomeSocial, nomePai, nomeMae = "Ana Silva", nomeConjuge;
        Boolean usaNomeSocial = false;
        String cpfCnpj, cns = CNS_OK, rgIe, rgUf, tipoSanguineo;
        Integer orgaoEmissorCod, corCod = 1, estadoCivilCod, situacaoFamiliarCod = 0, etniaCod;
        LocalDate rgDataEmissao, dataNascimento = LocalDate.of(1990, 1, 1), dataNaturalizacao, dataEntradaPais;
        String sexo = "F";
        Integer nacionalidadeTipo = 1, paisCod = 10, municipioNascCod = 411420;
        String numeroPortaria, cep = "87100000", endereco = "Rua das Flores", enderecoNumero = "100",
                enderecoComplemento;
        Integer tipoLogradouroCod = 1, bairroCod = 1, municipioCod = 411420;
        String telefone, celular, fax, email, homePage;
        Integer certidaoCivilTipo;
        String certidaoNumero, certidaoLivro, certidaoFolha, certidaoCartorio;
        LocalDate certidaoData, ctpsData;
        String ctpsSerie, ctpsNumero, ctpsUf, pisPasep, tituloEleitorNumero, tituloEleitorZona,
                tituloEleitorSecao, nis, frequentaEscola, cboCod, observacao;
        Integer grauEscolaridade, escolaridade, gerarBpa;

        Builder nome(String v) { this.nome = v; return this; }
        Builder nomeMae(String v) { this.nomeMae = v; return this; }
        Builder cns(String v) { this.cns = v; return this; }
        Builder cpfCnpj(String v) { this.cpfCnpj = v; return this; }
        Builder dataNascimento(LocalDate v) { this.dataNascimento = v; return this; }
        Builder sexo(String v) { this.sexo = v; return this; }
        Builder corCod(Integer v) { this.corCod = v; return this; }
        Builder etniaCod(Integer v) { this.etniaCod = v; return this; }
        Builder situacaoFamiliarCod(Integer v) { this.situacaoFamiliarCod = v; return this; }
        Builder nacionalidadeTipo(Integer v) { this.nacionalidadeTipo = v; return this; }
        Builder paisCod(Integer v) { this.paisCod = v; return this; }
        Builder municipioNascCod(Integer v) { this.municipioNascCod = v; return this; }
        Builder dataNaturalizacao(LocalDate v) { this.dataNaturalizacao = v; return this; }
        Builder numeroPortaria(String v) { this.numeroPortaria = v; return this; }
        Builder dataEntradaPais(LocalDate v) { this.dataEntradaPais = v; return this; }
        Builder cep(String v) { this.cep = v; return this; }
        Builder tipoLogradouroCod(Integer v) { this.tipoLogradouroCod = v; return this; }
        Builder endereco(String v) { this.endereco = v; return this; }
        Builder enderecoNumero(String v) { this.enderecoNumero = v; return this; }
        Builder bairroCod(Integer v) { this.bairroCod = v; return this; }
        Builder municipioCod(Integer v) { this.municipioCod = v; return this; }
        Builder telefone(String v) { this.telefone = v; return this; }
        Builder cboCod(String v) { this.cboCod = v; return this; }
        Builder orgaoEmissorCod(Integer v) { this.orgaoEmissorCod = v; return this; }

        PessoaCadastroRequest build() {
            return new PessoaCadastroRequest(nome, nomeSocial, usaNomeSocial, nomePai, nomeMae, nomeConjuge,
                    cpfCnpj, cns, rgIe, orgaoEmissorCod, rgUf, rgDataEmissao, dataNascimento, sexo, corCod,
                    estadoCivilCod, situacaoFamiliarCod, etniaCod, tipoSanguineo, nacionalidadeTipo, paisCod,
                    municipioNascCod, dataNaturalizacao, numeroPortaria, dataEntradaPais, cep, tipoLogradouroCod,
                    endereco, enderecoNumero, enderecoComplemento, bairroCod, municipioCod, telefone, celular,
                    fax, email, homePage, certidaoCivilTipo, certidaoNumero, certidaoLivro, certidaoFolha,
                    certidaoData, certidaoCartorio, ctpsSerie, ctpsNumero, ctpsUf, ctpsData, pisPasep,
                    tituloEleitorNumero, tituloEleitorZona, tituloEleitorSecao, nis, frequentaEscola,
                    grauEscolaridade, escolaridade, cboCod, observacao, gerarBpa);
        }
    }
}
