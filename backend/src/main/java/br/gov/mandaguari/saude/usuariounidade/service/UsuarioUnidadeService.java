package br.gov.mandaguari.saude.usuariounidade.service;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.usuariounidade.domain.UsuarioUnidade;
import br.gov.mandaguari.saude.usuariounidade.domain.UsuarioUnidadeId;
import br.gov.mandaguari.saude.usuariounidade.dto.UsuarioUnidadeDtos.*;
import br.gov.mandaguari.saude.usuariounidade.repository.UsuarioUnidadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SAU_USUUNI (Usuário × Unidade) capability-matrix logic. Rules mined from {@code sau_usuuni_impl.java}
 * (see SLICE-SPEC), cited {@code // R<n>}. Minimal validation surface: FK-existence (R1-R3), composite-PK
 * uniqueness (R4), all-flags-default-false on insert (R5), unconditional delete (R6), audit (R10).
 */
@Service
@Transactional(readOnly = true)
public class UsuarioUnidadeService {

    private final UsuarioUnidadeRepository repo;
    private final AuditService audit;

    public UsuarioUnidadeService(UsuarioUnidadeRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    /** List the capability rows of one user (R1: user must exist). */
    public List<UsuarioUnidadeResponse> list(Integer usuCod) {
        requireUsuario(usuCod);
        return repo.findByUsuCodOrderByUniCod(usuCod).stream().map(UsuarioUnidadeService::toResponse).toList();
    }

    public UsuarioUnidadeResponse get(Integer usuCod, Integer uniCod) {
        return toResponse(find(usuCod, uniCod));
    }

    @Transactional
    public UsuarioUnidadeResponse create(Integer usuCod, Integer uniCod, UsuarioUnidadeUpsertRequest req) {
        requireUsuario(usuCod);                                    // R1
        if (uniCod == null || !repo.unidadeExists(uniCod)) {      // R2
            throw new BusinessRule("usuuni.unidade.notfound", "Unidade não existe");
        }
        validateEspecialidade(req.especialidadeCod());             // R3
        if (repo.existsById(new UsuarioUnidadeId(usuCod, uniCod))) { // R4
            throw new Conflict("Usuário já possui acesso à unidade " + uniCod);
        }
        UsuarioUnidade e = new UsuarioUnidade();
        e.setUsuCod(usuCod);
        e.setUniCod(uniCod);
        apply(e, req);                                             // R5: unset flags stay null (not blocked)
        UsuarioUnidade saved = repo.save(e);
        audit.record("CREATE", "SAU_USUUNI", usuCod + "/" + uniCod); // R10
        return toResponse(saved);
    }

    @Transactional
    public UsuarioUnidadeResponse update(Integer usuCod, Integer uniCod, UsuarioUnidadeUpsertRequest req) {
        UsuarioUnidade e = find(usuCod, uniCod);
        validateEspecialidade(req.especialidadeCod());             // R3
        apply(e, req);
        UsuarioUnidade saved = repo.save(e);
        audit.record("UPDATE", "SAU_USUUNI", usuCod + "/" + uniCod); // R10
        return toResponse(saved);
    }

    @Transactional
    public void delete(Integer usuCod, Integer uniCod) {
        UsuarioUnidade e = find(usuCod, uniCod);
        repo.delete(e);                                            // R6: unconditional
        audit.record("DELETE", "SAU_USUUNI", usuCod + "/" + uniCod); // R10
    }

    // --- helpers ---

    private void requireUsuario(Integer usuCod) {
        if (usuCod == null || !repo.usuarioExists(usuCod)) {      // R1
            throw new NotFound("Usuário " + usuCod + " não encontrado");
        }
    }

    private void validateEspecialidade(Integer espCod) {
        if (espCod != null && espCod != 0 && !repo.especialidadeExists(espCod)) { // R3
            throw new BusinessRule("usuuni.especialidade.notfound", "Não existe Especialidade do usuário");
        }
    }

    private UsuarioUnidade find(Integer usuCod, Integer uniCod) {
        return repo.findById(new UsuarioUnidadeId(usuCod, uniCod))
                .orElseThrow(() -> new NotFound("Acesso do usuário " + usuCod + " à unidade " + uniCod + " não encontrado"));
    }

    private static void apply(UsuarioUnidade e, UsuarioUnidadeUpsertRequest req) {
        e.setEspecialidadeCod(req.especialidadeCod());
        e.setBloqueioTabela(req.bloqueioTabela());
        e.setBloqueioCadastro(req.bloqueioCadastro());
        e.setBloqueioAmbulatorio(req.bloqueioAmbulatorio());
        e.setPermiteConsultaDireta(req.permiteConsultaDireta());
        e.setBloqueioProntuarioConsulta(req.bloqueioProntuarioConsulta());
        e.setBloqueioProntuarioOdonto(req.bloqueioProntuarioOdonto());
        e.setBloqueioResultadoExame(req.bloqueioResultadoExame());
        e.setBloqueioEsus(req.bloqueioEsus());
        e.setBloqueioCaps(req.bloqueioCaps());
        e.setBloqueioNutricao(req.bloqueioNutricao());
        e.setBloqueioFarmacia(req.bloqueioFarmacia());
        e.setPermiteBnafar(req.permiteBnafar());
        e.setBloqueioAlmoxarifado(req.bloqueioAlmoxarifado());
        e.setBloqueioRequisicao(req.bloqueioRequisicao());
        e.setBloqueioBeneficio(req.bloqueioBeneficio());
        e.setBloqueioTransporte(req.bloqueioTransporte());
        e.setBloqueioVacina(req.bloqueioVacina());
        e.setBloqueioAgenda(req.bloqueioAgenda());
        e.setBloqueioAgendaManual(req.bloqueioAgendaManual());
        e.setBloqueioAgendaExterna(req.bloqueioAgendaExterna());
        e.setBloqueioAgendaEspecial(req.bloqueioAgendaEspecial());
        e.setPermiteAgendaAuditor(req.permiteAgendaAuditor());
        e.setBloqueioLaboratorio(req.bloqueioLaboratorio());
        e.setBloqueioHospital(req.bloqueioHospital());
        e.setBloqueioVigilancia(req.bloqueioVigilancia());
        e.setBloqueioAgravo(req.bloqueioAgravo());
        e.setBloqueioCms(req.bloqueioCms());
        e.setBloqueioOuvidoria(req.bloqueioOuvidoria());
        e.setBloqueioImpressao(req.bloqueioImpressao());
        e.setBloqueioExportacao(req.bloqueioExportacao());
        e.setBloqueioParametro(req.bloqueioParametro());
        e.setBloqueioRelatorio(req.bloqueioRelatorio());
        e.setBloqueioRelatorioTabela(req.bloqueioRelatorioTabela());
        e.setBloqueioRelatorioCadastro(req.bloqueioRelatorioCadastro());
        e.setBloqueioRelatorioAmbulatorio(req.bloqueioRelatorioAmbulatorio());
        e.setBloqueioRelatorioEsus(req.bloqueioRelatorioEsus());
        e.setBloqueioRelatorioCaps(req.bloqueioRelatorioCaps());
        e.setBloqueioRelatorioNutricao(req.bloqueioRelatorioNutricao());
        e.setBloqueioRelatorioVacina(req.bloqueioRelatorioVacina());
        e.setBloqueioRelatorioFarmacia(req.bloqueioRelatorioFarmacia());
        e.setBloqueioRelatorioAlmoxarifado(req.bloqueioRelatorioAlmoxarifado());
        e.setBloqueioRelatorioRequisicao(req.bloqueioRelatorioRequisicao());
        e.setBloqueioRelatorioBeneficio(req.bloqueioRelatorioBeneficio());
        e.setBloqueioRelatorioTransporte(req.bloqueioRelatorioTransporte());
        e.setBloqueioRelatorioAgenda(req.bloqueioRelatorioAgenda());
        e.setBloqueioRelatorioLaboratorio(req.bloqueioRelatorioLaboratorio());
        e.setBloqueioRelatorioHospital(req.bloqueioRelatorioHospital());
        e.setBloqueioRelatorioVigilancia(req.bloqueioRelatorioVigilancia());
        e.setBloqueioRelatorioAgravo(req.bloqueioRelatorioAgravo());
        e.setBloqueioRelatorioOuvidoria(req.bloqueioRelatorioOuvidoria());
        e.setBloqueioRelatorioExportacao(req.bloqueioRelatorioExportacao());
        e.setBloqueioGrafico(req.bloqueioGrafico());
        e.setPermiteAgendaAuditorPcd(req.permiteAgendaAuditorPcd());
        e.setPermiteSoaBnafar(req.permiteSoaBnafar());
    }

    private static UsuarioUnidadeResponse toResponse(UsuarioUnidade e) {
        return new UsuarioUnidadeResponse(
                e.getUsuCod(), e.getUniCod(), e.getEspecialidadeCod(),
                e.getBloqueioTabela(), e.getBloqueioCadastro(), e.getBloqueioAmbulatorio(), e.getPermiteConsultaDireta(), e.getBloqueioProntuarioConsulta(), e.getBloqueioProntuarioOdonto(), e.getBloqueioResultadoExame(), e.getBloqueioEsus(), e.getBloqueioCaps(), e.getBloqueioNutricao(), e.getBloqueioFarmacia(), e.getPermiteBnafar(), e.getBloqueioAlmoxarifado(), e.getBloqueioRequisicao(), e.getBloqueioBeneficio(), e.getBloqueioTransporte(), e.getBloqueioVacina(), e.getBloqueioAgenda(), e.getBloqueioAgendaManual(), e.getBloqueioAgendaExterna(), e.getBloqueioAgendaEspecial(), e.getPermiteAgendaAuditor(), e.getBloqueioLaboratorio(), e.getBloqueioHospital(), e.getBloqueioVigilancia(), e.getBloqueioAgravo(), e.getBloqueioCms(), e.getBloqueioOuvidoria(), e.getBloqueioImpressao(), e.getBloqueioExportacao(), e.getBloqueioParametro(), e.getBloqueioRelatorio(), e.getBloqueioRelatorioTabela(), e.getBloqueioRelatorioCadastro(), e.getBloqueioRelatorioAmbulatorio(), e.getBloqueioRelatorioEsus(), e.getBloqueioRelatorioCaps(), e.getBloqueioRelatorioNutricao(), e.getBloqueioRelatorioVacina(), e.getBloqueioRelatorioFarmacia(), e.getBloqueioRelatorioAlmoxarifado(), e.getBloqueioRelatorioRequisicao(), e.getBloqueioRelatorioBeneficio(), e.getBloqueioRelatorioTransporte(), e.getBloqueioRelatorioAgenda(), e.getBloqueioRelatorioLaboratorio(), e.getBloqueioRelatorioHospital(), e.getBloqueioRelatorioVigilancia(), e.getBloqueioRelatorioAgravo(), e.getBloqueioRelatorioOuvidoria(), e.getBloqueioRelatorioExportacao(), e.getBloqueioGrafico(), e.getPermiteAgendaAuditorPcd(), e.getPermiteSoaBnafar());
    }
}
