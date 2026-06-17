package br.gov.mandaguari.saude.distrito;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.distrito.domain.Distrito;
import br.gov.mandaguari.saude.distrito.dto.DistritoDtos.*;
import br.gov.mandaguari.saude.distrito.mapper.DistritoMapperImpl;
import br.gov.mandaguari.saude.distrito.repository.DistritoRepository;
import br.gov.mandaguari.saude.distrito.service.DistritoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DistritoServiceTest {

    @Mock DistritoRepository repo;
    @Mock AuditService audit;

    DistritoService service;

    @BeforeEach
    void setup() {
        service = new DistritoService(repo, new DistritoMapperImpl(), audit);
    }

    // R6: codigo is system-assigned (MAX+1)
    @Test
    void autoAssignsCodigoOnInsert() {
        given(repo.findMaxCodigo()).willReturn(3);
        given(repo.save(any())).willAnswer(inv -> inv.getArgument(0));

        DistritoResponse res = service.create(minimalCreate("Centro Norte"));

        assertThat(res.codigo()).isEqualTo((short) 4);
    }

    // R1: nome is required
    @Test
    void createRequiresNome() {
        assertThatThrownBy(() -> service.create(minimalCreate("")))
                .isInstanceOf(BusinessRule.class);
    }

    @Test
    void createRequiresNomeBlank() {
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

        // blank ddd → no validation error
        DistritoResponse res = service.create(createWithDdd(""));

        assertThat(res.nome()).isEqualTo("Centro Norte");
    }

    // R7: audit recorded on create
    @Test
    void createAudits() {
        given(repo.findMaxCodigo()).willReturn(0);
        given(repo.save(any())).willAnswer(inv -> inv.getArgument(0));

        service.create(minimalCreate("Centro Norte"));

        verify(audit).record("CREATE", "SAU_DIS", (short) 1);
    }

    // R1 on update
    @Test
    void updateRequiresNome() {
        Distrito existing = distrito((short) 1, "Velho Nome");
        given(repo.findById((short) 1)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update((short) 1, updateReq("")))
                .isInstanceOf(BusinessRule.class);
    }

    // R4 on update
    @Test
    void updateRejectsAlphaDdd() {
        Distrito existing = distrito((short) 1, "DS Norte");
        given(repo.findById((short) 1)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update((short) 1, updateWithDdd("ZZ")))
                .isInstanceOf(BusinessRule.class);
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
