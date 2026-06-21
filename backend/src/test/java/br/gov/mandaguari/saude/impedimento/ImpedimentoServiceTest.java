package br.gov.mandaguari.saude.impedimento;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.especialidade.domain.Especialidade;
import br.gov.mandaguari.saude.especialidade.repository.EspecialidadeRepository;
import br.gov.mandaguari.saude.impedimento.domain.Impedimento;
import br.gov.mandaguari.saude.impedimento.dto.ImpedimentoDtos.*;
import br.gov.mandaguari.saude.impedimento.mapper.ImpedimentoMapper;
import br.gov.mandaguari.saude.impedimento.repository.ImpedimentoRepository;
import br.gov.mandaguari.saude.impedimento.service.ImpedimentoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for mined Impedimento rules (SAU_IMP SLICE-SPEC). Repository + audit are mocked;
 * the real MapStruct mapper is used. Rule refs match the SLICE-SPEC citations.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImpedimentoServiceTest {

    static final LocalDate TODAY = LocalDate.of(2026, 6, 21);
    static final LocalDate DATE_INI = LocalDate.of(2026, 7, 1);
    static final LocalDate DATE_FIM = LocalDate.of(2026, 7, 31);

    @Mock ImpedimentoRepository repo;
    @Mock EspecialidadeRepository especialidadeRepo;
    @Mock AuditService audit;
    final ImpedimentoMapper mapper = Mappers.getMapper(ImpedimentoMapper.class);

    ImpedimentoService service() {
        return new ImpedimentoService(repo, especialidadeRepo, mapper, audit);
    }

    private Especialidade esp(int cod) {
        Especialidade e = new Especialidade();
        e.setCodigo(cod);
        e.setNome("Cardiologia");
        e.setCborCodigo("225125");
        return e;
    }

    private ImpedimentoCreateRequest validCreate() {
        return new ImpedimentoCreateRequest(TODAY, DATE_INI, DATE_FIM, 100L, 1);
    }

    private ImpedimentoUpdateRequest validUpdate() {
        return new ImpedimentoUpdateRequest(TODAY, DATE_INI, DATE_FIM, 100L, 1);
    }

    // R7 — profissionalCodigo must be non-zero
    @Test
    void rejectsProfissionalZero() {
        var req = new ImpedimentoCreateRequest(TODAY, DATE_INI, DATE_FIM, 0L, 1);
        assertThatThrownBy(() -> service().create(req))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("Profissional");
    }

    // R8 — profissional must exist
    @Test
    void rejectsMissingProfissional() {
        when(repo.profissionalExists(100L)).thenReturn(false);
        assertThatThrownBy(() -> service().create(validCreate()))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("não encontrado");
    }

    // R9 — especialidadeCodigo must be non-zero
    @Test
    void rejectsEspecialidadeZero() {
        when(repo.profissionalExists(100L)).thenReturn(true);
        var req = new ImpedimentoCreateRequest(TODAY, DATE_INI, DATE_FIM, 100L, 0);
        assertThatThrownBy(() -> service().create(req))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("Especialidade");
    }

    // R10 — especialidade must exist
    @Test
    void rejectsMissingEspecialidade() {
        when(repo.profissionalExists(100L)).thenReturn(true);
        when(especialidadeRepo.findById(1)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().create(validCreate()))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("não encontrada");
    }

    // R11 — profissional+especialidade pair must exist in SAU_PROESP
    @Test
    void rejectsMissingProEspPair() {
        when(repo.profissionalExists(100L)).thenReturn(true);
        when(especialidadeRepo.findById(1)).thenReturn(Optional.of(esp(1)));
        when(repo.proEspExists(100L, 1)).thenReturn(false);
        assertThatThrownBy(() -> service().create(validCreate()))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("especialidade");
    }

    // R4 — PK via MAX+1; no sequence
    @Test
    void assignsMaxPlusOnePk() {
        when(repo.profissionalExists(100L)).thenReturn(true);
        when(especialidadeRepo.findById(1)).thenReturn(Optional.of(esp(1)));
        when(repo.proEspExists(100L, 1)).thenReturn(true);
        when(repo.findMaxCodigo()).thenReturn(Optional.of(7));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().create(validCreate());

        verify(repo).save(argThat(imp -> imp.getCodigo() == 8));
    }

    // R4 — first record gets PK=1 when table is empty
    @Test
    void assignsPkOneWhenEmpty() {
        when(repo.profissionalExists(100L)).thenReturn(true);
        when(especialidadeRepo.findById(1)).thenReturn(Optional.of(esp(1)));
        when(repo.proEspExists(100L, 1)).thenReturn(true);
        when(repo.findMaxCodigo()).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().create(validCreate());

        verify(repo).save(argThat(imp -> imp.getCodigo() == 1));
    }

    // R6 — dataCadastro defaults to today when not supplied
    @Test
    void defaultsDataCadastroToToday() {
        when(repo.profissionalExists(100L)).thenReturn(true);
        when(especialidadeRepo.findById(1)).thenReturn(Optional.of(esp(1)));
        when(repo.proEspExists(100L, 1)).thenReturn(true);
        when(repo.findMaxCodigo()).thenReturn(Optional.of(0));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new ImpedimentoCreateRequest(null, DATE_INI, DATE_FIM, 100L, 1);
        service().create(req);

        // dataCadastro must not be null (defaulted to today by the service)
        verify(repo).save(argThat(imp -> imp.getDataCadastro() != null));
    }

    // R22 — audit record on create
    @Test
    void auditsOnCreate() {
        when(repo.profissionalExists(100L)).thenReturn(true);
        when(especialidadeRepo.findById(1)).thenReturn(Optional.of(esp(1)));
        when(repo.proEspExists(100L, 1)).thenReturn(true);
        when(repo.findMaxCodigo()).thenReturn(Optional.of(0));
        Impedimento saved = new Impedimento();
        saved.setCodigo(1);
        saved.setEspecialidade(esp(1));
        when(repo.save(any())).thenReturn(saved);

        service().create(validCreate());

        verify(audit).record(eq("CREATE"), eq("SAU_IMP"), eq(1));
    }

    // R22 — audit record on update
    @Test
    void auditsOnUpdate() {
        Impedimento existing = new Impedimento();
        existing.setCodigo(5);
        existing.setEspecialidade(esp(1));
        when(repo.findById(5)).thenReturn(Optional.of(existing));
        when(repo.profissionalExists(100L)).thenReturn(true);
        when(especialidadeRepo.findById(1)).thenReturn(Optional.of(esp(1)));
        when(repo.proEspExists(100L, 1)).thenReturn(true);
        when(repo.save(any())).thenReturn(existing);

        service().update(5, validUpdate());

        verify(audit).record(eq("UPDATE"), eq("SAU_IMP"), eq(5));
    }

    // R22 — audit record on delete
    @Test
    void auditsOnDelete() {
        Impedimento existing = new Impedimento();
        existing.setCodigo(5);
        when(repo.findById(5)).thenReturn(Optional.of(existing));

        service().delete(5);

        verify(audit).record(eq("DELETE"), eq("SAU_IMP"), eq(5));
    }

    // get — not found
    @Test
    void getThrowsWhenNotFound() {
        when(repo.findById(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().get(999))
                .isInstanceOf(NotFound.class);
    }
}
