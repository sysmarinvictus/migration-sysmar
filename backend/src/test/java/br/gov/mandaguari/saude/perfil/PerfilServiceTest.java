package br.gov.mandaguari.saude.perfil;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.perfil.domain.Perfil;
import br.gov.mandaguari.saude.perfil.dto.PerfilDtos.*;
import br.gov.mandaguari.saude.perfil.mapper.PerfilMapper;
import br.gov.mandaguari.saude.perfil.repository.PerfilRepository;
import br.gov.mandaguari.saude.perfil.service.PerfilService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the mined SAU_PRF (Perfil) rules. Repository + audit are mocked; the real MapStruct
 * mapper is used. Rule refs match the SLICE-SPEC SAU_PRF citations.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PerfilServiceTest {

    @Mock PerfilRepository repo;
    @Mock AuditService audit;
    final PerfilMapper mapper = Mappers.getMapper(PerfilMapper.class);

    PerfilService service() { return new PerfilService(repo, mapper, audit); }

    private Perfil perfil(int id, String nome) {
        Perfil p = new Perfil(); p.setId(id); p.setNome(nome); return p;
    }

    // R1 — PrfCod auto-allocated MAX+1
    @Test
    void create_allocatesNextPrfCodAsMaxPlusOne() {
        when(repo.findMaxId()).thenReturn(9);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service().create(new PerfilCreateRequest("Gestor"));
        verify(repo).save(argThat(p -> p.getId() == 10));
    }

    @Test
    void create_allocatesOneWhenEmpty() {
        when(repo.findMaxId()).thenReturn(0);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service().create(new PerfilCreateRequest("Gestor"));
        verify(repo).save(argThat(p -> p.getId() == 1));
    }

    // R2 — nome required
    @Test
    void create_rejectsBlankNome() {
        assertThatThrownBy(() -> service().create(new PerfilCreateRequest("  ")))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("Informe a descrição do perfil");
        verify(repo, never()).save(any());
    }

    // R3 — nome stored uppercase
    @Test
    void create_uppercasesNome() {
        when(repo.findMaxId()).thenReturn(0);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service().create(new PerfilCreateRequest("enfermeiro chefe"));
        verify(repo).save(argThat(p -> "ENFERMEIRO CHEFE".equals(p.getNome())));
    }

    // R10 — audit on create with the real allocated id
    @Test
    void create_writesAuditWithRealId() {
        when(repo.findMaxId()).thenReturn(4);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service().create(new PerfilCreateRequest("Gestor"));
        verify(audit).record(eq("CREATE"), eq("SAU_PRF"), eq(5));
    }

    // R2/R3 — update requires + uppercases nome, audits
    @Test
    void update_rejectsBlankNome() {
        when(repo.findById(5)).thenReturn(Optional.of(perfil(5, "GESTOR")));
        assertThatThrownBy(() -> service().update(5, new PerfilUpdateRequest("")))
                .isInstanceOf(BusinessRule.class);
    }

    @Test
    void update_uppercasesAndAudits() {
        Perfil p = perfil(5, "GESTOR");
        when(repo.findById(5)).thenReturn(Optional.of(p));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service().update(5, new PerfilUpdateRequest("gestor geral"));
        verify(repo).save(argThat(s -> "GESTOR GERAL".equals(s.getNome())));
        verify(audit).record(eq("UPDATE"), eq("SAU_PRF"), eq(5));
    }

    // R4 — delete blocked when referenced by a system user
    @Test
    void delete_blockedWhenReferencedByUsuario() {
        when(repo.findById(5)).thenReturn(Optional.of(perfil(5, "GESTOR")));
        when(repo.isReferencedByUsuario(5)).thenReturn(true);
        assertThatThrownBy(() -> service().delete(5))
                .isInstanceOf(Conflict.class)
                .hasMessageContaining("usuários do sistema");
        verify(repo, never()).delete(any(Perfil.class));
    }

    // R5 — delete blocked when set as the social-professional default profile (SAU_PAR4)
    @Test
    void delete_blockedWhenReferencedBySocialParam() {
        when(repo.findById(5)).thenReturn(Optional.of(perfil(5, "GESTOR")));
        when(repo.isReferencedByUsuario(5)).thenReturn(false);
        when(repo.isReferencedBySocialProfileParam(5)).thenReturn(true);
        assertThatThrownBy(() -> service().delete(5)).isInstanceOf(Conflict.class);
        verify(repo, never()).delete(any(Perfil.class));
    }

    // R6 — unreferenced delete cascades SAU_PRFCON, deletes, audits
    @Test
    void delete_cascadesPrfconAndAudits() {
        Perfil p = perfil(5, "GESTOR");
        when(repo.findById(5)).thenReturn(Optional.of(p));
        when(repo.isReferencedByUsuario(5)).thenReturn(false);
        when(repo.isReferencedBySocialProfileParam(5)).thenReturn(false);
        service().delete(5);
        verify(repo).deletePrfconByPrfCod(5);     // R6 cascade
        verify(repo).delete(p);
        verify(audit).record(eq("DELETE"), eq("SAU_PRF"), eq(5));
    }

    // R5 defensive — a missing SAU_PAR4 guard table must not block delete
    @Test
    void delete_toleratesMissingSocialParamTable() {
        Perfil p = perfil(5, "GESTOR");
        when(repo.findById(5)).thenReturn(Optional.of(p));
        when(repo.isReferencedByUsuario(5)).thenReturn(false);
        when(repo.isReferencedBySocialProfileParam(5)).thenThrow(new RuntimeException("sau_par4 absent"));
        service().delete(5);
        verify(repo).delete(p);
    }

    // get — not found
    @Test
    void get_notFound() {
        when(repo.findById(404)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().get(404)).isInstanceOf(NotFound.class);
    }
}
