package br.gov.mandaguari.saude.medicamento;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.medicamento.domain.Medicamento;
import br.gov.mandaguari.saude.medicamento.dto.MedicamentoDtos.*;
import br.gov.mandaguari.saude.medicamento.mapper.MedicamentoMapperImpl;
import br.gov.mandaguari.saude.medicamento.repository.MedicamentoRepository;
import br.gov.mandaguari.saude.medicamento.repository.RenameAtualFlags;
import br.gov.mandaguari.saude.medicamento.service.MedicamentoService;
import br.gov.mandaguari.saude.medicamento.service.MedicamentoSubService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MedicamentoServiceTest {

    @Mock MedicamentoRepository repo;
    @Mock AuditService audit;
    @Mock MedicamentoSubService subService;

    MedicamentoService service;

    @BeforeEach
    void setup() {
        service = new MedicamentoService(repo, new MedicamentoMapperImpl(), audit, subService);
    }

    // R1
    @Test void createRequiresNome() {
        assertThatThrownBy(() -> service.create(req(null)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Nome do Medicamento");
    }

    // R15 + R16 + R50
    @Test void createGeneratesIdStampsLoginAndDefaultsFlags() {
        given(repo.nextCodigo()).willReturn(18);
        given(repo.save(any())).willAnswer(i -> i.getArgument(0));
        MedicamentoResponse res = service.create(req("DIPIRONA"));
        assertThat(res.id()).isEqualTo(18);                 // R15
        assertThat(res.usuarioLogin()).isEqualTo("sistema");// R16 (no auth context)
        assertThat(res.psicotropico()).isEqualTo((short) 0);// R50
        assertThat(res.controleEspecial()).isEqualTo((short) 0);
        assertThat(res.usarPosologia()).isFalse();
    }

    // R2
    @Test void createRejectsNonExistentTipRemCod() {
        given(repo.tipoMedicamentoExists(5)).willReturn(false);
        assertThatThrownBy(() -> service.create(reqWith(r -> r.tipoMedicamentoCodigo(5))))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Tipos de Medicamento");
    }

    // R37
    @Test void createRejectsInvalidSituacao() {
        assertThatThrownBy(() -> service.create(reqWith(r -> r.situacao((short) 3))))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Situação");
    }

    // R38
    @Test void createRejectsInvalidTipoProduto() {
        assertThatThrownBy(() -> service.create(reqWith(r -> r.tipoProduto((short) 9))))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Tipo de Produto");
    }

    // R42
    @Test void createRequiresPortariaWhenPsicotropico() {
        assertThatThrownBy(() -> service.create(reqWith(r -> r.psicotropico((short) 1))))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Portaria");
    }
    @Test void createAcceptsPsicotropicoWithPortaria() {
        given(repo.nextCodigo()).willReturn(1);
        given(repo.save(any())).willAnswer(i -> i.getArgument(0));
        MedicamentoResponse res = service.create(reqWith(r -> { r.psicotropico((short) 1); r.portariaPsicotropico("344/98"); }));
        assertThat(res.psicotropico()).isEqualTo((short) 1);
    }

    // R10: derive tipoProduto from RenameAtual flags
    @Test void derivesTipoProdutoFromRenameAtualFlags() {
        given(repo.nextCodigo()).willReturn(1);
        given(repo.save(any())).willAnswer(i -> i.getArgument(0));
        given(repo.renameAtualExists("RA1")).willReturn(true);
        given(repo.renameAtualFlags("RA1")).willReturn(Optional.of(flags(true, false, false, false)));
        MedicamentoResponse res = service.create(reqWith(r -> r.renameAtualCodigo("RA1")));
        assertThat(res.tipoProduto()).isEqualTo((short) 1); // só Básico → 1
    }

    // R9: reject incompatible tipoProduto
    @Test void rejectsIncompatibleTipoProduto() {
        given(repo.renameAtualExists("RA1")).willReturn(true);
        given(repo.renameAtualFlags("RA1")).willReturn(Optional.of(flags(true, false, false, false)));
        assertThatThrownBy(() -> service.create(reqWith(r -> { r.renameAtualCodigo("RA1"); r.tipoProduto((short) 2); })))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("compativel");
    }

    // R11: semRename bypasses RENAME validations
    @Test void bypassesRenameValidationWhenSemRename() {
        given(repo.nextCodigo()).willReturn(1);
        given(repo.save(any())).willAnswer(i -> i.getArgument(0));
        // renameAtualExists NOT stubbed → must not be called when semRename=1
        MedicamentoResponse res = service.create(reqWith(r -> { r.renameAtualCodigo("RA1"); r.semRename(true); }));
        assertThat(res.id()).isEqualTo(1);
    }

    // R44
    @Test void requiresMppCancellationMotivoWhenClearingMppFlag() {
        Medicamento existing = base(10); existing.setMedicamentoPotencialmentePerigoso(true);
        given(repo.findById(10)).willReturn(Optional.of(existing));
        assertThatThrownBy(() -> service.update(10, upd(u -> u.medicamentoPotencialmentePerigoso(false))))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("cancelamento do MPP");
    }

    // R53
    @Test void blocksUnsetControleEspecialWithActivePrescriptions() {
        Medicamento existing = base(10); existing.setControleEspecial((short) 1);
        given(repo.findById(10)).willReturn(Optional.of(existing));
        given(repo.isReferencedByRecespItens(10)).willReturn(true);
        assertThatThrownBy(() -> service.update(10, upd(u -> u.controleEspecial((short) 0))))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("controle especial");
    }

    // R3
    @Test void rejectsNonExistentDcbCod() {
        given(repo.dcbExists("XYZ")).willReturn(false);
        assertThatThrownBy(() -> service.create(reqWith(r -> r.dcbCodigo("XYZ"))))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("DCB");
    }

    // R4
    @Test void rejectsNonExistentAprRemCod() {
        given(repo.apresentacaoExists(8)).willReturn(false);
        assertThatThrownBy(() -> service.create(reqWith(r -> r.apresentacaoCodigo(8))))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Apresentação");
    }

    // R6
    @Test void rejectsNonExistentRENAMECod() {
        given(repo.renameExists("RN1")).willReturn(false);
        assertThatThrownBy(() -> service.create(reqWith(r -> r.renameCodigo("RN1"))))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("RENAME");
    }

    // R8
    @Test void rejectsNonExistentObmCod() {
        given(repo.obmExists("OB1")).willReturn(false);
        assertThatThrownBy(() -> service.create(reqWith(r -> r.obmCodigo("OB1"))))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("OBM");
    }

    // R20
    @Test void blocksDeleteWhenReferencedByInteracaoRem1() {
        given(repo.findById(1)).willReturn(Optional.of(base(1)));
        given(repo.isReferencedByInteracaoRem1(1)).willReturn(true);
        assertThatThrownBy(() -> service.delete(1)).isInstanceOf(Conflict.class);
    }

    // R22
    @Test void blocksDeleteWhenReferencedByRemlot() {
        given(repo.findById(1)).willReturn(Optional.of(base(1)));
        given(repo.isReferencedByRemlot(1)).willReturn(true);
        assertThatThrownBy(() -> service.delete(1)).isInstanceOf(Conflict.class);
    }

    // R21
    @Test void blocksDeleteWhenReferencedByRecespItens() {
        given(repo.findById(1)).willReturn(Optional.of(base(1)));
        given(repo.isReferencedByRecespItens(1)).willReturn(true);
        assertThatThrownBy(() -> service.delete(1)).isInstanceOf(Conflict.class);
    }

    // R23-R26 + R18
    @Test void deleteCascadesAndAudits() {
        given(repo.findById(1)).willReturn(Optional.of(base(1)));
        given(repo.isReferencedByInteracaoRem1(1)).willReturn(false);
        given(repo.isReferencedByInteracaoRem2(1)).willReturn(false);
        given(repo.isReferencedByRecespItens(1)).willReturn(false);
        given(repo.isReferencedByRemlot(1)).willReturn(false);
        service.delete(1);
        verify(subService).cascadeDeleteForMedicamento(1);
        verify(audit).record("DELETE", "SAU_REM", 1);
    }

    // ── helpers ──────────────────────────────────────────────────────────────
    private static Medicamento base(Integer id) {
        Medicamento m = new Medicamento(); m.setId(id); m.setNome("MED " + id); return m;
    }

    private static RenameAtualFlags flags(boolean b, boolean e, boolean p, boolean es) {
        return new RenameAtualFlags() {
            public Boolean getBasico() { return b; }
            public Boolean getEstrategico() { return e; }
            public Boolean getProprio() { return p; }
            public Boolean getEspecializado() { return es; }
        };
    }

    /** Minimal valid create request (only nome). */
    private static MedicamentoCreateRequest req(String nome) {
        return new MedicamentoCreateRequest(nome, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, (short) 1, null, null, null, null);
    }

    // mutable builder over the record for terse variation
    private static final class B {
        String nome = "MED"; Integer tipoMedicamentoCodigo; String dcbCodigo, renameCodigo, renameAtualCodigo;
        Integer apresentacaoCodigo; String obmCodigo; Short tipoProduto; String concentracao;
        Short farmaciaBasica, psicotropico, controleEspecial, etico; java.math.BigDecimal valorHospitalar, valorUnitario;
        Boolean semRename; String portariaPsicotropico; Short situacao = 1; Boolean omitirSaldo, usarPosologia, mpp; String mppEfeitos;
        B tipoMedicamentoCodigo(Integer v){tipoMedicamentoCodigo=v;return this;}
        B dcbCodigo(String v){dcbCodigo=v;return this;}
        B renameCodigo(String v){renameCodigo=v;return this;}
        B obmCodigo(String v){obmCodigo=v;return this;}
        B apresentacaoCodigo(Integer v){apresentacaoCodigo=v;return this;}
        B renameAtualCodigo(String v){renameAtualCodigo=v;return this;}
        B tipoProduto(Short v){tipoProduto=v;return this;}
        B psicotropico(Short v){psicotropico=v;return this;}
        B portariaPsicotropico(String v){portariaPsicotropico=v;return this;}
        B situacao(Short v){situacao=v;return this;}
        B semRename(Boolean v){semRename=v;return this;}
        MedicamentoCreateRequest build(){ return new MedicamentoCreateRequest(nome, tipoMedicamentoCodigo, dcbCodigo,
                renameCodigo, renameAtualCodigo, apresentacaoCodigo, obmCodigo, tipoProduto, concentracao,
                farmaciaBasica, psicotropico, controleEspecial, etico, valorHospitalar, valorUnitario, semRename,
                portariaPsicotropico, situacao, omitirSaldo, usarPosologia, mpp, mppEfeitos); }
    }
    private static MedicamentoCreateRequest reqWith(java.util.function.Consumer<B> c) { B b = new B(); c.accept(b); return b.build(); }

    private static final class U {
        String nome = "MED"; Short controleEspecial; Boolean mpp; String mppCancelamentoMotivo;
        U controleEspecial(Short v){controleEspecial=v;return this;}
        U medicamentoPotencialmentePerigoso(Boolean v){mpp=v;return this;}
        MedicamentoUpdateRequest build(){ return new MedicamentoUpdateRequest(nome, null, null, null, null, null, null,
                null, null, null, null, controleEspecial, null, null, null, null, null, (short)1, null, null, mpp,
                null, mppCancelamentoMotivo); }
    }
    private static MedicamentoUpdateRequest upd(java.util.function.Consumer<U> c) { U u = new U(); c.accept(u); return u.build(); }
}
