package br.gov.mandaguari.saude.receituarioespecial;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.receituarioespecial.domain.ReceituarioEspecial;
import br.gov.mandaguari.saude.receituarioespecial.domain.ReceituarioEspecialId;
import br.gov.mandaguari.saude.receituarioespecial.domain.ReceituarioEspecialItem;
import br.gov.mandaguari.saude.receituarioespecial.dto.ReceituarioEspecialDtos.*;
import br.gov.mandaguari.saude.receituarioespecial.repository.PatientInfoProjection;
import br.gov.mandaguari.saude.receituarioespecial.repository.ReceituarioEspecialItemRepository;
import br.gov.mandaguari.saude.receituarioespecial.repository.ReceituarioEspecialRepository;
import br.gov.mandaguari.saude.receituarioespecial.service.ReceituarioEspecialService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the mined SAU_RECESP rules (see SLICE-SPEC SAU_RECESP). Repositories + audit are mocked.
 * Rule refs match the SLICE-SPEC citations (sau_recesp_impl.java / psau_inc_recesp.java).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReceituarioEspecialServiceTest {

    static final int UNI = 1;
    static final long PAC = 500L;
    static final long PRO = 100L;

    @Mock ReceituarioEspecialRepository repo;
    @Mock ReceituarioEspecialItemRepository itemRepo;
    @Mock AuditService audit;

    ReceituarioEspecialService service() { return new ReceituarioEspecialService(repo, itemRepo, audit); }

    // ---------- request builders ----------

    private ItemRequest validItem() {
        return new ItemRequest(0, "Rivotril 2mg", null, null, 0, null, 3, 1, null, null);
    }

    private ReceituarioEspecialWriteRequest validCreate() {
        return new ReceituarioEspecialWriteRequest(UNI, LocalDate.of(2026, 6, 1), PAC, PRO, null,
                null, null, null, List.of(validItem()));
    }

    /** Stubs a fully valid create path: existence checks true, patient active w/ CNS, saves echo the arg. */
    private void stubValidCreate() {
        when(repo.unidadeExists(UNI)).thenReturn(true);
        when(repo.pacienteExists(PAC)).thenReturn(true);
        when(repo.prescritorExists(PRO)).thenReturn(true);
        when(repo.findPatientInfo(PAC)).thenReturn(Optional.of(
                info(1, "700000000000005", "Maria Silva", null, false, LocalDate.of(1990, 1, 1))));
        when(repo.findMaxCodigoForUnit(UNI)).thenReturn(Optional.of(4L));
        when(repo.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(itemRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- R1 / R2 numbering ----------

    @Test // R1 — RecEspCod = MAX(per-unit) + 1
    void allocatesSequentialNumberPerUnit() {
        stubValidCreate();
        service().create(validCreate());
        verify(repo).saveAndFlush(argThat(r -> r.getCodigo() != null && r.getCodigo() == 5L
                && r.getUnidadeId() == UNI));
    }

    @Test // R1 — first prescription of a unit gets number 1
    void allocatesOneWhenUnitEmpty() {
        stubValidCreate();
        when(repo.findMaxCodigoForUnit(UNI)).thenReturn(Optional.empty());
        var resp = service().create(validCreate());
        assertThat(resp.numero()).isEqualTo(1L);
    }

    // ---------- master required-field validations ----------

    @Test // R8 — unit is part of the PK: required
    void rejectsMissingUnit() {
        var req = new ReceituarioEspecialWriteRequest(0, LocalDate.now(), PAC, PRO, null, null, null, null,
                List.of(validItem()));
        assertThatThrownBy(() -> service().create(req))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Unidade");
    }

    @Test // R8 — unit must exist
    void rejectsUnknownUnit() {
        when(repo.unidadeExists(UNI)).thenReturn(false);
        assertThatThrownBy(() -> service().create(validCreate()))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Não existe Unidade");
    }

    @Test // R4 — date required
    void rejectsMissingDate() {
        when(repo.unidadeExists(UNI)).thenReturn(true);
        var req = new ReceituarioEspecialWriteRequest(UNI, null, PAC, PRO, null, null, null, null,
                List.of(validItem()));
        assertThatThrownBy(() -> service().create(req))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Data do Receituário");
    }

    @Test // R5 — patient required
    void rejectsMissingPatient() {
        when(repo.unidadeExists(UNI)).thenReturn(true);
        var req = new ReceituarioEspecialWriteRequest(UNI, LocalDate.now(), 0L, PRO, null, null, null, null,
                List.of(validItem()));
        assertThatThrownBy(() -> service().create(req))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Código do Paciente");
    }

    @Test // R10 — patient must exist
    void rejectsUnknownPatient() {
        when(repo.unidadeExists(UNI)).thenReturn(true);
        when(repo.pacienteExists(PAC)).thenReturn(false);
        assertThatThrownBy(() -> service().create(validCreate()))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Não existe Paciente");
    }

    @Test // R6 — prescriber required
    void rejectsMissingPrescriber() {
        when(repo.unidadeExists(UNI)).thenReturn(true);
        when(repo.pacienteExists(PAC)).thenReturn(true);
        when(repo.findPatientInfo(PAC)).thenReturn(Optional.of(info(1, "700000000000005", "Maria", null, false, null)));
        var req = new ReceituarioEspecialWriteRequest(UNI, LocalDate.now(), PAC, 0L, null, null, null, null,
                List.of(validItem()));
        assertThatThrownBy(() -> service().create(req))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Código do Profissional");
    }

    @Test // R11 — prescriber must exist
    void rejectsUnknownPrescriber() {
        when(repo.unidadeExists(UNI)).thenReturn(true);
        when(repo.pacienteExists(PAC)).thenReturn(true);
        when(repo.findPatientInfo(PAC)).thenReturn(Optional.of(info(1, "700000000000005", "Maria", null, false, null)));
        when(repo.prescritorExists(PRO)).thenReturn(false);
        assertThatThrownBy(() -> service().create(validCreate()))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Não existe Profissional");
    }

    @Test // R9 — funcionário 0 is allowed; a non-existent non-zero is rejected
    void rejectsUnknownEmployeeButAllowsZero() {
        stubValidCreate();
        when(repo.funcionarioExists(77L)).thenReturn(false);
        var req = new ReceituarioEspecialWriteRequest(UNI, LocalDate.now(), PAC, PRO, 77L, null, null, null,
                List.of(validItem()));
        assertThatThrownBy(() -> service().create(req))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Não existe Funcionários");
        // 0 is fine:
        service().create(validCreate());   // funcionario null → skipped, no throw
    }

    @Test // R7 — observation ≤ 300 chars
    void rejectsObservationOver300Chars() {
        stubValidCreate();
        var req = new ReceituarioEspecialWriteRequest(UNI, LocalDate.now(), PAC, PRO, null, null, null,
                "x".repeat(301), List.of(validItem()));
        assertThatThrownBy(() -> service().create(req))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("300");
    }

    // ---------- patient eligibility ----------

    @Test // R12 — inactive patient (PacSit=2) blocked
    void rejectsInactivePatient() {
        when(repo.unidadeExists(UNI)).thenReturn(true);
        when(repo.pacienteExists(PAC)).thenReturn(true);
        when(repo.findPatientInfo(PAC)).thenReturn(Optional.of(info(2, "700000000000005", "Maria", null, false, null)));
        assertThatThrownBy(() -> service().create(validCreate()))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Paciente Inativo");
    }

    @Test // R13 — missing CNS is a non-blocking WARNING (create still succeeds)
    void warnsButAllowsPatientWithoutCns() {
        stubValidCreate();
        when(repo.findPatientInfo(PAC)).thenReturn(Optional.of(info(1, null, "Maria", null, false, null)));
        var resp = service().create(validCreate());
        assertThat(resp.avisos()).anyMatch(a -> a.contains("CNS"));
    }

    // ---------- derivations / audit ----------

    @Test // R15 — patient age = age(birth, prescription date)
    void derivesPatientAge() {
        stubValidCreate();  // birth 1990-01-01, prescription 2026-06-01 → 36
        var resp = service().create(validCreate());
        assertThat(resp.pacienteIdade()).isEqualTo(36);
    }

    @Test // R16 — social-name display "NomeSocial (NomeCivil)"
    void derivesSocialNameDisplay() {
        stubValidCreate();
        when(repo.findPatientInfo(PAC)).thenReturn(Optional.of(
                info(1, "700000000000005", "Jose Silva", "Maria Silva", true, LocalDate.of(1990, 1, 1))));
        var resp = service().create(validCreate());
        assertThat(resp.pacienteNomeExibicao()).isEqualTo("Maria Silva (Jose Silva)");
    }

    @Test // R17 — RecEspUsuLogin stamped server-side (no auth in unit test → "system")
    void stampsUserLogin() {
        stubValidCreate();
        service().create(validCreate());
        verify(repo).saveAndFlush(argThat(r -> "system".equals(r.getUsuarioLogin())));
    }

    @Test // R28 — audit on create
    void writesAuditRecordOnCreate() {
        stubValidCreate();
        service().create(validCreate());
        verify(audit).record(eq("CREATE"), eq("SAU_RECESP"), eq("1/5"));
    }

    // ---------- child line-item rules ----------

    @Test // R18 — unknown medication rejected; free-text (RemCod 0) allowed
    void rejectsUnknownMedicationButAllowsFreeText() {
        stubValidCreate();
        when(itemRepo.medicamentoExists(999)).thenReturn(false);
        var badItem = new ItemRequest(999, "X", null, null, 0, null, 3, 1, null, null);
        var req = withItems(badItem);
        assertThatThrownBy(() -> service().create(req))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Não existe Medicamento");
        service().create(validCreate());  // RemCod 0 free-text → ok
    }

    @Test // R19 — prescription text defaults to the drug name
    void defaultsPrescriptionTextFromMedicationName() {
        stubValidCreate();
        when(itemRepo.medicamentoExists(42)).thenReturn(true);
        when(itemRepo.medicamentoNome(42)).thenReturn(Optional.of("CLONAZEPAM 2MG"));
        var item = new ItemRequest(42, "ignored by default", null, null, 0, null, 3, 1, null, null);
        service().create(withItems(item));
        verify(itemRepo).save(argThat(i -> "CLONAZEPAM 2MG".equals(i.getPrescricao())));
    }

    @Test // R20 — prescription text required per line
    void rejectsMissingPrescriptionText() {
        stubValidCreate();
        var item = new ItemRequest(0, "  ", null, null, 0, null, 3, 1, null, null);
        assertThatThrownBy(() -> service().create(withItems(item)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Informe a Prescrição");
    }

    @Test // R21 — unknown posology rejected
    void rejectsUnknownPosology() {
        stubValidCreate();
        when(itemRepo.posologiaExists(88)).thenReturn(false);
        var item = new ItemRequest(0, "Rivotril", null, null, 88, null, 3, 1, null, null);
        assertThatThrownBy(() -> service().create(withItems(item)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Não existe Posologia");
    }

    @Test // R22 — observation defaults to the posology description
    void defaultsObservationFromPosology() {
        stubValidCreate();
        when(itemRepo.posologiaExists(7)).thenReturn(true);
        when(itemRepo.posologiaDescricao(7)).thenReturn(Optional.of("1 comprimido ao dia"));
        var item = new ItemRequest(0, "Rivotril", null, null, 7, null, 3, 1, null, null);
        service().create(withItems(item));
        verify(itemRepo).save(argThat(i -> "1 comprimido ao dia".equals(i.getObservacao())));
    }

    @Test // R23 — receituário type required per line
    void rejectsMissingReceituarioType() {
        stubValidCreate();
        var item = new ItemRequest(0, "Rivotril", null, null, 0, null, 0, 1, null, null);
        assertThatThrownBy(() -> service().create(withItems(item)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Tipo do receituário");
    }

    @Test // R24 — quantity/type are optional (no validation)
    void allowsMissingQuantityAndType() {
        stubValidCreate();
        var item = new ItemRequest(0, "Rivotril", null, null, 0, null, 3, null, null, null);
        service().create(withItems(item));  // no throw
        verify(itemRepo).save(argThat(i -> i.getQuantidade() == null));
    }

    @Test // R25 — RecInd defaults to false
    void defaultsRecIndFalseOnInsert() {
        stubValidCreate();
        service().create(validCreate());
        verify(itemRepo).save(argThat(i -> Boolean.FALSE.equals(i.getIndeferido())));
    }

    @Test // R26 — line sequence + master last-seq counter
    void maintainsLastLineSequenceCounter() {
        stubValidCreate();
        var req = new ReceituarioEspecialWriteRequest(UNI, LocalDate.now(), PAC, PRO, null, null, null, null,
                List.of(validItem(), new ItemRequest(0, "Gardenal", new BigDecimal("2.0"), 5, 0, null, 3, 1, null, null)));
        service().create(req);
        verify(itemRepo).save(argThat(i -> i.getSequencia() == 1));
        verify(itemRepo).save(argThat(i -> i.getSequencia() == 2));
        verify(repo).save(argThat(r -> r.getSequenciaUltima() == 2));   // monotonic counter
    }

    // ---------- delete (regulatory) / copy ----------

    @Test // R29 — delete is BLOCKED (retention, Portaria 344/98), not the legacy hard-delete
    void blocksDeleteForRetention() {
        when(repo.findById(new ReceituarioEspecialId(UNI, 5L)))
                .thenReturn(Optional.of(master(UNI, 5L)));
        assertThatThrownBy(() -> service().delete(UNI, 5L))
                .isInstanceOf(Conflict.class).hasMessageContaining("344/98");
        verify(repo, never()).delete(any());
        verify(itemRepo, never()).deleteByMaster(any(), any());
    }

    @Test // delete of a non-existent prescription → 404
    void deleteThrowsWhenNotFound() {
        when(repo.findById(new ReceituarioEspecialId(UNI, 9L))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().delete(UNI, 9L)).isInstanceOf(NotFound.class);
    }

    @Test // R31 — copy clones with a new server-allocated number, preserving lines
    void copyClonesWithNewNumber() {
        when(repo.findById(new ReceituarioEspecialId(UNI, 5L))).thenReturn(Optional.of(master(UNI, 5L)));
        when(itemRepo.findByUnidadeIdAndCodigoOrderBySequencia(UNI, 5L))
                .thenReturn(List.of(line(UNI, 5L, 1)));
        when(repo.findMaxCodigoForUnit(UNI)).thenReturn(Optional.of(5L));
        when(repo.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(itemRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repo.findPatientInfo(any())).thenReturn(Optional.empty());

        var resp = service().copy(UNI, 5L);
        assertThat(resp.numero()).isEqualTo(6L);
        verify(itemRepo).save(argThat(i -> i.getCodigo() == 6L && i.getSequencia() == 1));
    }

    // ---------- helpers ----------

    private ReceituarioEspecialWriteRequest withItems(ItemRequest... items) {
        return new ReceituarioEspecialWriteRequest(UNI, LocalDate.of(2026, 6, 1), PAC, PRO, null, null, null, null,
                List.of(items));
    }

    private static ReceituarioEspecial master(int uni, long cod) {
        ReceituarioEspecial r = new ReceituarioEspecial();
        r.setUnidadeId(uni);
        r.setCodigo(cod);
        r.setPacienteCodigo(PAC);
        r.setPrescritorCodigo(PRO);
        r.setData(LocalDate.of(2026, 6, 1));
        r.setSequenciaUltima(1);
        return r;
    }

    private static ReceituarioEspecialItem line(int uni, long cod, int seq) {
        ReceituarioEspecialItem it = new ReceituarioEspecialItem();
        it.setUnidadeId(uni);
        it.setCodigo(cod);
        it.setSequencia(seq);
        it.setPrescricao("Rivotril 2mg");
        it.setTipoReceita(3);
        it.setIndeferido(false);
        return it;
    }

    /** Anonymous {@link PatientInfoProjection} test double. */
    private static PatientInfoProjection info(Integer sit, String cns, String nome, String nomeSoc,
                                              Boolean usaSoc, LocalDate nasc) {
        return new PatientInfoProjection() {
            public Integer getSituacao() { return sit; }
            public String getCns() { return cns; }
            public String getNome() { return nome; }
            public String getNomeSocial() { return nomeSoc; }
            public Boolean getUsaNomeSocial() { return usaSoc; }
            public LocalDate getDataNascimento() { return nasc; }
        };
    }
}
