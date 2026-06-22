package br.gov.mandaguari.saude.unidade;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.unidade.domain.Unidade;
import br.gov.mandaguari.saude.unidade.dto.UnidadeDtos.*;
import br.gov.mandaguari.saude.unidade.mapper.UnidadeMapperImpl;
import br.gov.mandaguari.saude.unidade.repository.UnidadeRepository;
import br.gov.mandaguari.saude.unidade.service.UnidadeService;
import br.gov.mandaguari.saude.unidade.service.UnidadeSubService;
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
class UnidadeServiceTest {

    @Mock UnidadeRepository repo;
    @Mock AuditService audit;
    @Mock UnidadeSubService subService;

    UnidadeService service;

    // UnidadeCreateRequest has 54 fields (order matches DTO declaration):
    // 1:nome 2:razSoc 3:cnpj 4:cep 5:end 6:num 7:comp 8:bairro
    // 9:tel 10:fax 11:licFun 12:resp 13:email
    // 14:cnes 15:bpa 16:sipni 17:orgEmi 18:esf 19:psf 20:sisPrenatal 21:hiperdia 22:gestao
    // 23:sia 24:sigla 25:sit 26:siaSus 27:scnesId
    // 28:expEsus 29:expBnafar 30:cadCNS 31:cadEnd 32:ateSemCNS 33:ateSemEnd 34:encFisio
    // 35:externo 36:forPes 37:tipUni 38:ateSec 39:bloqPac 40:avisoVac 41:cadCPF 42:painel
    // 43:recMpp 44:recMppImp 45:baixaRem 46:bloqLanc 47:bloqDisp 48:bloqAg
    // 49:munCod 50:respProf 51:diretor 52:auditor 53:autorizador 54:distrito

    private static final String VALID_CNPJ = "11.222.333/0001-81";

    @BeforeEach
    void setup() {
        service = new UnidadeService(repo, new UnidadeMapperImpl(), audit, subService);
    }

    // R16: codigo system-assigned from seq_sau_uni_cod
    @Test
    void autoAssignsCodigoOnInsert() {
        given(repo.nextCodigo()).willReturn(6);
        given(repo.municipioExists(1)).willReturn(true);
        given(repo.save(any())).willAnswer(inv -> inv.getArgument(0));

        UnidadeResponse res = service.create(req("UBS Central", VALID_CNPJ, "87900000", "Rua A", "1", "Centro", 1));

        assertThat(res.codigo()).isEqualTo(6);
    }

