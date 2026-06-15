package br.gov.mandaguari.saude.tipomedicamento;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.tipomedicamento.domain.TipoMedicamento;
import br.gov.mandaguari.saude.tipomedicamento.dto.TipoMedicamentoDtos.*;
import br.gov.mandaguari.saude.tipomedicamento.mapper.TipoMedicamentoMapper;
import br.gov.mandaguari.saude.tipomedicamento.repository.TipoMedicamentoRepository;
import br.gov.mandaguari.saude.tipomedicamento.service.TipoMedicamentoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the mined Tipo de Medicamento rules. One test per rule in the SAU_TIPREM SLICE-SPEC.
 * Repository + audit are mocked; the real MapStruct mapper is used for response mapping.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TipoMedicamentoServiceTest {

    @Mock TipoMedicamentoRepository repo;
    @Mock AuditService audit;
    final TipoMedicamentoMapper mapper = Mappers.getMapper(TipoMedicamentoMapper.class);

    TipoMedicamentoService service() { return new TipoMedicamentoService(repo, mapper, audit); }

    private TipoMedicamentoCreateRequest validCreate() {
        return new TipoMedicamentoCreateRequest(100, "Controlado");
    }

    // R1 — codigo out of range (>999999) rejected
    @Test
    void rejectsCodigoOutOfRange() {
        var req = new TipoMedicamentoCreateRequest(1_000_000, "Controlado");
        assertThatThrownBy(() -> service().create(req))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("0 e 999999");
    }

    // R1 — duplicate codigo rejected on insert
    @Test
    void rejectsDuplicateCodigo() {
        when(repo.existsById(100)).thenReturn(true);
        assertThatThrownBy(() -> service().create(validCreate()))
                .isInstanceOf(Conflict.class).hasMessageContaining("100");
    }

    // R1 — codigo immutable on update (missing → 404)
    @Test
    void codigoImmutableOnUpdate() {
        when(repo.findById(999)).thenReturn(Optional.empty());
        var req = new TipoMedicamentoUpdateRequest("Novo");
        assertThatThrownBy(() -> service().update(999, req)).isInstanceOf(NotFound.class);
    }

    // R2 — descricao required
    @Test
    void rejectsBlankDescricao() {
        when(repo.existsById(100)).thenReturn(false);
        var req = new TipoMedicamentoCreateRequest(100, "  ");
        assertThatThrownBy(() -> service().create(req))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("descrição");
    }

    // R3 — delete blocked when referenced by a medicamento (SAU_REM)
    @Test
    void rejectsDeleteWhenReferencedByMedicamento() {
        TipoMedicamento t = new TipoMedicamento(); t.setCodigo(100); t.setDescricao("Controlado");
        when(repo.findById(100)).thenReturn(Optional.of(t));
        when(repo.isReferencedByMedicamento(100)).thenReturn(true);
        assertThatThrownBy(() -> service().delete(100)).isInstanceOf(Conflict.class);
        verify(repo, never()).delete(any());
    }

    // R3 — delete allowed when not referenced
    @Test
    void deletesWhenNotReferenced() {
        TipoMedicamento t = new TipoMedicamento(); t.setCodigo(100); t.setDescricao("Controlado");
        when(repo.findById(100)).thenReturn(Optional.of(t));
        when(repo.isReferencedByMedicamento(100)).thenReturn(false);
        service().delete(100);
        verify(repo).delete(t);
    }

    // R4 — create writes an audit record
    @Test
    void writesAuditOnCreate() {
        when(repo.existsById(100)).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        TipoMedicamentoResponse resp = service().create(validCreate());
        assertThat(resp.descricao()).isEqualTo("Controlado");
        verify(audit).record(eq("CREATE"), eq("SAU_TIPREM"), eq(100));
    }
}
