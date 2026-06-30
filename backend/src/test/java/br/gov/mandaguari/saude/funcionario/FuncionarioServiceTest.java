package br.gov.mandaguari.saude.funcionario;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.funcionario.domain.Funcionario;
import br.gov.mandaguari.saude.funcionario.dto.FuncionarioDtos.*;
import br.gov.mandaguari.saude.funcionario.repository.FuncionarioRepository;
import br.gov.mandaguari.saude.funcionario.repository.PersonProjection;
import br.gov.mandaguari.saude.funcionario.service.FuncionarioService;
import br.gov.mandaguari.saude.profissional.service.SoundexService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the mined SAU_FUN (Funcionário) rules. Repository + audit mocked; the real
 * {@link SoundexService} is wired (R3 asserts an actual phonetic key). Rule refs match SLICE-SPEC SAU_FUN.
 * Synthetic, non-PHI identifiers only.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FuncionarioServiceTest {

    static final long PES = 100L;

    @Mock FuncionarioRepository repo;
    @Mock AuditService audit;
    final SoundexService soundex = new SoundexService();

    FuncionarioService service() { return new FuncionarioService(repo, soundex, audit); }

    /** Plain PersonProjection stub (not a Mockito mock — avoids nested stubbing). */
    private PersonProjection person(String nome) {
        return new PersonProjection() {
            public String getNome() { return nome; }
            public String getCpfCnpj() { return null; }
            public String getTelefone() { return null; }
            public String getCelular() { return null; }
        };
    }

    private void stubHappyCreate(String nome) {
        when(repo.personExists(PES)).thenReturn(true);
        when(repo.existsById(PES)).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repo.findPerson(PES)).thenReturn(Optional.of(person(nome)));
    }

    private PessoaSubRequest pessoa(String nome, String cpf, String fone, String cel) {
        return new PessoaSubRequest(nome, cpf, fone, cel);
    }

    private FuncionarioCreateRequest create(String tel, String ramal, Short sit, PessoaSubRequest p) {
        return new FuncionarioCreateRequest(PES, tel, ramal, sit, p);
    }

    private FuncionarioCreateRequest validCreate() {
        return create("(44) 3232-3232", "201", null, pessoa("Maria Sintetica", null, null, null));
    }

    // R1 — person must exist
    @Test
    void rejectsCreateWhenPersonDoesNotExist() {
        when(repo.personExists(PES)).thenReturn(false);
        assertThatThrownBy(() -> service().create(validCreate()))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Não existe Pessoa");
    }

    // subtype — only one funcionário per person
    @Test
    void rejectsCreateWhenFuncionarioAlreadyExists() {
        when(repo.personExists(PES)).thenReturn(true);
        when(repo.existsById(PES)).thenReturn(true);
        assertThatThrownBy(() -> service().create(validCreate()))
                .isInstanceOf(Conflict.class).hasMessageContaining("Já existe Funcionário");
    }

    // R5 — situacao defaults to 1 (ATIVO) on insert
    @Test
    void defaultsSituacaoToAtivoOnInsert() {
        stubHappyCreate("Maria");
        service().create(validCreate());
        verify(repo).save(argThat(f -> f.getSituacao() == (short) 1));
    }

    @Test
    void honorsExplicitSituacao() {
        stubHappyCreate("Maria");
        service().create(create("(44) 3232-3232", null, (short) 2, pessoa("Maria", null, null, null)));
        verify(repo).save(argThat(f -> f.getSituacao() == (short) 2));
    }

    // R3 — soundex recomputed from the SYS_PES name (real SoundexService output)
    @Test
    void computesSoundexFromName() {
        stubHappyCreate("PHILIPE");
        service().create(create("(44) 3232-3232", null, null, pessoa("PHILIPE", null, null, null)));
        verify(repo).save(argThat(f -> "FIRIPE".equals(f.getNomeSoundex())));
    }

    // R2 — writes back person name/cpf/phones to SYS_PES
    @Test
    void writesBackPersonFieldsToSysPes() {
        stubHappyCreate("Maria");
        var req = create("(44) 3232-3232", null, null,
                pessoa("Maria Atualizada", "01111111294", "(44) 3232-3232", "(44) 99999-8888"));
        service().create(req);
        verify(repo).updatePerson(PES, "Maria Atualizada", "01111111294", "(44) 3232-3232", "(44) 99999-8888");
    }

    // R8 — work phone format when present
    @Test
    void rejectsInvalidWorkPhoneWhenPresent() {
        when(repo.personExists(PES)).thenReturn(true);
        when(repo.existsById(PES)).thenReturn(false);
        assertThatThrownBy(() -> service().create(create("123", null, null, pessoa("X", null, null, null))))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Telefone de trabalho inválido");
    }

    // R6 — person phone format when present
    @Test
    void rejectsInvalidPersonPhoneWhenPresent() {
        when(repo.personExists(PES)).thenReturn(true);
        when(repo.existsById(PES)).thenReturn(false);
        assertThatThrownBy(() -> service().create(create(null, null, null, pessoa("X", null, "44322", null))))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Telefone inválido");
    }

    // R7 — person mobile format when present
    @Test
    void rejectsInvalidPersonMobileWhenPresent() {
        when(repo.personExists(PES)).thenReturn(true);
        when(repo.existsById(PES)).thenReturn(false);
        assertThatThrownBy(() -> service().create(create(null, null, null, pessoa("X", null, null, "99999-8888"))))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Celular inválido");
    }

    // R9 — ramal is free text (no validation)
    @Test
    void ramalIsUnvalidatedFreeText() {
        stubHappyCreate("Maria");
        service().create(create("(44) 3232-3232", "RAMAL-XYZ", null, pessoa("Maria", null, null, null)));
        verify(repo).save(argThat(f -> "RAMAL-XYZ".equals(f.getRamal())));
    }

    // R12 — NO CPF/CNS check-digit validation (divergence from SAU_PRO): an invalid CPF is accepted
    @Test
    void doesNotValidateCpfCheckDigits() {
        stubHappyCreate("Maria");
        var req = create("(44) 3232-3232", null, null, pessoa("Maria", "12345678900", null, null));
        assertThatCode(() -> service().create(req)).doesNotThrowAnyException();
        verify(repo).updatePerson(eq(PES), any(), eq("12345678900"), any(), any());
    }

    // R17 — audit on create (legacy did NOT audit; carry-forward)
    @Test
    void auditsCreate() {
        stubHappyCreate("Maria");
        service().create(validCreate());
        verify(audit).record(eq("CREATE"), eq("SAU_FUN"), eq(PES));
    }

    // R13 — delete blocked when referenced by a system user
    @Test
    void blocksDeleteWhenReferencedByUsuario() {
        Funcionario f = new Funcionario(); f.setId(PES);
        when(repo.findById(PES)).thenReturn(Optional.of(f));
        when(repo.hasSystemUser(PES)).thenReturn(true);
        assertThatThrownBy(() -> service().delete(PES))
                .isInstanceOf(Conflict.class).hasMessageContaining("usuário do sistema");
        verify(repo, never()).delete(any(Funcionario.class));
    }

    // R14 — delete blocked when referenced by SAU_RECESP (controlled substances)
    @Test
    void blocksDeleteWhenReferencedByControlledPrescription() {
        Funcionario f = new Funcionario(); f.setId(PES);
        when(repo.findById(PES)).thenReturn(Optional.of(f));
        when(repo.hasSystemUser(PES)).thenReturn(false);
        when(repo.hasControlledPrescription(PES)).thenReturn(true);
        assertThatThrownBy(() -> service().delete(PES))
                .isInstanceOf(Conflict.class).hasMessageContaining("controle especial");
        verify(repo, never()).delete(any(Funcionario.class));
    }

    // R15 — delete allowed when unreferenced
    @Test
    void allowsDeleteWhenUnreferenced() {
        Funcionario f = new Funcionario(); f.setId(PES);
        when(repo.findById(PES)).thenReturn(Optional.of(f));
        when(repo.hasSystemUser(PES)).thenReturn(false);
        when(repo.hasControlledPrescription(PES)).thenReturn(false);
        service().delete(PES);
        verify(repo).delete(f);
        verify(audit).record(eq("DELETE"), eq("SAU_FUN"), eq(PES));
    }

    @Test
    void getThrowsWhenNotFound() {
        when(repo.findById(anyLong())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().get(999L)).isInstanceOf(NotFound.class);
    }

    // R17/LGPD — single-record PHI read is audited
    @Test
    void getAuditsPhiRead() {
        Funcionario f = new Funcionario(); f.setId(PES);
        when(repo.findById(PES)).thenReturn(Optional.of(f));
        when(repo.findPerson(PES)).thenReturn(Optional.of(person("Maria")));
        service().get(PES);
        verify(audit).record(eq("READ"), eq("SAU_FUN"), eq(PES));
    }

    // R5 — update preserves existing situacao when omitted (null)
    @Test
    void updatePreservesSituacaoWhenOmitted() {
        Funcionario f = new Funcionario(); f.setId(PES); f.setSituacao((short) 2);
        when(repo.findById(PES)).thenReturn(Optional.of(f));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repo.findPerson(PES)).thenReturn(Optional.of(person("Maria")));
        service().update(PES, new FuncionarioUpdateRequest("(44) 3232-3232", null, null,
                pessoa("Maria", null, null, null)));
        verify(repo).save(argThat(s -> s.getSituacao() == (short) 2));   // unchanged
    }
}