    // R1: nome required
    @Test
    void createRequiresNome() {
        assertThatThrownBy(() -> service.create(req("", VALID_CNPJ, "87900000", "Rua A", "1", "Centro", 1)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Nome");
    }

    // R2: cnpj required
    @Test
    void createRequiresCnpj() {
        assertThatThrownBy(() -> service.create(req("UBS A", "", "87900000", "Rua A", "1", "Centro", 1)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("CNPJ");
    }

    // R2: invalid cnpj rejected
    @Test
    void createRejectsInvalidCnpj() {
        assertThatThrownBy(() -> service.create(req("UBS A", "11111111111111", "87900000", "Rua A", "1", "Centro", 1)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("inválido");
    }

    // R3: cep required
    @Test
    void createRequiresCep() {
        assertThatThrownBy(() -> service.create(req("UBS A", VALID_CNPJ, "", "Rua A", "1", "Centro", 1)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("CEP");
    }

    // R4: endereco required
    @Test
    void createRequiresEndereco() {
        assertThatThrownBy(() -> service.create(req("UBS A", VALID_CNPJ, "87900000", "", "1", "Centro", 1)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("endereço");
    }

    // R5: enderecoNumero required
    @Test
    void createRequiresEndNum() {
        assertThatThrownBy(() -> service.create(req("UBS A", VALID_CNPJ, "87900000", "Rua A", "", "Centro", 1)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("número");
    }

    // R6: bairro required
    @Test
    void createRequiresBairro() {
        assertThatThrownBy(() -> service.create(req("UBS A", VALID_CNPJ, "87900000", "Rua A", "1", "", 1)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Bairro");
    }

    // R7: municipio required
    @Test
    void createRequiresMunicipio() {
        assertThatThrownBy(() -> service.create(req("UBS A", VALID_CNPJ, "87900000", "Rua A", "1", "Bairro", 0)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Município");
    }

    // R7: municipio must exist
    @Test
    void createRejectsUnknownMunicipio() {
        given(repo.municipioExists(999)).willReturn(false);
        assertThatThrownBy(() -> service.create(req("UBS A", VALID_CNPJ, "87900000", "Rua A", "1", "Bairro", 999)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Município");
    }

    // R8: phone format
    @Test
    void createRejectsInvalidPhone() {
        given(repo.municipioExists(1)).willReturn(true);
        assertThatThrownBy(() -> service.create(reqWithPhone("UBS A", "123456")))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("inválido");
    }

    @Test
    void createAcceptsValidPhone() {
        given(repo.nextCodigo()).willReturn(1);
        given(repo.municipioExists(1)).willReturn(true);
        given(repo.save(any())).willAnswer(inv -> inv.getArgument(0));
        assertThat(service.create(reqWithPhone("UBS A", "(44) 3221-5000")).telefone())
                .isEqualTo("(44) 3221-5000");
    }

    // R10: cnes required when bpa=1
    @Test
    void createRequiresCnesWhenBpa() {
        given(repo.municipioExists(1)).willReturn(true);
        assertThatThrownBy(() -> service.create(reqWithBpa()))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("CNES");
    }

    // R11: orgaoEmissor requires diretorCodigo
    @Test
    void createRejectsOrgaoWithoutDiretor() {
        given(repo.municipioExists(1)).willReturn(true);
        assertThatThrownBy(() -> service.create(reqWithOrgao(null)))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Diretor");
    }

    // R12: orgaoEmissor requires autorizadorCodigo
    @Test
    void createRejectsOrgaoWithoutAutorizador() {
        given(repo.municipioExists(1)).willReturn(true);
        assertThatThrownBy(() -> service.create(reqWithOrgaoAndDiretor()))
                .isInstanceOf(BusinessRule.class).hasMessageContaining("Auditor");
    }

    // R26: Autorizador and Diretor must differ
    @Test
    void rejectsSameAutorizadorAndDiretor() {
        given(repo.municipioExists(1)).willReturn(true);
        assertThatThrownBy(() -> service.create(reqWithDirAut("OEM", 5L, 5L)))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("diferente do Diretor");
    }

    // R26: exception — OrgEmi starting 'U' allows Autorizador == Diretor
    @Test
    void allowsSameAutorizadorAndDiretorWhenOrgEmiStartsWithU() {
        given(repo.nextCodigo()).willReturn(1);
        given(repo.municipioExists(1)).willReturn(true);
        given(repo.save(any())).willAnswer(inv -> inv.getArgument(0));
        assertThat(service.create(reqWithDirAut("UXX", 5L, 5L)).codigo()).isEqualTo(1);
    }

    // R4: externo defaults to false on insert
    @Test
    void defaultsUniExtToFalse() {
        given(repo.nextCodigo()).willReturn(1);
        given(repo.municipioExists(1)).willReturn(true);
        given(repo.save(any())).willAnswer(inv -> inv.getArgument(0));
        assertThat(service.create(req("UBS A", VALID_CNPJ, "87900000", "Rua A", "1", "Centro", 1)).externo())
                .isFalse();
    }

    // R32: UniNom stored UPPERCASE
    @Test
    void storesUniNomUpperCase() {
        given(repo.nextCodigo()).willReturn(1);
        given(repo.municipioExists(1)).willReturn(true);
        given(repo.save(any())).willAnswer(inv -> inv.getArgument(0));
        assertThat(service.create(req("ubs central", VALID_CNPJ, "87900000", "Rua A", "1", "Centro", 1)).nome())
                .isEqualTo("UBS CENTRAL");
    }

    // R33: UniSigla stored UPPERCASE
    @Test
    void storesUniSiglaUpperCase() {
        given(repo.nextCodigo()).willReturn(1);
        given(repo.municipioExists(1)).willReturn(true);
        given(repo.save(any())).willAnswer(inv -> inv.getArgument(0));
        assertThat(service.create(reqWithText("UBS A", null, "Rua A", "Bairro", null, "ubsc")).sigla())
                .isEqualTo("UBSC");
    }

    // R34: UniRazSoc stored UPPERCASE
    @Test
    void storesUniRazSocUpperCase() {
        given(repo.nextCodigo()).willReturn(1);
        given(repo.municipioExists(1)).willReturn(true);
        given(repo.save(any())).willAnswer(inv -> inv.getArgument(0));
        assertThat(service.create(reqWithText("UBS A", "prefeitura municipal", "Rua A", "Bairro", null, null)).razaoSocial())
                .isEqualTo("PREFEITURA MUNICIPAL");
    }

    // R35: UniEnd, UniBai, UniRes stored UPPERCASE
    @Test
    void storesAddressFieldsUpperCase() {
        given(repo.nextCodigo()).willReturn(1);
        given(repo.municipioExists(1)).willReturn(true);
        given(repo.save(any())).willAnswer(inv -> inv.getArgument(0));
        UnidadeResponse res = service.create(
                reqWithText("UBS A", null, "rua das flores", "centro velho", "joão da silva", null));
        assertThat(res.endereco()).isEqualTo("RUA DAS FLORES");
        assertThat(res.bairro()).isEqualTo("CENTRO VELHO");
        assertThat(res.responsavel()).isEqualTo("JOÃO DA SILVA");
    }

    // delete guards
    @Test
    void deleteBlockedByUnisetor() {
        given(repo.findById(1)).willReturn(Optional.of(unidade(1)));
        given(repo.isReferencedByUnisetor(1)).willReturn(true);
        assertThatThrownBy(() -> service.delete(1)).isInstanceOf(Conflict.class);
    }

    @Test
    void deleteBlockedByPac() {
        given(repo.findById(1)).willReturn(Optional.of(unidade(1)));
        given(repo.isReferencedByUnisetor(1)).willReturn(false);
        given(repo.isReferencedByRecesp(1)).willReturn(false);
        given(repo.isReferencedByProEsp1(1)).willReturn(false);
        given(repo.isReferencedByPar5Sal(1)).willReturn(false);
        given(repo.isReferencedByPar5Sol(1)).willReturn(false);
        given(repo.isReferencedByUsuUni(1)).willReturn(false);
        given(repo.isReferencedByUsu(1)).willReturn(false);
        given(repo.isReferencedByRem1(1)).willReturn(false);
        given(repo.isReferencedByRemUnisetor(1)).willReturn(false);
        given(repo.isReferencedByPacAlt(1)).willReturn(false);
        given(repo.isReferencedByPacIns(1)).willReturn(false);
        given(repo.isReferencedByPac(1)).willReturn(true);
        assertThatThrownBy(() -> service.delete(1)).isInstanceOf(Conflict.class);
    }

    @Test
    void deleteAudits() {
        given(repo.findById(1)).willReturn(Optional.of(unidade(1)));
        given(repo.isReferencedByUnisetor(1)).willReturn(false);
        given(repo.isReferencedByRecesp(1)).willReturn(false);
        given(repo.isReferencedByProEsp1(1)).willReturn(false);
        given(repo.isReferencedByPar5Sal(1)).willReturn(false);
        given(repo.isReferencedByPar5Sol(1)).willReturn(false);
        given(repo.isReferencedByUsuUni(1)).willReturn(false);
        given(repo.isReferencedByUsu(1)).willReturn(false);
        given(repo.isReferencedByRem1(1)).willReturn(false);
        given(repo.isReferencedByRemUnisetor(1)).willReturn(false);
        given(repo.isReferencedByPacAlt(1)).willReturn(false);
        given(repo.isReferencedByPacIns(1)).willReturn(false);
        given(repo.isReferencedByPac(1)).willReturn(false);
        given(repo.isReferencedByPar2Des(1)).willReturn(false);
        given(repo.isReferencedByPar2Agend(1)).willReturn(false);
        service.delete(1);
        verify(subService).cascadeDeleteForUnidade(1); // R52–R55
        verify(audit).record("DELETE", "SAU_UNI", 1);
    }

    @Test
    void getThrowsNotFound() {
        given(repo.findById(99)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(99)).isInstanceOf(NotFound.class);
    }

    @Test
    void cnpjAlgorithmValidates() {
        assertThat(UnidadeService.isValidCnpj("11.222.333/0001-81")).isTrue();
        assertThat(UnidadeService.isValidCnpj("11111111111111")).isFalse();
        assertThat(UnidadeService.isValidCnpj("00000000000000")).isFalse();
    }

    // --- builders (54 args each) ---

    /** Base request; varies nome, cnpj, cep, endereco, num, bairro, municipioCodigo. */
    private static UnidadeCreateRequest req(String nome, String cnpj, String cep,
                                            String end, String num, String bairro, Integer mun) {
        // 1    2     3     4    5    6    7     8
        return new UnidadeCreateRequest(
                nome, null, cnpj, cep, end, num, null, bairro,
                // 9:tel  10:fax  11:lic  12:resp  13:email
                null, null, null, null, null,
                // 14:cnes  15:bpa  16:sipni  17:orgEmi  18:esf  19:psf  20:sisPre  21:hip  22:gest
                null, null, null, null, null, null, null, null, null,
                // 23:sia  24:sig  25:sit  26:siaSUS  27:scnesId
                // 28:expEsus  29:expBnafar  30:cadCNS  31:cadEnd  32:ateSemCNS  33:ateSemEnd  34:encFisio
                null, null, null, null, null, null, null, null, null, null, null, null,
                // 35:ext  36:forPes  37:tipUni  38:ateSec  39:bloqPac  40:avisoVac  41:cadCPF
                // 42:painel  43:recMpp  44:recMppImp  45:baixaRem  46:bloqLanc  47:bloqDisp  48:bloqAg
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                // 49:munCod  50:respProf  51:dir  52:aud  53:aut  54:distrito
                mun, null, null, null, null, null);
    }

    private static UnidadeCreateRequest reqWithPhone(String nome, String tel) {
        return new UnidadeCreateRequest(
                nome, null, VALID_CNPJ, "87900000", "Rua A", "1", null, "Bairro",
                tel, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                1, null, null, null, null, null);
    }

    private static UnidadeCreateRequest reqWithBpa() {
        return new UnidadeCreateRequest(
                "UBS A", null, VALID_CNPJ, "87900000", "Rua A", "1", null, "Bairro",
                null, null, null, null, null,
                null, (short) 1, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                1, null, null, null, null, null);
    }

    /** orgaoEmissor="OEM", diretorCodigo=dirCod (null → fires R11). */
    private static UnidadeCreateRequest reqWithOrgao(Long dirCod) {
        return new UnidadeCreateRequest(
                "UBS A", null, VALID_CNPJ, "87900000", "Rua A", "1", null, "Bairro",
                null, null, null, null, null,
                null, null, null, "OEM", null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                1, null, dirCod, null, null, null);
    }

    /** orgaoEmissor="OEM", diretorCodigo=10L, autorizadorCodigo=null → fires R12. */
    private static UnidadeCreateRequest reqWithOrgaoAndDiretor() {
        return new UnidadeCreateRequest(
                "UBS A", null, VALID_CNPJ, "87900000", "Rua A", "1", null, "Bairro",
                null, null, null, null, null,
                null, null, null, "OEM", null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                1, null, 10L, null, null, null);
    }

    /** orgaoEmissor=orgEmi, diretorCodigo=dir, autorizadorCodigo=aut (for R26). */
    private static UnidadeCreateRequest reqWithDirAut(String orgEmi, Long dir, Long aut) {
        return new UnidadeCreateRequest(
                "UBS A", null, VALID_CNPJ, "87900000", "Rua A", "1", null, "Bairro",
                null, null, null, null, null,
                null, null, null, orgEmi, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                1, null, dir, null, aut, null);
    }

    /** Varies the UPPERCASE-transformed text fields: nome, razaoSocial, endereco, bairro, responsavel, sigla. */
    private static UnidadeCreateRequest reqWithText(String nome, String razSoc, String end,
                                                    String bairro, String resp, String sigla) {
        return new UnidadeCreateRequest(
                nome, razSoc, VALID_CNPJ, "87900000", end, "1", null, bairro,
                null, null, null, resp, null,
                null, null, null, null, null, null, null, null, null,
                null, sigla, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                1, null, null, null, null, null);
    }

    private static Unidade unidade(Integer cod) {
        Unidade u = new Unidade();
        u.setCodigo(cod);
        u.setNome("UBS " + cod);
        return u;
    }
}
