package br.gov.mandaguari.saude.formaapresentacao;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.formaapresentacao.domain.FormaApresentacao;
import br.gov.mandaguari.saude.formaapresentacao.dto.FormaApresentacaoDtos.*;
import br.gov.mandaguari.saude.formaapresentacao.mapper.FormaApresentacaoMapperImpl;
import br.gov.mandaguari.saude.formaapresentacao.repository.FormaApresentacaoRepository;
import br.gov.mandaguari.saude.formaapresentacao.service.FormaApresentacaoService;
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

/** One test per mined rule (R1-R12 testable at the service layer; R10/R12 also exercised in the IT). */
@ExtendWith(MockitoExtension.class)
class FormaApresentacaoServiceTest {

    @Mock FormaApresentacaoRepository repo;
    @Mock AuditService audit;

    FormaApresentacaoService service;

    @BeforeEach
    void setup() {
        service = new FormaApresentacaoService(repo, new FormaApresentacaoMapperImpl(), audit);
    }

    // R1
    @Test void insertGeneratesNextSequentialCode() {
        given(repo.nextCodigo()).willReturn(5);
        given(repo.existsById(5)).willReturn(false);
        given(repo.save(any())).willAnswer(i -> i.getArgument(0));
        assertThat(service.create(new FormaApresentacaoCreateRequest("Comprimido", "CP")).id()).isEqualTo(5);
    }

    // R2
    @Test void descricaoIsRequired() {
        assertThatThrownBy(() -> service.create(new FormaApresentacaoCreateRequest("", "CP")))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Descrição da Forma");
    }

    // R3
    @Test void abreviacaoIsRequired() {
        assertThatThrownBy(() -> service.create(new FormaApresentacaoCreateRequest("Comprimido", "")))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Abreviação");
    }

    // R4
    @Test void descricaoStoredUpperCase() {
        givenInsertable();
        assertThat(service.create(new FormaApresentacaoCreateRequest("comprimido", "cp")).descricao())
                .isEqualTo("COMPRIMIDO");
    }

    // R5
    @Test void abreviacaoStoredUpperCase() {
        givenInsertable();
        assertThat(service.create(new FormaApresentacaoCreateRequest("comprimido", "cp")).abreviacao())
                .isEqualTo("CP");
    }

    // R6
    @Test void codigoRejectsOutOfRange() {
        given(repo.nextCodigo()).willReturn(1_000_000);
        assertThatThrownBy(() -> service.create(new FormaApresentacaoCreateRequest("Comprimido", "CP")))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("faixa permitida");
    }

    // R7
    @Test void deleteBlockedWhenReferencedByMedicamento() {
        given(repo.findById(1)).willReturn(Optional.of(forma(1)));
        given(repo.isReferencedByMedicamento(1)).willReturn(true);
        assertThatThrownBy(() -> service.delete(1)).isInstanceOf(Conflict.class);
    }

    // R8
    @Test void insertRejectsDuplicatePrimaryKey() {
        given(repo.nextCodigo()).willReturn(5);
        given(repo.existsById(5)).willReturn(true);
        assertThatThrownBy(() -> service.create(new FormaApresentacaoCreateRequest("Comprimido", "CP")))
                .isInstanceOf(Conflict.class);
    }

    // R9 — not-found on update/delete (optimistic-lock half deferred: no @Version column)
    @Test void updateRejectsWhenRecordChangedOrDeleted() {
        given(repo.findById(99)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(99, new FormaApresentacaoUpdateRequest("X", "X")))
                .isInstanceOf(NotFound.class);
        assertThatThrownBy(() -> service.get(99)).isInstanceOf(NotFound.class);
    }

    // R11 — audit on every write (CREATE + UPDATE + DELETE)
    @Test void successfulWriteEmitsAuditLog() {
        givenInsertable();
        service.create(new FormaApresentacaoCreateRequest("Comprimido", "CP"));
        verify(audit).record("CREATE", "SAU_APRREM", 5);

        given(repo.findById(1)).willReturn(Optional.of(forma(1)));
        given(repo.save(any())).willAnswer(i -> i.getArgument(0));
        service.update(1, new FormaApresentacaoUpdateRequest("Xarope", "XRP"));
        verify(audit).record("UPDATE", "SAU_APRREM", 1);

        given(repo.isReferencedByMedicamento(1)).willReturn(false);
        service.delete(1);
        verify(audit).record("DELETE", "SAU_APRREM", 1);
    }

    private void givenInsertable() {
        given(repo.nextCodigo()).willReturn(5);
        given(repo.existsById(5)).willReturn(false);
        given(repo.save(any())).willAnswer(i -> i.getArgument(0));
    }

    private static FormaApresentacao forma(Integer id) {
        FormaApresentacao f = new FormaApresentacao();
        f.setId(id); f.setDescricao("COMPRIMIDO"); f.setAbreviacao("CP");
        return f;
    }
}
