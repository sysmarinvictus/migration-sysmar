package br.gov.mandaguari.saude.local;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.local.domain.Local;
import br.gov.mandaguari.saude.local.dto.LocalDtos.*;
import br.gov.mandaguari.saude.local.mapper.LocalMapper;
import br.gov.mandaguari.saude.local.repository.LocalRepository;
import br.gov.mandaguari.saude.local.repository.LocalRepository.MunicipioInfo;
import br.gov.mandaguari.saude.local.service.LocalService;
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
 * Unit tests for the mined Local rules. One test per rule in the SAU_LOC SLICE-SPEC. Repository +
 * audit are mocked; the real MapStruct mapper is used for response mapping.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LocalServiceTest {

    @Mock LocalRepository repo;
    @Mock AuditService audit;
    final LocalMapper mapper = Mappers.getMapper(LocalMapper.class);

    LocalService service() { return new LocalService(repo, mapper, audit); }

    private static MunicipioInfo municipio(String nome, String uf, String ibge) {
        return new MunicipioInfo() {
            public String getNome() { return nome; }
            public String getUf() { return uf; }
            public String getIbge() { return ibge; }
        };
    }

    private LocalCreateRequest validCreate() {
        return new LocalCreateRequest(100, "Centro", 4114402);
    }

    private void municipioExists() {
        when(repo.findMunicipio(4114402)).thenReturn(Optional.of(municipio("Mandaguari", "PR", "4114402")));
    }

    // R1 — codigo out of range (>999999) rejected
    @Test
    void rejectsCodigoOutOfRange() {
        var req = new LocalCreateRequest(1_000_000, "Centro", 4114402);
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
        var req = new LocalUpdateRequest("Novo", 4114402);
        assertThatThrownBy(() -> service().update(999, req)).isInstanceOf(NotFound.class);
    }

    // R2 — nome required
    @Test
    void rejectsBlankNome() {
        when(repo.existsById(100)).thenReturn(false);
        var req = new LocalCreateRequest(100, "  ", 4114402);
        assertThatThrownBy(() -> service().create(req))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("nome");
    }

    // R3 — município required (0/absent rejected)
    @Test
    void rejectsMissingMunicipio() {
        when(repo.existsById(100)).thenReturn(false);
        var req = new LocalCreateRequest(100, "Centro", 0);
        assertThatThrownBy(() -> service().create(req))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("município");
    }

    // R4 — unknown município rejected
    @Test
    void rejectsUnknownMunicipio() {
        when(repo.existsById(100)).thenReturn(false);
        when(repo.findMunicipio(4114402)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().create(validCreate()))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("não existe");
    }

    // R4 — município name/UF/IBGE derived from SYS_MUN
    @Test
    void derivesMunicipioFields() {
        Local l = new Local(); l.setCodigo(100); l.setNome("Centro"); l.setMunicipioCodigo(4114402);
        when(repo.findById(100)).thenReturn(Optional.of(l));
        municipioExists();
        LocalResponse resp = service().get(100);
        assertThat(resp.municipioNome()).isEqualTo("Mandaguari");
        assertThat(resp.municipioUf()).isEqualTo("PR");
        assertThat(resp.municipioIbge()).isEqualTo("4114402");
    }

    // R5 — delete is unconditional (no delete guard)
    @Test
    void deletesFreely() {
        Local l = new Local(); l.setCodigo(100); l.setNome("Centro");
        when(repo.findById(100)).thenReturn(Optional.of(l));
        service().delete(100);
        verify(repo).delete(l);
    }

    // R6 — create writes an audit record
    @Test
    void writesAuditOnCreate() {
        when(repo.existsById(100)).thenReturn(false);
        municipioExists();
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service().create(validCreate());
        verify(audit).record(eq("CREATE"), eq("SAU_LOC"), eq(100));
    }
}
