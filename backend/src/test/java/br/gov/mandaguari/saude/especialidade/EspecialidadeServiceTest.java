package br.gov.mandaguari.saude.especialidade;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.especialidade.domain.Especialidade;
import br.gov.mandaguari.saude.especialidade.dto.EspecialidadeDtos.*;
import br.gov.mandaguari.saude.especialidade.mapper.EspecialidadeMapper;
import br.gov.mandaguari.saude.especialidade.repository.EspecialidadeRepository;
import br.gov.mandaguari.saude.especialidade.service.EspecialidadeService;
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
 * Unit tests for the mined Especialidade rules. One test per rule in the SAU_ESP SLICE-SPEC.
 * Repository + audit are mocked; the real MapStruct mapper is used for response mapping.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EspecialidadeServiceTest {

    @Mock EspecialidadeRepository repo;
    @Mock AuditService audit;
    final EspecialidadeMapper mapper = Mappers.getMapper(EspecialidadeMapper.class);

    EspecialidadeService service() { return new EspecialidadeService(repo, mapper, audit); }

    private EspecialidadeCreateRequest validCreate() {
        return new EspecialidadeCreateRequest(100, "Cardiologia", "A", false, null, null);
    }

    // R1 — nome is required
    @Test
    void rejectsBlankNome() {
        var req = new EspecialidadeCreateRequest(100, "  ", null, null, null, null);
        assertThatThrownBy(() -> service().create(req))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("obrigatório");
    }

    // R2 — duplicate código rejected on insert
    @Test
    void rejectsDuplicateCodigo() {
        when(repo.existsById(100)).thenReturn(true);
        assertThatThrownBy(() -> service().create(validCreate()))
                .isInstanceOf(Conflict.class)
                .hasMessageContaining("100");
    }

    // R2 — código is immutable on update (update takes the path id; missing → 404)
    @Test
    void codigoImmutableOnUpdate() {
        when(repo.findById(999)).thenReturn(Optional.empty());
        var req = new EspecialidadeUpdateRequest("Nova", null, null, null, null);
        assertThatThrownBy(() -> service().update(999, req)).isInstanceOf(NotFound.class);
    }

    // R3 — unknown CBO rejected
    @Test
    void rejectsUnknownCbor() {
        when(repo.existsById(100)).thenReturn(false);
        when(repo.cborExists(55)).thenReturn(false);
        var req = new EspecialidadeCreateRequest(100, "Cardiologia", "A", false, 55, null);
        assertThatThrownBy(() -> service().create(req))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("CBO");
    }

    // R3 — cborDescricao derived from SAU_CBOR lookup
    @Test
    void derivesCborDescricao() {
        Especialidade e = new Especialidade();
        e.setCodigo(100); e.setNome("Cardiologia"); e.setCborCodigo(55);
        when(repo.findById(100)).thenReturn(Optional.of(e));
        when(repo.findCborDescricao(55)).thenReturn(Optional.of("Médico cardiologista"));

        EspecialidadeResponse resp = service().get(100);
        assertThat(resp.cborDescricao()).isEqualTo("Médico cardiologista");
    }

    // R4 — delete blocked when referenced by a profissional
    @Test
    void rejectsDeleteWhenReferenced() {
        Especialidade e = new Especialidade(); e.setCodigo(100); e.setNome("Cardiologia");
        when(repo.findById(100)).thenReturn(Optional.of(e));
        when(repo.isReferencedByProfissional(100)).thenReturn(true);
        assertThatThrownBy(() -> service().delete(100)).isInstanceOf(Conflict.class);
        verify(repo, never()).delete(any());
    }

    // R5 — vaga mínima > máxima rejected (confidence: low — verify against KB)
    @Test
    void rejectsVagaMinGreaterThanMax() {
        when(repo.existsById(100)).thenReturn(false);
        var agenda = new AgendaParametros(null, null, null, null, null, null, null, null,
                10, 5,           // vagaMuitoUrgente min=10 > max=5
                null, null, null, null, null, null);
        var req = new EspecialidadeCreateRequest(100, "Cardiologia", "A", false, null, agenda);
        assertThatThrownBy(() -> service().create(req))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("máxima");
    }

    // R6 — create/update/delete write an audit record
    @Test
    void writesAuditOnCreate() {
        when(repo.existsById(100)).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service().create(validCreate());
        verify(audit).record(eq("CREATE"), eq("SAU_ESP"), eq(100));
    }

    @Test
    void writesAuditOnUpdate() {
        Especialidade e = new Especialidade(); e.setCodigo(100); e.setNome("Cardiologia");
        when(repo.findById(100)).thenReturn(Optional.of(e));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service().update(100, new EspecialidadeUpdateRequest("Cardiologia Geral", null, null, null, null));
        verify(audit).record(eq("UPDATE"), eq("SAU_ESP"), eq(100));
    }

    @Test
    void writesAuditOnDelete() {
        Especialidade e = new Especialidade(); e.setCodigo(100); e.setNome("Cardiologia");
        when(repo.findById(100)).thenReturn(Optional.of(e));
        when(repo.isReferencedByProfissional(100)).thenReturn(false);
        service().delete(100);
        verify(audit).record(eq("DELETE"), eq("SAU_ESP"), eq(100));
    }
}
