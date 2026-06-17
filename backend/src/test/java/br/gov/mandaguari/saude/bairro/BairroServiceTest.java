package br.gov.mandaguari.saude.bairro;

import br.gov.mandaguari.saude.bairro.domain.Bairro;
import br.gov.mandaguari.saude.bairro.dto.BairroDtos.*;
import br.gov.mandaguari.saude.bairro.mapper.BairroMapperImpl;
import br.gov.mandaguari.saude.bairro.repository.BairroRepository;
import br.gov.mandaguari.saude.bairro.service.BairroService;
import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
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
class BairroServiceTest {

    @Mock BairroRepository repo;
    @Mock AuditService audit;

    BairroService service;

    @BeforeEach
    void setup() {
        service = new BairroService(repo, new BairroMapperImpl(), audit);
    }

    // R1: codigo is system-assigned (MAX+1)
    @Test
    void autoAssignsCodigoOnInsert() {
        given(repo.findMaxCodigo()).willReturn(7);
        given(repo.existsByNomeIgnoreCase("Centro")).willReturn(false);
        given(repo.save(any())).willAnswer(inv -> inv.getArgument(0));

        BairroResponse res = service.create(new BairroCreateRequest("Centro"));

        assertThat(res.codigo()).isEqualTo(8);
    }

    @Test
    void startsAtOneWhenTableEmpty() {
        given(repo.findMaxCodigo()).willReturn(0);
        given(repo.existsByNomeIgnoreCase("Alto")).willReturn(false);
        given(repo.save(any())).willAnswer(inv -> inv.getArgument(0));

        BairroResponse res = service.create(new BairroCreateRequest("Alto"));

        assertThat(res.codigo()).isEqualTo(1);
    }

    // R2: nome required
    @Test
    void rejectsBlankNome() {
        assertThatThrownBy(() -> service.create(new BairroCreateRequest("  ")))
                .isInstanceOf(BusinessRule.class);
    }

    // R3: nome must be unique on create
    @Test
    void rejectsDuplicateNomeOnCreate() {
        given(repo.existsByNomeIgnoreCase("Centro")).willReturn(true);

        assertThatThrownBy(() -> service.create(new BairroCreateRequest("Centro")))
                .isInstanceOf(Conflict.class);
    }

    // R3: nome must be unique on update (excluding current record)
    @Test
    void rejectsDuplicateNomeOnUpdate() {
        Bairro existing = new Bairro();
        existing.setCodigo(3);
        existing.setNome("Jardim");
        given(repo.findById(3)).willReturn(Optional.of(existing));
        given(repo.existsByNomeIgnoreCaseAndCodigoNot("Centro", 3)).willReturn(true);

        assertThatThrownBy(() -> service.update(3, new BairroUpdateRequest("Centro")))
                .isInstanceOf(Conflict.class);
    }

    // R4: delete blocked when referenced by SYS_PES
    @Test
    void rejectsDeleteWhenReferencedByPessoa() {
        Bairro b = bairro(1, "Vila Nova");
        given(repo.findById(1)).willReturn(Optional.of(b));
        given(repo.isReferencedByPessoa(1)).willReturn(true);

        assertThatThrownBy(() -> service.delete(1))
                .isInstanceOf(Conflict.class)
                .hasMessageContaining("pessoa");
    }

    // R5: delete blocked when referenced by SAU_DIS
    @Test
    void rejectsDeleteWhenReferencedByDistrito() {
        Bairro b = bairro(2, "Universitário");
        given(repo.findById(2)).willReturn(Optional.of(b));
        given(repo.isReferencedByPessoa(2)).willReturn(false);
        given(repo.isReferencedByDistrito(2)).willReturn(true);

        assertThatThrownBy(() -> service.delete(2))
                .isInstanceOf(Conflict.class)
                .hasMessageContaining("distrito");
    }

    // R6: audit on create
    @Test
    void writesAuditOnCreate() {
        given(repo.findMaxCodigo()).willReturn(0);
        given(repo.existsByNomeIgnoreCase(anyString())).willReturn(false);
        given(repo.save(any())).willAnswer(inv -> inv.getArgument(0));

        service.create(new BairroCreateRequest("Setor Norte"));

        verify(audit).record("CREATE", "SAU_BAI", 1);
    }

    // R6: audit on delete
    @Test
    void writesAuditOnDelete() {
        Bairro b = bairro(5, "Setor Sul");
        given(repo.findById(5)).willReturn(Optional.of(b));
        given(repo.isReferencedByPessoa(5)).willReturn(false);
        given(repo.isReferencedByDistrito(5)).willReturn(false);

        service.delete(5);

        verify(audit).record("DELETE", "SAU_BAI", 5);
    }

    @Test
    void notFoundOnGet() {
        given(repo.findById(999)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(999))
                .isInstanceOf(NotFound.class);
    }

    // --- helpers ---

    private static Bairro bairro(int codigo, String nome) {
        Bairro b = new Bairro();
        b.setCodigo(codigo);
        b.setNome(nome);
        return b;
    }
}
