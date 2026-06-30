package br.gov.mandaguari.saude.programa;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.programa.domain.Programa;
import br.gov.mandaguari.saude.programa.dto.ProgramaDtos.*;
import br.gov.mandaguari.saude.programa.mapper.ProgramaMapper;
import br.gov.mandaguari.saude.programa.repository.GrupoProgramaRepository;
import br.gov.mandaguari.saude.programa.repository.ProgramaRepository;
import br.gov.mandaguari.saude.programa.service.ProgramaService;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProgramaServiceTest {

    @Mock ProgramaRepository repo;
    @Mock GrupoProgramaRepository grupoRepo;
    @Mock AuditService audit;
    final ProgramaMapper mapper = Mappers.getMapper(ProgramaMapper.class);

    ProgramaService service() { return new ProgramaService(repo, grupoRepo, mapper, audit); }

    private ProgramaCreateRequest create(String id, Boolean admin) {
        return new ProgramaCreateRequest(id, "Atendimento médico", 3, admin, false, false);
    }

    @Test
    void create_rejectsDuplicateId() {
        when(repo.existsById("ATEMED")).thenReturn(true);
        assertThatThrownBy(() -> service().create(create("ATEMED", false)))
                .isInstanceOf(Conflict.class);
        verify(repo, never()).save(any());
    }

    @Test
    void create_persistsFlagsAndAudits() {
        when(repo.existsById("ATEMED")).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ProgramaResponse resp = service().create(create("ATEMED", true));
        verify(repo).save(argThat(p -> p.getId().equals("ATEMED")
                && p.getAdmin() == 1 && p.getMedico() == 0));
        verify(audit).record(eq("CREATE"), eq("SAU_PRG"), eq("ATEMED"));
        assertThat(resp.admin()).isTrue();
    }

    @Test
    void delete_blockedWhenReferencedByPermission() {
        when(repo.findById("ATEMED")).thenReturn(Optional.of(prog("ATEMED")));
        when(repo.isReferencedByPermission("ATEMED")).thenReturn(true);
        assertThatThrownBy(() -> service().delete("ATEMED")).isInstanceOf(Conflict.class);
        verify(repo, never()).delete(any(Programa.class));
    }

    @Test
    void delete_allowedWhenUnreferenced() {
        Programa p = prog("ATEMED");
        when(repo.findById("ATEMED")).thenReturn(Optional.of(p));
        when(repo.isReferencedByPermission("ATEMED")).thenReturn(false);
        service().delete("ATEMED");
        verify(repo).delete(p);
        verify(audit).record(eq("DELETE"), eq("SAU_PRG"), eq("ATEMED"));
    }

    @Test
    void get_notFound() {
        when(repo.findById("X")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().get("X")).isInstanceOf(NotFound.class);
    }

    private Programa prog(String id) {
        Programa p = new Programa(); p.setId(id); p.setAdmin((short) 0); p.setMedico((short) 0); return p;
    }
}
