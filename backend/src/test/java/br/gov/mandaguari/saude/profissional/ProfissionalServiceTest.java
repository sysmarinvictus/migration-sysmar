package br.gov.mandaguari.saude.profissional;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.conselhoclasse.repository.ConselhoClasseRepository;
import br.gov.mandaguari.saude.profissional.domain.Profissional;
import br.gov.mandaguari.saude.profissional.dto.ProfissionalDtos.*;
import br.gov.mandaguari.saude.profissional.mapper.ProfissionalMapper;
import br.gov.mandaguari.saude.profissional.repository.PersonProjection;
import br.gov.mandaguari.saude.profissional.repository.ProfissionalRepository;
import br.gov.mandaguari.saude.profissional.service.ProfissionalService;
import br.gov.mandaguari.saude.profissional.service.SoundexService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for mined SAU_PRO (Profissional) rules. Repository + audit + conselho repo + soundex
 * (where the value is not under test) are mocked; the real MapStruct mapper is used. The real
 * {@link SoundexService} is also wired so R15 asserts an actual phonetic key. Rule refs match the
 * SLICE-SPEC SAU_PRO citations. Synthetic, non-PHI identifiers only.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfissionalServiceTest {

    /** Valid synthetic CNS (provisional range, mod-11 ≡ 0) — generated, not real. */
    static final String VALID_CNS = "700000000000021";
    /** Valid synthetic CPF (mod-11) — generated, not real. */
    static final String VALID_CPF = "01111111294";
    static final long PES = 100L;

    @Mock ProfissionalRepository repo;
    @Mock ConselhoClasseRepository conselhoRepo;
    @Mock AuditService audit;
    final ProfissionalMapper mapper = Mappers.getMapper(ProfissionalMapper.class);
    final SoundexService soundex = new SoundexService();

    ProfissionalService service() {
        return new ProfissionalService(repo, conselhoRepo, mapper, soundex, audit);
    }

    /** A plain PersonProjection stub (NOT a Mockito mock — avoids nested stubbing inside thenReturn). */
    private PersonProjection person(String nome) {
        return new PersonProjection() {
            @Override public String getNome() { return nome; }
            @Override public String getCpfCnpj() { return null; }
            @Override public String getRgIe() { return null; }
            @Override public String getSexo() { return null; }
            @Override public java.time.LocalDate getDataNascimento() { return null; }
            @Override public String getEndereco() { return null; }
            @Override public String getEnderecoNumero() { return null; }
            @Override public String getEnderecoComplemento() { return null; }
            @Override public String getCep() { return null; }
            @Override public Integer getBairroCod() { return null; }
            @Override public Integer getMunicipioCod() { return null; }
            @Override public String getTelefone() { return null; }
            @Override public String getCelular() { return null; }
        };
    }

    /** Wires the happy-path collaborators so a create() reaches save(). */
    private void stubHappyCreate(String nome) {
        when(repo.personExists(PES)).thenReturn(true);
        when(repo.existsById(PES)).thenReturn(false);
        when(repo.findCnsOwner(anyString(), eq(PES))).thenReturn(Optional.empty());
        when(repo.findCpfCnpjOwner(anyString(), eq(PES))).thenReturn(Optional.empty());
        when(repo.findPerson(PES)).thenReturn(Optional.of(person(nome)));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private PessoaSubRequest pessoa(String nome, String cpf, String fone, String cel) {
        return new PessoaSubRequest(nome, cpf, fone, cel);
    }

    private ProfissionalCreateRequest create(String cns, Short concla, PessoaSubRequest pessoa) {
        return new ProfissionalCreateRequest(PES, cns, null, "PR", concla, null, null, null, null, null, null, pessoa);
    }

    private ProfissionalCreateRequest validCreate() {
        return create(VALID_CNS, (short) 0, pessoa("Maria Sintetica", null, null, null));
    }

    // R1 — referenced SYS_PES person must already exist
    @Test
    void rejectsCreateWhenPersonDoesNotExist() {
        when(repo.personExists(PES)).thenReturn(false);
        assertThatThrownBy(() -> service().create(validCreate()))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("Não existe Profissional");
    }

    // R2 — confirm writes back person name/cpf/phones to SYS_PES
    @Test
    void updatesUnderlyingPessoaNameCpfPhones() {
        stubHappyCreate("Maria Sintetica");
        var req = create(VALID_CNS, (short) 0,
                pessoa("Maria Atualizada", VALID_CPF, "(44) 3232-3232", "(44) 99999-8888"));
        service().create(req);
        verify(repo).updatePerson(PES, "Maria Atualizada", VALID_CPF, "(44) 3232-3232", "(44) 99999-8888");
    }

    // R3 — CNS required
    @Test
    void requiresCns() {
        when(repo.personExists(PES)).thenReturn(true);
        when(repo.existsById(PES)).thenReturn(false);
        assertThatThrownBy(() -> service().create(create("   ", (short) 0, pessoa("X", null, null, null))))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("Informe o Número do CNS!");
    }

    // R4 — CNS check-digit/format
    @Test
    void rejectsInvalidCns() {
        when(repo.personExists(PES)).thenReturn(true);
        when(repo.existsById(PES)).thenReturn(false);
        assertThatThrownBy(() -> service().create(create("700000000000001", (short) 0, pessoa("X", null, null, null))))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("CNS inválido");
    }

    // R5 — CNS uniqueness across people (excludes current id)
    @Test
    void rejectsDuplicateCnsAcrossPeople() {
        when(repo.personExists(PES)).thenReturn(true);
        when(repo.existsById(PES)).thenReturn(false);
        when(repo.findCnsOwner(anyString(), eq(PES))).thenReturn(Optional.of(777L));
        assertThatThrownBy(() -> service().create(validCreate()))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("777");
    }

    // R6 — CPF validated only when present
    @Test
    void rejectsInvalidCpfWhenPresent() {
        when(repo.personExists(PES)).thenReturn(true);
        when(repo.existsById(PES)).thenReturn(false);
        when(repo.findCnsOwner(anyString(), eq(PES))).thenReturn(Optional.empty());
        var req = create(VALID_CNS, (short) 0, pessoa("X", "12345678900", null, null));
        assertThatThrownBy(() -> service().create(req))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("CPF inválido");
    }

    // R7 — CPF/CNPJ uniqueness across people
    @Test
    void rejectsDuplicateCpfAcrossPeople() {
        when(repo.personExists(PES)).thenReturn(true);
        when(repo.existsById(PES)).thenReturn(false);
        when(repo.findCnsOwner(anyString(), eq(PES))).thenReturn(Optional.empty());
        when(repo.findCpfCnpjOwner(eq(VALID_CPF), eq(PES))).thenReturn(Optional.of(888L));
        var req = create(VALID_CNS, (short) 0, pessoa("X", VALID_CPF, null, null));
        assertThatThrownBy(() -> service().create(req))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("888");
    }

    // R8 — phone format when present
    @Test
    void rejectsInvalidPhone() {
        when(repo.personExists(PES)).thenReturn(true);
        when(repo.existsById(PES)).thenReturn(false);
        when(repo.findCnsOwner(anyString(), eq(PES))).thenReturn(Optional.empty());
        var req = create(VALID_CNS, (short) 0, pessoa("X", null, "4432323232", null));
        assertThatThrownBy(() -> service().create(req))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("Telefone inválido");
    }

    // R9 — mobile format when present
    @Test
    void rejectsInvalidMobile() {
        when(repo.personExists(PES)).thenReturn(true);
        when(repo.existsById(PES)).thenReturn(false);
        when(repo.findCnsOwner(anyString(), eq(PES))).thenReturn(Optional.empty());
        var req = create(VALID_CNS, (short) 0, pessoa("X", null, null, "99999-8888"));
        assertThatThrownBy(() -> service().create(req))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("Celular inválido");
    }

    // R10 — ConClaCod must exist when != 0; 0 allowed
    @Test
    void rejectsUnknownConselhoDeClasse() {
        when(repo.personExists(PES)).thenReturn(true);
        when(repo.existsById(PES)).thenReturn(false);
        when(repo.findCnsOwner(anyString(), eq(PES))).thenReturn(Optional.empty());
        when(conselhoRepo.existsById((short) 9)).thenReturn(false);
        var req = create(VALID_CNS, (short) 9, pessoa("X", null, null, null));
        assertThatThrownBy(() -> service().create(req))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("Não existe Conselho de Classe");
    }

    // R12 — situacao defaults to 1 (ATIVO) on INS
    @Test
    void defaultsSituacaoToAtivoOnInsert() {
        stubHappyCreate("Maria");
        service().create(validCreate());
        verify(repo).save(argThat(p -> p.getSituacao() == (short) 1));
    }

    // R13 — externo defaults 0, exportaEsus defaults false on INS
    @Test
    void defaultsProExtZeroAndExpEsusFalse() {
        stubHappyCreate("Maria");
        service().create(validCreate());
        verify(repo).save(argThat(p ->
                p.getExterno() == (short) 0 && Boolean.FALSE.equals(p.getExportaEsus())));
    }

    // R15 — soundex recomputed from the SYS_PES name (real SoundexService output)
    @Test
    void computesSoundexFromName() {
        stubHappyCreate("PHILIPE");
        service().create(validCreate());
        // SoundexService("PHILIPE") = "FIRIPE" (PH→F, then L→R)
        verify(repo).save(argThat(p -> "FIRIPE".equals(p.getNomeSoundex())));
    }

    // R17/R18 — audit on create records the REAL professional id (NOT the legacy hardcoded 0)
    @Test
    void writesAuditRowWithRealProfessionalIdOnCreate() {
        Profissional saved = new Profissional();
        saved.setId(PES);
        when(repo.personExists(PES)).thenReturn(true);
        when(repo.existsById(PES)).thenReturn(false);
        when(repo.findCnsOwner(anyString(), eq(PES))).thenReturn(Optional.empty());
        when(repo.findPerson(PES)).thenReturn(Optional.of(person("Maria")));
        when(repo.save(any())).thenReturn(saved);

        service().create(validCreate());

        verify(audit).record(eq("CREATE"), eq("SAU_PRO"), eq(PES));
        verify(audit, never()).record(eq("CREATE"), eq("SAU_PRO"), eq(0L));
    }

    // R17/R18 — audit on update records the REAL professional id
    @Test
    void writesAuditRowWithRealProfessionalIdOnUpdate() {
        Profissional existing = new Profissional();
        existing.setId(PES);
        when(repo.findById(PES)).thenReturn(Optional.of(existing));
        when(repo.findCnsOwner(anyString(), eq(PES))).thenReturn(Optional.empty());
        when(repo.findPerson(PES)).thenReturn(Optional.of(person("Maria")));
        when(repo.save(any())).thenReturn(existing);

        var upd = new ProfissionalUpdateRequest(VALID_CNS, null, "PR", (short) 0, null, null, null, null, null, null,
                pessoa("Maria", null, null, null));
        service().update(PES, upd);

        verify(audit).record(eq("UPDATE"), eq("SAU_PRO"), eq(PES));
    }

    // R19/R20 — delete blocked when professional has a specialty (SAU_PROESP)
    @Test
    void blocksDeleteWithSpecialty() {
        Profissional p = new Profissional(); p.setId(PES);
        when(repo.findById(PES)).thenReturn(Optional.of(p));
        when(repo.hasSpecialty(PES)).thenReturn(true);
        assertThatThrownBy(() -> service().delete(PES))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("especialidade");
        verify(repo, never()).delete(any(Profissional.class));
    }

    // R21 — delete blocked when linked to a system user (SAU_USU)
    @Test
    void blocksDeleteWithSystemUser() {
        Profissional p = new Profissional(); p.setId(PES);
        when(repo.findById(PES)).thenReturn(Optional.of(p));
        when(repo.hasSystemUser(PES)).thenReturn(true);
        assertThatThrownBy(() -> service().delete(PES))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("usuário do sistema");
        verify(repo, never()).delete(any(Profissional.class));
    }

    // R22 — delete blocked when linked to Uni Nut Pro Pes
    @Test
    void blocksDeleteWithUniNut() {
        Profissional p = new Profissional(); p.setId(PES);
        when(repo.findById(PES)).thenReturn(Optional.of(p));
        when(repo.hasUniNut(PES)).thenReturn(true);
        assertThatThrownBy(() -> service().delete(PES))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("Uni Nut Pro Pes");
        verify(repo, never()).delete(any(Profissional.class));
    }

    // R23 — delete blocked when has SISPRENATAL records
    @Test
    void blocksDeleteWithSisprenatal() {
        Profissional p = new Profissional(); p.setId(PES);
        when(repo.findById(PES)).thenReturn(Optional.of(p));
        when(repo.hasSisprenatal(PES)).thenReturn(true);
        assertThatThrownBy(() -> service().delete(PES))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("SISPRENATAL");
        verify(repo, never()).delete(any(Profissional.class));
    }

    // R24 — delete blocked when has HIPERDIA records
    @Test
    void blocksDeleteWithHiperdia() {
        Profissional p = new Profissional(); p.setId(PES);
        when(repo.findById(PES)).thenReturn(Optional.of(p));
        when(repo.hasHiperdia(PES)).thenReturn(true);
        assertThatThrownBy(() -> service().delete(PES))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("HIPERDIA");
        verify(repo, never()).delete(any(Profissional.class));
    }

    // R25 — delete blocked when assigned a SAU_UNI role
    @Test
    void blocksDeleteWhenUnidadeRoleAssigned() {
        Profissional p = new Profissional(); p.setId(PES);
        when(repo.findById(PES)).thenReturn(Optional.of(p));
        when(repo.hasUnidadeRole(PES)).thenReturn(true);
        assertThatThrownBy(() -> service().delete(PES))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("Unidade");
        verify(repo, never()).delete(any(Profissional.class));
    }

    // R26 — delete blocked when has a controlled-substance prescription (SAU_RECESP, Portaria 344/98)
    @Test
    void blocksDeleteWhenHasControlledPrescription() {
        Profissional p = new Profissional(); p.setId(PES);
        when(repo.findById(PES)).thenReturn(Optional.of(p));
        when(repo.hasControlledPrescription(PES)).thenReturn(true);
        assertThatThrownBy(() -> service().delete(PES))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("Portaria 344");
        verify(repo, never()).delete(any(Profissional.class));
    }

    // R19 — delete allowed when no guard reports a reference
    @Test
    void blocksDeleteWhenReferencedAllowsWhenNone() {
        Profissional p = new Profissional(); p.setId(PES);
        when(repo.findById(PES)).thenReturn(Optional.of(p));
        when(repo.hasSpecialty(PES)).thenReturn(false);
        when(repo.hasSystemUser(PES)).thenReturn(false);
        when(repo.hasUniNut(PES)).thenReturn(false);
        when(repo.hasSisprenatal(PES)).thenReturn(false);
        when(repo.hasHiperdia(PES)).thenReturn(false);
        when(repo.hasUnidadeRole(PES)).thenReturn(false);
        when(repo.hasControlledPrescription(PES)).thenReturn(false);

        service().delete(PES);

        verify(repo).delete(p);
        verify(audit).record(eq("DELETE"), eq("SAU_PRO"), eq(PES));
    }

    // get — not found
    @Test
    void getThrowsWhenNotFound() {
        when(repo.findById(anyLong())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().get(999L)).isInstanceOf(NotFound.class);
    }
}
