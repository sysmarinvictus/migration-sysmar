package br.gov.mandaguari.saude.conselhoclasse;

import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.conselhoclasse.domain.ConselhoClasse;
import br.gov.mandaguari.saude.conselhoclasse.dto.ConselhoClasseDtos.*;
import br.gov.mandaguari.saude.conselhoclasse.mapper.ConselhoClasseMapper;
import br.gov.mandaguari.saude.conselhoclasse.repository.ConselhoClasseRepository;
import br.gov.mandaguari.saude.conselhoclasse.service.ConselhoClasseService;
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
import static org.mockito.Mockito.*;

/**
 * Unit tests for the mined Conselho de Classe rules. One test per rule in the SAU_CONCLA SLICE-SPEC.
 * Repository is mocked; the real MapStruct mapper is used for response mapping.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConselhoClasseServiceTest {

    @Mock ConselhoClasseRepository repo;
    final ConselhoClasseMapper mapper = Mappers.getMapper(ConselhoClasseMapper.class);

    ConselhoClasseService service() { return new ConselhoClasseService(repo, mapper); }

    private ConselhoClasseCreateRequest validCreate() {
        return new ConselhoClasseCreateRequest((short) 100, "CRM", "Conselho Regional de Medicina");
    }

    // R1 — codigo out of range (>999) rejected
    @Test
    void rejectsCodigoOutOfRange() {
        var req = new ConselhoClasseCreateRequest((short) 1000, "CRM", "x");
        assertThatThrownBy(() -> service().create(req))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("0 e 999");
    }

    // R2 — duplicate codigo rejected on insert
    @Test
    void rejectsDuplicateCodigo() {
        when(repo.existsById((short) 100)).thenReturn(true);
        assertThatThrownBy(() -> service().create(validCreate()))
                .isInstanceOf(Conflict.class)
                .hasMessageContaining("100");
    }

    // R2 — codigo is immutable on update (update takes the path id; missing → 404)
    @Test
    void codigoImmutableOnUpdate() {
        when(repo.findById((short) 999)).thenReturn(Optional.empty());
        var req = new ConselhoClasseUpdateRequest("CRF", "Conselho Regional de Farmácia");
        assertThatThrownBy(() -> service().update((short) 999, req)).isInstanceOf(NotFound.class);
    }

    // R3 — delete blocked when referenced by a profissional (SAU_PRO)
    @Test
    void rejectsDeleteWhenReferencedByProfissional() {
        ConselhoClasse c = new ConselhoClasse(); c.setCodigo((short) 100); c.setSigla("CRM");
        when(repo.findById((short) 100)).thenReturn(Optional.of(c));
        when(repo.isReferencedByProfissional((short) 100)).thenReturn(true);
        assertThatThrownBy(() -> service().delete((short) 100)).isInstanceOf(Conflict.class);
        verify(repo, never()).delete(any());
    }

    // R5 — sigla/nome are optional (null accepted on create)
    @Test
    void allowsNullSiglaAndNome() {
        when(repo.existsById((short) 50)).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var req = new ConselhoClasseCreateRequest((short) 50, null, null);
        ConselhoClasseResponse resp = service().create(req);
        assertThat(resp.codigo()).isEqualTo((short) 50);
        assertThat(resp.sigla()).isNull();
        assertThat(resp.nome()).isNull();
    }

    // happy path — create persists and maps
    @Test
    void createsValid() {
        when(repo.existsById((short) 100)).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ConselhoClasseResponse resp = service().create(validCreate());
        assertThat(resp.sigla()).isEqualTo("CRM");
        verify(repo).save(any());
    }
}
