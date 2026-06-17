package br.gov.mandaguari.saude.unidade.service;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.unidade.domain.Unidade;
import br.gov.mandaguari.saude.unidade.dto.UnidadeDtos.*;
import br.gov.mandaguari.saude.unidade.mapper.UnidadeMapper;
import br.gov.mandaguari.saude.unidade.repository.UnidadeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Unidade de Atendimento business logic — rules mined from {@code sau_uni_impl.java}
 * (checkExtendedTable0E24). Each rule carries a {@code // R<n>} citation.
 */
@Service
@Transactional(readOnly = true)
public class UnidadeService {

    private final UnidadeRepository repo;
    private final UnidadeMapper mapper;
    private final AuditService audit;

    public UnidadeService(UnidadeRepository repo, UnidadeMapper mapper, AuditService audit) {
        this.repo = repo;
        this.mapper = mapper;
        this.audit = audit;
    }

    public Page<UnidadeResponse> list(String nome, Pageable pageable) {
        Page<Unidade> page = (nome == null || nome.isBlank())
                ? repo.findAll(pageable)
                : repo.findByNomeContainingIgnoreCase(nome, pageable);
        return page.map(mapper::toResponse);
    }

    public UnidadeResponse get(Integer codigo) {
        return mapper.toResponse(find(codigo));
    }

    public List<UnidadeLookupItem> lookup(String q, Pageable pageable) {
        return repo.lookup(q == null ? "" : q, pageable).stream().map(mapper::toLookupItem).toList();
    }

    @Transactional
    public UnidadeResponse create(UnidadeCreateRequest req) {
        validateRequired(req);

        Integer nextCodigo = repo.findMaxCodigo() + 1;               // R16: system MAX+1
        Unidade u = buildFrom(req);
        u.setCodigo(nextCodigo);

        Unidade saved = repo.save(u);
        audit.record("CREATE", "SAU_UNI", saved.getCodigo());        // R17
        return mapper.toResponse(saved);
    }

    @Transactional
    public UnidadeResponse update(Integer codigo, UnidadeUpdateRequest req) {
        Unidade u = find(codigo);
        validateRequired(req.nome(), req.cnpj(), req.cep(), req.endereco(),
                req.enderecoNumero(), req.bairro(), req.municipioCodigo(),
                req.telefone(), req.fax(), req.cnes(), req.bpa(),
                req.sisPreNatal(), req.hiperdia(), req.sipni(),
                req.orgaoEmissor(), req.diretorCodigo(), req.autorizadorCodigo(),
                req.distritoCodigo(), req.tipoUnidadeCodigo());
        applyFields(u, req.nome(), req.razaoSocial(), req.cnpj(), req.cep(),
                req.endereco(), req.enderecoNumero(), req.enderecoComplemento(),
                req.bairro(), req.telefone(), req.fax(), req.licencaFuncionamento(),
                req.responsavel(), req.email(), req.cnes(), req.bpa(), req.sipni(),
                req.orgaoEmissor(), req.estrategiaFamiliar(), req.psf(),
                req.sisPreNatal(), req.hiperdia(), req.gestao(), req.sia(),
                req.sigla(), req.situacao(), req.siaSus(), req.scnesId(),
                req.exportarEsus(), req.exportarBnafar(), req.cadastroCns(),
                req.cadastroEndereco(), req.atendimentoSemCns(), req.atendimentoSemEndereco(),
                req.encaminhamentoFisioterapia(), req.externo(), req.forPesCod(),
                req.tipoUnidadeCodigo(), req.atencaoSecundaria(), req.bloqueioPacSemCadInd(),
                req.avisoVacinaAtrasada(), req.cadastroCpf(), req.painel(),
                req.recepcaoIntermedMpp(), req.recepcaoIntermedMppImp(),
                req.baixaRemSemCns(), req.bloqueioLancPcdAut(), req.bloqueioDispPacExt(),
                req.bloqueioAgendSolExaPacExt(), req.municipioCodigo(),
                req.respProfissionalCodigo(), req.diretorCodigo(), req.auditorCodigo(),
                req.autorizadorCodigo(), req.distritoCodigo());

        Unidade saved = repo.save(u);
        audit.record("UPDATE", "SAU_UNI", saved.getCodigo());        // R17
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(Integer codigo) {
        Unidade u = find(codigo);
        guardDeleteConstraints(codigo);
        repo.delete(u);
        audit.record("DELETE", "SAU_UNI", codigo);                   // R17
    }

    // --- helpers ---

    private Unidade find(Integer codigo) {
        return repo.findById(codigo)
                .orElseThrow(() -> new NotFound("Unidade " + codigo + " não encontrada"));
    }

    private void validateRequired(UnidadeCreateRequest req) {
        validateRequired(req.nome(), req.cnpj(), req.cep(), req.endereco(),
                req.enderecoNumero(), req.bairro(), req.municipioCodigo(),
                req.telefone(), req.fax(), req.cnes(), req.bpa(),
                req.sisPreNatal(), req.hiperdia(), req.sipni(),
                req.orgaoEmissor(), req.diretorCodigo(), req.autorizadorCodigo(),
                req.distritoCodigo(), req.tipoUnidadeCodigo());
    }

    private void validateRequired(String nome, String cnpj, String cep, String endereco,
                                  String enderecoNumero, String bairro, Integer municipioCodigo,
                                  String telefone, String fax, Integer cnes,
                                  Short bpa, Short sisPreNatal, Short hiperdia, Short sipni,
                                  String orgaoEmissor, Long diretorCodigo, Long autorizadorCodigo,
                                  Short distritoCodigo, Integer tipoUnidadeCodigo) {
        // R1
        if (nome == null || nome.isBlank())
            throw new BusinessRule("uni.nome.required", "Informe o Nome da Unidade!");
        // R2
        if (cnpj == null || cnpj.isBlank())
            throw new BusinessRule("uni.cnpj.required", "Informe o CNPJ!");
        if (!isValidCnpj(cnpj))
            throw new BusinessRule("uni.cnpj.invalido", "CNPJ inválido! Tente novamente.");
        // R3
        if (cep == null || cep.isBlank())
            throw new BusinessRule("uni.cep.required", "Informe o CEP!");
        // R4
        if (endereco == null || endereco.isBlank())
            throw new BusinessRule("uni.endereco.required", "Informe o endereço!");
        // R5
        if (enderecoNumero == null || enderecoNumero.isBlank())
            throw new BusinessRule("uni.endnum.required", "Informe o número do endereço!");
        // R6
        if (bairro == null || bairro.isBlank())
            throw new BusinessRule("uni.bairro.required", "Informe o Bairro!");
        // R7
        if (municipioCodigo == null || municipioCodigo == 0)
            throw new BusinessRule("uni.municipio.required", "Informe o código do Município!");
        if (!repo.municipioExists(municipioCodigo))
            throw new BusinessRule("uni.municipio.notfound", "Não existe 'Município'.");
        // R8
        validatePhone("uni.telefone.invalido", "Telefone inválido!", telefone);
        // R9
        validatePhone("uni.fax.invalido", "Celular inválido!", fax);
        // R10
        boolean needsCnes = isFlag1(bpa) || isFlag1(sisPreNatal) || isFlag1(hiperdia) || isFlag1(sipni);
        if (needsCnes && (cnes == null || cnes == 0))
            throw new BusinessRule("uni.cnes.required", "Informe o número do CNES!");
        // R11: orgaoEmissor requires diretorCodigo
        if (orgaoEmissor != null && !orgaoEmissor.isBlank()
                && (diretorCodigo == null || diretorCodigo == 0))
            throw new BusinessRule("uni.diretor.required", "Informe o Diretor Clínico!");
        // R12: orgaoEmissor requires autorizadorCodigo
        if (orgaoEmissor != null && !orgaoEmissor.isBlank()
                && (autorizadorCodigo == null || autorizadorCodigo == 0))
            throw new BusinessRule("uni.autorizador.required", "Informe o Profissional Auditor!");
        // FK validations (skip zero/null)
        if (distritoCodigo != null && distritoCodigo != 0 && !repo.distritoExists(distritoCodigo))
            throw new BusinessRule("uni.distrito.notfound", "Não existe 'Distrito Sanitário'.");
        if (tipoUnidadeCodigo != null && tipoUnidadeCodigo != 0 && !repo.tipoUnidadeExists(tipoUnidadeCodigo))
            throw new BusinessRule("uni.tipuni.notfound", "Não existe 'Tipo de Unidade'.");
        // R13–R15: professional CNS checks deferred until SAU_PRO is migrated
    }

    /** R16: check all 14 delete-guard tables before removing the record. */
    private void guardDeleteConstraints(Integer codigo) {
        if (repo.isReferencedByUnisetor(codigo))
            throw new Conflict("Unidade está vinculada a Setores e não pode ser excluída");
        if (repo.isReferencedByRecesp(codigo))
            throw new Conflict("Unidade está vinculada a Receituário de Controle Especial e não pode ser excluída");
        if (repo.isReferencedByProEsp1(codigo))
            throw new Conflict("Unidade está vinculada a Profissional×Especialidade e não pode ser excluída");
        if (repo.isReferencedByPar5Sal(codigo))
            throw new Conflict("Unidade está vinculada como unidade de saída em Parâmetros e não pode ser excluída");
        if (repo.isReferencedByPar5Sol(codigo))
            throw new Conflict("Unidade está vinculada como unidade solicitante em Parâmetros e não pode ser excluída");
        if (repo.isReferencedByUsuUni(codigo))
            throw new Conflict("Unidade está vinculada a Usuários e não pode ser excluída");
        if (repo.isReferencedByUsu(codigo))
            throw new Conflict("Unidade está configurada como unidade padrão de Usuário e não pode ser excluída");
        if (repo.isReferencedByRem1(codigo))
            throw new Conflict("Unidade está vinculada a Remessas e não pode ser excluída");
        if (repo.isReferencedByRemUnisetor(codigo))
            throw new Conflict("Unidade está vinculada a Remessa×Setor e não pode ser excluída");
        if (repo.isReferencedByPacAlt(codigo))
            throw new Conflict("Unidade está vinculada a Pacientes (alteração cadastral) e não pode ser excluída");
        if (repo.isReferencedByPacIns(codigo))
            throw new Conflict("Unidade está vinculada a Pacientes (inclusão cadastral) e não pode ser excluída");
        if (repo.isReferencedByPac(codigo))
            throw new Conflict("Unidade está vinculada a Pacientes e não pode ser excluída");
        if (repo.isReferencedByPar2Des(codigo))
            throw new Conflict("Unidade está vinculada como destino em Parâmetros de Agendamento e não pode ser excluída");
        if (repo.isReferencedByPar2Agend(codigo))
            throw new Conflict("Unidade está vinculada em Parâmetros de Agendamento e não pode ser excluída");
    }

    private Unidade buildFrom(UnidadeCreateRequest req) {
        Unidade u = new Unidade();
        applyFields(u, req.nome(), req.razaoSocial(), req.cnpj(), req.cep(),
                req.endereco(), req.enderecoNumero(), req.enderecoComplemento(),
                req.bairro(), req.telefone(), req.fax(), req.licencaFuncionamento(),
                req.responsavel(), req.email(), req.cnes(), req.bpa(), req.sipni(),
                req.orgaoEmissor(), req.estrategiaFamiliar(), req.psf(),
                req.sisPreNatal(), req.hiperdia(), req.gestao(), req.sia(),
                req.sigla(), req.situacao(), req.siaSus(), req.scnesId(),
                req.exportarEsus(), req.exportarBnafar(), req.cadastroCns(),
                req.cadastroEndereco(), req.atendimentoSemCns(), req.atendimentoSemEndereco(),
                req.encaminhamentoFisioterapia(), req.externo(), req.forPesCod(),
                req.tipoUnidadeCodigo(), req.atencaoSecundaria(), req.bloqueioPacSemCadInd(),
                req.avisoVacinaAtrasada(), req.cadastroCpf(), req.painel(),
                req.recepcaoIntermedMpp(), req.recepcaoIntermedMppImp(),
                req.baixaRemSemCns(), req.bloqueioLancPcdAut(), req.bloqueioDispPacExt(),
                req.bloqueioAgendSolExaPacExt(), req.municipioCodigo(),
                req.respProfissionalCodigo(), req.diretorCodigo(), req.auditorCodigo(),
                req.autorizadorCodigo(), req.distritoCodigo());
        // R14: gestao defaults to 1 (MUNICIPAL) when not provided
        if (u.getGestao() == null || u.getGestao() == 0) u.setGestao((short) 1);
        // R15: situacao defaults to 1 (ATIVADO) when not provided
        if (u.getSituacao() == null || u.getSituacao() == 0) u.setSituacao((short) 1);
        return u;
    }

    private static void applyFields(Unidade u, String nome, String razaoSocial, String cnpj,
                                    String cep, String endereco, String enderecoNumero,
                                    String enderecoComplemento, String bairro, String telefone,
                                    String fax, String licencaFuncionamento, String responsavel,
                                    String email, Integer cnes, Short bpa, Short sipni,
                                    String orgaoEmissor, Short estrategiaFamiliar, Short psf,
                                    Short sisPreNatal, Short hiperdia, Short gestao,
                                    String sia, String sigla, Short situacao, String siaSus,
                                    String scnesId, Boolean exportarEsus, Boolean exportarBnafar,
                                    Boolean cadastroCns, Boolean cadastroEndereco,
                                    Boolean atendimentoSemCns, Boolean atendimentoSemEndereco,
                                    Boolean encaminhamentoFisioterapia, Boolean externo,
                                    Long forPesCod, Integer tipoUnidadeCodigo,
                                    Boolean atencaoSecundaria, Boolean bloqueioPacSemCadInd,
                                    Boolean avisoVacinaAtrasada, Boolean cadastroCpf,
                                    Boolean painel, Boolean recepcaoIntermedMpp,
                                    Boolean recepcaoIntermedMppImp, Boolean baixaRemSemCns,
                                    Boolean bloqueioLancPcdAut, Boolean bloqueioDispPacExt,
                                    Boolean bloqueioAgendSolExaPacExt, Integer municipioCodigo,
                                    Long respProfissionalCodigo, Long diretorCodigo,
                                    Long auditorCodigo, Long autorizadorCodigo, Short distritoCodigo) {
        u.setNome(upperTrim(nome));
        u.setRazaoSocial(trim(razaoSocial));
        u.setCnpj(trim(cnpj));
        u.setCep(trim(cep));
        u.setEndereco(trim(endereco));
        u.setEnderecoNumero(trim(enderecoNumero));
        u.setEnderecoComplemento(trim(enderecoComplemento));
        u.setBairro(trim(bairro));
        u.setTelefone(trim(telefone));
        u.setFax(trim(fax));
        u.setLicencaFuncionamento(trim(licencaFuncionamento));
        u.setResponsavel(trim(responsavel));
        u.setEmail(trim(email));
        u.setCnes(cnes);
        u.setBpa(bpa);
        u.setSipni(sipni);
        u.setOrgaoEmissor(trim(orgaoEmissor));
        u.setEstrategiaFamiliar(estrategiaFamiliar);
        u.setPsf(psf);
        u.setSisPreNatal(sisPreNatal);
        u.setHiperdia(hiperdia);
        u.setGestao(gestao);
        u.setSia(trim(sia));
        u.setSigla(trim(sigla));
        u.setSituacao(situacao);
        u.setSiaSus(trim(siaSus));
        u.setScnesId(trim(scnesId));
        u.setExportarEsus(exportarEsus);
        u.setExportarBnafar(exportarBnafar);
        u.setCadastroCns(cadastroCns);
        u.setCadastroEndereco(cadastroEndereco);
        u.setAtendimentoSemCns(atendimentoSemCns);
        u.setAtendimentoSemEndereco(atendimentoSemEndereco);
        u.setEncaminhamentoFisioterapia(encaminhamentoFisioterapia);
        u.setExterno(externo);
        u.setForPesCod(externo != null && externo ? forPesCod : null); // R: ForPesCod only when externo
        u.setTipoUnidadeCodigo(tipoUnidadeCodigo);
        u.setAtencaoSecundaria(atencaoSecundaria);
        u.setBloqueioPacSemCadInd(bloqueioPacSemCadInd);
        u.setAvisoVacinaAtrasada(avisoVacinaAtrasada);
        u.setCadastroCpf(cadastroCpf);
        u.setPainel(painel);
        u.setRecepcaoIntermedMpp(recepcaoIntermedMpp);
        u.setRecepcaoIntermedMppImp(recepcaoIntermedMppImp);
        u.setBaixaRemSemCns(baixaRemSemCns);
        u.setBloqueioLancPcdAut(bloqueioLancPcdAut);
        u.setBloqueioDispPacExt(bloqueioDispPacExt);
        u.setBloqueioAgendSolExaPacExt(bloqueioAgendSolExaPacExt);
        u.setMunicipioCodigo(municipioCodigo);
        u.setRespProfissionalCodigo(respProfissionalCodigo);
        u.setDiretorCodigo(diretorCodigo);
        u.setAuditorCodigo(auditorCodigo);
        u.setAutorizadorCodigo(autorizadorCodigo);
        u.setDistritoCodigo(distritoCodigo);
    }

    // R8/R9: phone format — ^(\([0-9]{2}\))\s([9]{1})?([0-9]{4})-([0-9]{4})$ (from sau_uni_impl.java)
    private static final String PHONE_REGEX = "^(\\([0-9]{2}\\))\\s([9]{1})?([0-9]{4})-([0-9]{4})$";

    private static void validatePhone(String code, String msg, String value) {
        if (value != null && !value.isBlank() && !value.matches(PHONE_REGEX)) {
            throw new BusinessRule(code, msg);
        }
    }

    private static boolean isFlag1(Short flag) {
        return flag != null && flag == 1;
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String upperTrim(String s) {
        String t = trim(s);
        return t == null ? null : t.toUpperCase();
    }

    /** R2: CNPJ validation algorithm (14 digits, Luhn-like weights). */
    public static boolean isValidCnpj(String cnpj) {
        if (cnpj == null) return false;
        String digits = cnpj.replaceAll("[^0-9]", "");
        if (digits.length() != 14) return false;
        if (digits.chars().distinct().count() == 1) return false;

        int[] w1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < 12; i++) sum += (digits.charAt(i) - '0') * w1[i];
        int rem = sum % 11;
        char d1 = (rem < 2) ? '0' : (char) ('0' + (11 - rem));
        if (digits.charAt(12) != d1) return false;

        int[] w2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        sum = 0;
        for (int i = 0; i < 13; i++) sum += (digits.charAt(i) - '0') * w2[i];
        rem = sum % 11;
        char d2 = (rem < 2) ? '0' : (char) ('0' + (11 - rem));
        return digits.charAt(13) == d2;
    }
}
