package br.gov.mandaguari.saude.posologia;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.posologia.domain.Posologia;
import br.gov.mandaguari.saude.posologia.dto.PosologiaDtos.*;
import br.gov.mandaguari.saude.posologia.mapper.PosologiaMapperImpl;
import br.gov.mandaguari.saude.posologia.repository.PosologiaRepository;
import br.gov.mandaguari.saude.posologia.service.PosologiaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PosologiaServiceTest {

    @Mock PosologiaRepository repo;
    @Mock AuditService audit;

    PosologiaService service;

    @BeforeEach
    void setup() {
        service = new PosologiaService(repo, new PosologiaMapperImpl(), audit);
    }

    // R1: codigo is system-assigned (MAX+1)
    @Test
    void autoAssignsCodigoOnInsert() {
        given(repo.findMaxCodigo()).willReturn(5);
        given(repo.save(any())).willAnswer(inv -> inv.getArgument(0));

        PosologiaResponse res = service.create(req("Tomar 1 comprimido de 8/8h"));

        assertThat(res.codigo()).isEqualTo(6);
    }

    @Test
    void startsAtOneWhenTableEmpty() {
        given(repo.findMaxCodigo()).willReturn(0);
        given(repo.save(any())).willAnswer(inv -> inv.getArgument(0));

        PosologiaResponse res = service.create(req("Dose inicial"));

        assertThat(res.codigo()).isEqualTo(1);
    }

    // R2: descricao required
    @Test
    void rejectsBlankDescricao() {
        assertThatThrownBy(() -> service.create(req("")))
                .isInstanceOf(BusinessRule.class);
    }

    // R3: delete blocked when referenced by SAU_REMPOSO
    @Test
    void rejectsDeleteWhenReferencedByRemposo() {
        Posologia p = posologia(1, "Dose");
        given(repo.findById(1)).willReturn(Optional.of(p));
        given(repo.isReferencedByRemposo(1)).willReturn(true);

        assertThatThrownBy(() -> service.delete(1))
                .isInstanceOf(Conflict.class);
    }

    // R4: delete blocked when referenced by SAU_RECESP1
    @Test
    void rejectsDeleteWhenReferencedByRecesp1() {
        Posologia p = posologia(1, "Dose");
        given(repo.findById(1)).willReturn(Optional.of(p));
        given(repo.isReferencedByRemposo(1)).willReturn(false);
        given(repo.isReferencedByRecesp1(1)).willReturn(true);

        assertThatThrownBy(() -> service.delete(1))
                .isInstanceOf(Conflict.class);
    }

    // R6: audit on create
    @Test
    void writesAuditOnCreate() {
        given(repo.findMaxCodigo()).willReturn(0);
        given(repo.save(any())).willAnswer(inv -> inv.getArgument(0));

        service.create(req("Dose"));

        verify(audit).record(eq("CREATE"), eq("SAU_REMOBS"), any());
    }

    @Test
    void writesAuditOnUpdate() {
        Posologia p = posologia(3, "Antiga");
        given(repo.findById(3)).willReturn(Optional.of(p));
        given(repo.save(any())).willAnswer(inv -> inv.getArgument(0));

        service.update(3, new PosologiaUpdateRequest("Nova", null, null, null, null, null));

        verify(audit).record(eq("UPDATE"), eq("SAU_REMOBS"), eq(3));
    }

    @Test
    void writesAuditOnDelete() {
        Posologia p = posologia(2, "Para excluir");
        given(repo.findById(2)).willReturn(Optional.of(p));
        given(repo.isReferencedByRemposo(2)).willReturn(false);
        given(repo.isReferencedByRecesp1(2)).willReturn(false);

        service.delete(2);

        verify(audit).record(eq("DELETE"), eq("SAU_REMOBS"), eq(2));
    }

    // --- helpers ---

    private static PosologiaCreateRequest req(String descricao) {
        return new PosologiaCreateRequest(descricao, null, null, null, null, null);
    }

    private static Posologia posologia(int codigo, String descricao) {
        Posologia p = new Posologia();
        p.setCodigo(codigo);
        p.setDescricao(descricao);
        return p;
    }
}
