package br.gov.mandaguari.saude.setor;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.setor.domain.UniSetor;
import br.gov.mandaguari.saude.setor.domain.UniSetorId;
import br.gov.mandaguari.saude.setor.dto.UniSetorDtos.*;
import br.gov.mandaguari.saude.setor.mapper.UniSetorMapperImpl;
import br.gov.mandaguari.saude.setor.repository.UniSetorRepository;
import br.gov.mandaguari.saude.setor.service.UniSetorService;
import br.gov.mandaguari.saude.unidade.domain.Unidade;
import br.gov.mandaguari.saude.unidade.repository.UnidadeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UniSetorServiceTest {

    @Mock UniSetorRepository repo;
    @Mock UnidadeRepository unidadeRepo;
    @Mock AuditService audit;

    UniSetorService service;

    static final Integer UNI_COD = 1;
    static final Integer SETOR_COD = 10;

    @BeforeEach
    void setup() {
        service = new UniSetorService(repo, new UniSetorMapperImpl(), audit, unidadeRepo);
        lenient().when(unidadeRepo.existsById(UNI_COD)).thenReturn(true);
        lenient().when(unidadeRepo.findById(UNI_COD)).thenReturn(Optional.of(stubUnidade()));
        lenient().when(repo.existsById(any())).thenReturn(false);
        lenient().when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // R7: UniCod must exist in SAU_UNI
    @Test
    void rejectsUnidadeNotFound() {
        given(unidadeRepo.existsById(99)).willReturn(false);
        assertThatThrownBy(() -> service.create(99, minimalCreate(SETOR_COD, "TRIAGEM")))
                .isInstanceOf(BusinessRule.class);
    }

    // R9: composite PK unique on INSERT
    @Test
    void rejectsDuplicatePrimaryKey() {
        given(repo.existsById(new UniSetorId(UNI_COD, SETOR_COD))).willReturn(true);
        assertThatThrownBy(() -> service.create(UNI_COD, minimalCreate(SETOR_COD, "TRIAGEM")))
                .isInstanceOf(Conflict.class);
    }

    // R11: SetorNom stored uppercase
    @Test
    void nomeStoredAsUppercase() {
        UniSetorResponse res = service.create(UNI_COD, minimalCreate(SETOR_COD, "triagem central"));
        assertThat(res.nome()).isEqualTo("TRIAGEM CENTRAL");
    }

    // R11: leading/trailing whitespace trimmed
    @Test
    void nomeTrimmedBeforeUppercase() {
        UniSetorResponse res = service.create(UNI_COD, minimalCreate(SETOR_COD, "  farmacia  "));
        assertThat(res.nome()).isEqualTo("FARMACIA");
    }

    // R17: audit event published on create
    @Test
    void createAudits() {
        service.create(UNI_COD, minimalCreate(SETOR_COD, "TRIAGEM"));
        verify(audit).record(eq("CREATE"), eq("SAU_UNISETOR"), any());
    }

    // R17: audit event published on update
    @Test
    void updateAudits() {
        given(repo.findById(new UniSetorId(UNI_COD, SETOR_COD))).willReturn(Optional.of(stubSetor()));
        service.update(UNI_COD, SETOR_COD, updateReq("FARMACIA"));
        verify(audit).record(eq("UPDATE"), eq("SAU_UNISETOR"), any());
    }

    // R17: audit event published on delete
    @Test
    void deleteAudits() {
        given(repo.findById(new UniSetorId(UNI_COD, SETOR_COD))).willReturn(Optional.of(stubSetor()));
        service.delete(UNI_COD, SETOR_COD);
        verify(audit).record(eq("DELETE"), eq("SAU_UNISETOR"), any());
    }

    // R18: path params are authoritative PK — update body has no PK fields
    @Test
    void updateUsesPathParamAsKey() {
        UniSetor existing = stubSetor();
        given(repo.findById(new UniSetorId(UNI_COD, SETOR_COD))).willReturn(Optional.of(existing));
        service.update(UNI_COD, SETOR_COD, updateReq("FARMACIA"));
        assertThat(existing.getUniCod()).isEqualTo(UNI_COD);
        assertThat(existing.getSetorCod()).isEqualTo(SETOR_COD);
    }

    // R8/R20: derived fields come from SAU_UNI (not from SAU_UNISETOR columns)
    @Test
    void enrichPopulatesUnidadeFields() {
        given(repo.findById(new UniSetorId(UNI_COD, SETOR_COD))).willReturn(Optional.of(stubSetor()));
        UniSetorResponse res = service.get(UNI_COD, SETOR_COD);
        assertThat(res.unidadeNome()).isEqualTo("UBS TESTE");
        assertThat(res.unidadeCnes()).isEqualTo(1234567);
    }

    // GET: returns 404 for unknown composite key
    @Test
    void getThrowsNotFoundForUnknownKey() {
        given(repo.findById(any())).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(UNI_COD, 999))
                .isInstanceOf(NotFound.class);
    }

    // R12: DELETE blocked by SAU_PAR5
    @Test
    void rejectsDeleteWhenReferencedBySauPar5() {
        given(repo.findById(new UniSetorId(UNI_COD, SETOR_COD))).willReturn(Optional.of(stubSetor()));
        given(repo.isReferencedByPar5(UNI_COD, SETOR_COD)).willReturn(true);
        assertThatThrownBy(() -> service.delete(UNI_COD, SETOR_COD))
                .isInstanceOf(Conflict.class);
    }

    // R13: DELETE blocked by SAU_USUUNI1
    @Test
    void rejectsDeleteWhenReferencedBySauUsuUni1() {
        given(repo.findById(new UniSetorId(UNI_COD, SETOR_COD))).willReturn(Optional.of(stubSetor()));
        given(repo.isReferencedByUsuUni1(UNI_COD, SETOR_COD)).willReturn(true);
        assertThatThrownBy(() -> service.delete(UNI_COD, SETOR_COD))
                .isInstanceOf(Conflict.class);
    }

    // R14: DELETE blocked by SAU_REMLOT
    @Test
    void rejectsDeleteWhenReferencedBySauRemLot() {
        given(repo.findById(new UniSetorId(UNI_COD, SETOR_COD))).willReturn(Optional.of(stubSetor()));
        given(repo.isReferencedByRemLot(UNI_COD, SETOR_COD)).willReturn(true);
        assertThatThrownBy(() -> service.delete(UNI_COD, SETOR_COD))
                .isInstanceOf(Conflict.class);
    }

    // R15: DELETE blocked by SAU_REM_UNISETOR
    @Test
    void rejectsDeleteWhenReferencedBySauRemUnisetor() {
        given(repo.findById(new UniSetorId(UNI_COD, SETOR_COD))).willReturn(Optional.of(stubSetor()));
        given(repo.isReferencedByRemUnisetor(UNI_COD, SETOR_COD)).willReturn(true);
        assertThatThrownBy(() -> service.delete(UNI_COD, SETOR_COD))
                .isInstanceOf(Conflict.class);
    }

    // R10: optimistic concurrency — deferred (requires @Version column via Flyway, see OQ8)
    @Test
    @Disabled("OQ8: optimistic concurrency deferred — @Version requires schema migration approval")
    void rejectsStaleUpdateOnConcurrentChange() {}

    // --- helpers ---

    private UniSetorCreateRequest minimalCreate(int setorCod, String nome) {
        return new UniSetorCreateRequest(setorCod, nome, (short) 0, "ativo", null, null, null);
    }

    private UniSetorUpdateRequest updateReq(String nome) {
        return new UniSetorUpdateRequest(nome, (short) 0, "ativo", null, null, null);
    }

    private UniSetor stubSetor() {
        UniSetor s = new UniSetor();
        s.setUniCod(UNI_COD);
        s.setSetorCod(SETOR_COD);
        s.setNome("TRIAGEM");
        s.setEstocador((short) 0);
        s.setSituacao("ativo");
        return s;
    }

    private Unidade stubUnidade() {
        Unidade u = new Unidade();
        u.setCodigo(UNI_COD);
        u.setNome("UBS TESTE");
        u.setCnes(1234567);
        u.setSituacao((short) 1);
        return u;
    }
}
