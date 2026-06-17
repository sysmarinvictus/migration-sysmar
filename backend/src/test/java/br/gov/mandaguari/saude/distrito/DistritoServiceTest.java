package br.gov.mandaguari.saude.distrito;

import br.gov.mandaguari.saude.bairro.domain.Bairro;
import br.gov.mandaguari.saude.bairro.repository.BairroRepository;
import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.distrito.domain.Distrito;
import br.gov.mandaguari.saude.distrito.dto.DistritoDtos.*;
import br.gov.mandaguari.saude.distrito.mapper.DistritoMapperImpl;
import br.gov.mandaguari.saude.distrito.repository.DistritoRepository;
import br.gov.mandaguari.saude.distrito.service.DistritoService;
import br.gov.mandaguari.saude.tipologradouro.domain.TipoLogradouro;
import br.gov.mandaguari.saude.tipologradouro.repository.TipoLogradouroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DistritoServiceTest {

    @Mock DistritoRepository repo;
    @Mock AuditService audit;
    @Mock TipoLogradouroRepository tiplogRepo;
    @Mock BairroRepository bairroRepo;

    DistritoService service;

    @BeforeEach
    void setup() {
        service = new DistritoService(repo, new DistritoMapperImpl(), audit, tiplogRepo, bairroRepo);
        // Lenient: tests that throw before enrich() won't call these
        lenient().when(tiplogRepo.findById(anyInt())).thenReturn(Optional.empty());
        lenient().when(bairroRepo.findById(anyInt())).thenReturn(Optional.empty());
    }

    // R13: codigo is system-assigned (MAX+1)
    @Test
    void autoAssignsCodigoOnInsert() {
        given(repo.findMaxCodigo()).willReturn(3);
        given(repo.save(any())).willAnswer(inv -> inv.getArgument(0));

        DistritoResponse res = service.create(minimalCreate("Centro Norte"));

        assertThat(res.codigo()).isEqualTo((short) 4);
    }

    // R4: nome stored as uppercase
    @Test
    void nomeStoredAsUppercase() {
        given(repo.findMaxCodigo()).willReturn(0);
        given(repo.save(any())).willAnswer(inv -> inv.getArgument(0));

        DistritoResponse res = service.create(minimalCreate("centro norte"));

        assertThat(res.nome()).isEqualTo("CENTRO NORTE");
    }

    // R2: nome is required on INSERT (spec target: rejectsBlankNomeOnInsert)
    @Test
    void rejectsBlankNomeOnInsert() {
        assertThatThrownBy(() -> service.create(minimalCreate("")))
                .isInstanceOf(BusinessRule.class);
    }

    @Test
    void rejectsBlankNomeOnInsertWhitespace() {
        assertThatThrownBy(() -> service.create(minimalCreate("   ")))
                .isInstanceOf(BusinessRule.class);
    }

    // R2: DisTipLogCod must exist when non-null and non-zero
    @Test
    void createRejectsUnknownTipLog() {
        given(repo.tipLogExists(99)).willReturn(false);

        assertThatThrownBy(() -> service.create(createWithTipLog(99)))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("logradouro");
    }

    @Test
    void createSkipsTipLogValidationWhenZero() {
        given(repo.findMaxCodigo()).willReturn(0);
        given(repo.save(any())).willAnswer(inv -> inv.getArgument(0));

        // tipLogCod = 0 → no existsById check
        DistritoResponse res = service.create(createWithTipLog(0));

        assertThat(res.codigo()).isEqualTo((short) 1);
    }

    // R3: DisBaiCod must exist when non-null and non-zero
    @Test
    void createRejectsUnknownBairro() {
        given(repo.bairroExists(55)).willReturn(false);

        assertThatThrownBy(() -> service.create(createWithBairro(55)))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("Bairro");
    }

    // R4: DisDDD must consist of digits when provided
    @Test
    void createRejectsAlphaDdd() {
        assertThatThrownBy(() -> service.create(createWithDdd("AB")))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("Números");
    }

    @Test
    void createAcceptsNumericDdd() {
        given(repo.findMaxCodigo()).willReturn(0);
        given(repo.save(any())).willAnswer(inv -> inv.getArgument(0));

        DistritoResponse res = service.create(createWithDdd("44"));

        assertThat(res.ddd()).isEqualTo("44");
    }

    @Test
    void createAcceptsBlankDdd() {
        given(repo.findMaxCodigo()).willReturn(0);
        given(repo.save(any())).willAnswer(inv -> inv.getArgument(0));

        // blank ddd → no validation error; nome is uppercase-normalised (R4)
        DistritoResponse res = service.create(createWithDdd(""));

        assertThat(res.nome()).isEqualTo("CENTRO NORTE");
    }

    // R7: audit recorded on create
    @Test
    void createAudits() {
        given(repo.findMaxCodigo()).willReturn(0);
        given(repo.save(any())).willAnswer(inv -> inv.getArgument(0));

        service.create(minimalCreate("Centro Norte"));

        verify(audit).record("CREATE", "SAU_DIS", (short) 1);
    }

    // R3: nome is required on UPDATE (spec target: rejectsBlankNomeOnUpdate)
    @Test
    void rejectsBlankNomeOnUpdate() {
        Distrito existing = distrito((short) 1, "Velho Nome");
        given(repo.findById((short) 1)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update((short) 1, updateReq("")))
                .isInstanceOf(BusinessRule.class);
    }

    // R12: DisDDD digits-only on UPDATE (spec target: rejectsNonDigitDddOnUpdate)
    @Test
    void rejectsNonDigitDddOnUpdate() {
        Distrito existing = distrito((short) 1, "DS Norte");
        given(repo.findById((short) 1)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update((short) 1, updateWithDdd("ZZ")))
                .isInstanceOf(BusinessRule.class);
    }

    // R6: DisTipLogCod must exist when non-null and non-zero on UPDATE (spec target: rejectsUnknownTipLogOnUpdate)
    @Test
    void rejectsUnknownTipLogOnUpdate() {
        Distrito existing = distrito((short) 1, "DS Norte");
        given(repo.findById((short) 1)).willReturn(Optional.of(existing));
        given(repo.tipLogExists(77)).willReturn(false);

        assertThatThrownBy(() -> service.update((short) 1,
                new DistritoUpdateRequest("DS Norte", null, null, null, null, null, null, null, 77, null)))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("logradouro");
    }

    // R9: DisBaiCod must exist when non-null and non-zero on UPDATE (spec target: rejectsUnknownBairroOnUpdate)
    @Test
    void rejectsUnknownBairroOnUpdate() {
        Distrito existing = distrito((short) 1, "DS Norte");
        given(repo.findById((short) 1)).willReturn(Optional.of(existing));
        given(repo.bairroExists(88)).willReturn(false);

        assertThatThrownBy(() -> service.update((short) 1,
                new DistritoUpdateRequest("DS Norte", null, null, null, null, null, null, null, null, 88)))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("Bairro");
    }

    // R5: delete blocked by SAU_UNI
    @Test
    void deleteBlockedByUnidade() {
        Distrito d = distrito((short) 2, "DS Sul");
        given(repo.findById((short) 2)).willReturn(Optional.of(d));
        given(repo.isReferencedByUnidade((short) 2)).willReturn(true);

        assertThatThrownBy(() -> service.delete((short) 2))
                .isInstanceOf(Conflict.class)
                .hasMessageContaining("Unidade");
    }

    // R7: audit on delete
    @Test
    void deleteAudits() {
        Distrito d = distrito((short) 1, "DS Leste");
        given(repo.findById((short) 1)).willReturn(Optional.of(d));
        given(repo.isReferencedByUnidade((short) 1)).willReturn(false);

        service.delete((short) 1);

        verify(audit).record("DELETE", "SAU_DIS", (short) 1);
    }

    @Test
    void getThrowsNotFoundForUnknownCodigo() {
        given(repo.findById((short) 99)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.get((short) 99))
                .isInstanceOf(NotFound.class);
    }

    // R7: tiplogSigla derived from SAU_TIPLOG.TipLogSig on GET (spec target: derivesTipLogSigFromLookup)
    @Test
    void derivesTipLogSigFromLookup() {
        Distrito d = distrito((short) 1, "DS Centro");
        d.setTipoLogradouroCodigo(5);
        given(repo.findById((short) 1)).willReturn(Optional.of(d));
        TipoLogradouro tiplog = new TipoLogradouro();
        tiplog.setCodigo(5);
        tiplog.setSigla("AV");
        given(tiplogRepo.findById(5)).willReturn(Optional.of(tiplog));

        DistritoResponse res = service.get((short) 1);

        assertThat(res.tiplogSigla()).isEqualTo("AV");
    }

    // R10: bairroNome derived from SAU_BAI.BaiNom on GET (spec target: derivesBairroNomeFromLookup)
    @Test
    void derivesBairroNomeFromLookup() {
        Distrito d = distrito((short) 2, "DS Leste");
        d.setBairroCodigo(3);
        given(repo.findById((short) 2)).willReturn(Optional.of(d));
        Bairro bairro = new Bairro();
        bairro.setCodigo(3);
        bairro.setNome("Jardim América");
        given(bairroRepo.findById(3)).willReturn(Optional.of(bairro));

        DistritoResponse res = service.get((short) 2);

        assertThat(res.bairroNome()).isEqualTo("Jardim América");
    }

    // R14: duplicate DisCod → Conflict (DataIntegrityViolationException from DB → 409 via GlobalExceptionHandler)
    @Test
    void rejectsDuplicateCodigo() {
        given(repo.findMaxCodigo()).willReturn(3);
        given(repo.save(any())).willThrow(new DataIntegrityViolationException("uk_dis_cod"));

        assertThatThrownBy(() -> service.create(minimalCreate("DS Duplicado")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // R15: optimistic concurrency — deferred (requires @Version column migration, OQ7)
    @Disabled("OQ7: R15 optimistic locking deferred — requires @Version column in SAU_DIS schema migration")
    @Test
    void rejectsConcurrentModification() {}

    // R18: PK is structurally immutable on UPDATE — DistritoUpdateRequest has no codigo field;
    //      path param is the sole authority. Document as a structural invariant.
    @Test
    void updatePkIsBoundToPathParam() {
        // DistritoUpdateRequest intentionally omits 'codigo' — any attempt to change the PK
        // via the request body is structurally impossible. This test documents that invariant.
        assertThat(DistritoUpdateRequest.class.getDeclaredFields())
                .noneMatch(f -> f.getName().equals("codigo"));
    }

    // --- builders ---

    private static DistritoCreateRequest minimalCreate(String nome) {
        return new DistritoCreateRequest(nome, null, null, null, null, null, null, null, null, null);
    }

    private static DistritoCreateRequest createWithTipLog(Integer tipLogCod) {
        return new DistritoCreateRequest("Centro Norte", null, null, null, null, null, null, null, tipLogCod, null);
    }

    private static DistritoCreateRequest createWithBairro(Integer bairroCod) {
        return new DistritoCreateRequest("Centro Norte", null, null, null, null, null, null, null, null, bairroCod);
    }

    private static DistritoCreateRequest createWithDdd(String ddd) {
        return new DistritoCreateRequest("Centro Norte", null, null, null, null, ddd, null, null, null, null);
    }

    private static DistritoUpdateRequest updateReq(String nome) {
        return new DistritoUpdateRequest(nome, null, null, null, null, null, null, null, null, null);
    }

    private static DistritoUpdateRequest updateWithDdd(String ddd) {
        return new DistritoUpdateRequest("DS Norte", null, null, null, null, ddd, null, null, null, null);
    }

    private static Distrito distrito(Short cod, String nome) {
        Distrito d = new Distrito();
        d.setCodigo(cod);
        d.setNome(nome);
        return d;
    }
}
