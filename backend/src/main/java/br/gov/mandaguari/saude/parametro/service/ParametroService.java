package br.gov.mandaguari.saude.parametro.service;

import br.gov.mandaguari.saude.common.audit.AuditProperties;
import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.parametro.domain.Parametro;
import br.gov.mandaguari.saude.parametro.dto.ParametroDtos.*;
import br.gov.mandaguari.saude.parametro.repository.ParametroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SAU_PAR singleton config logic. The row is per-empresa (tenant); the empresa code is the app tenant
 * (reused from {@link AuditProperties#getEmpresaCodigo()}). Rules mined from {@code sau_par_ger_impl.java}
 * (see SLICE-SPEC), cited {@code // R<n>}. Only the two day-count fields carry validation; the rest of the
 * subset is plain config. Every update is audited. The ambulatorial procedure-code block is deferred.
 */
@Service
@Transactional(readOnly = true)
public class ParametroService {

    private static final int MAX_DIAS = 180; // R2

    private final ParametroRepository repo;
    private final AuditProperties tenant;
    private final AuditService audit;

    public ParametroService(ParametroRepository repo, AuditProperties tenant, AuditService audit) {
        this.repo = repo;
        this.tenant = tenant;
        this.audit = audit;
    }

    public ParametroResponse get() {
        return toResponse(load());
    }

    @Transactional
    public ParametroResponse updateGeral(ParametroGeralUpdateRequest req) {
        // R1/R2: user-inactivity and password-expiry day-counts are required (>0) and cannot exceed 180.
        validateDias(req.inatividadeUsuarioDias());
        validateDias(req.senhaUsuarioDias());

        Parametro p = load();
        p.setValidadeReceita(req.validadeReceita());
        p.setValidadeReceitaSimplesDias(req.validadeReceitaSimplesDias());
        p.setValidadeReceitaUsoContinuoDias(req.validadeReceitaUsoContinuoDias());
        p.setValidadeReceitaControleEspecialDias(req.validadeReceitaControleEspecialDias());
        p.setInatividadeUsuarioDias(req.inatividadeUsuarioDias());
        p.setSenhaUsuarioDias(req.senhaUsuarioDias());
        p.setSecretaria(req.secretaria());
        p.setSecretariaEndereco(req.secretariaEndereco());
        p.setSecretariaCep(req.secretariaCep());
        p.setSecretariaFone1(req.secretariaFone1());
        p.setSecretariaFone2(req.secretariaFone2());
        p.setSecretariaEmail(req.secretariaEmail());
        p.setCadastroSemCns(req.cadastroSemCns());
        p.setReciboComprador(req.reciboComprador());

        Parametro saved = repo.save(p);
        audit.record("UPDATE", "SAU_PAR_GER", saved.getEmpresaCod());
        return toResponse(saved);
    }

    @Transactional
    public ParametroResponse updateAmbulatorial(ParametroAmbulatorialUpdateRequest req) {
        Parametro p = load();
        p.setExigeCid10Atestado(req.exigeCid10Atestado());
        p.setEstornarAtendimento(req.estornarAtendimento());
        p.setImprimeRiscoMaterno(req.imprimeRiscoMaterno());
        p.setAtendimentoHistorico(req.atendimentoHistorico());

        Parametro saved = repo.save(p);
        audit.record("UPDATE", "SAU_PAR_AMB", saved.getEmpresaCod());
        return toResponse(saved);
    }

    // --- helpers ---

    /** R1: "Informe a quantidade de dias!" (required, >0); R2: "...não pode ser superior a 180 dias!". */
    private static void validateDias(Integer dias) {
        if (dias == null || dias == 0) {
            throw new BusinessRule("par.dias.required", "Informe a quantidade de dias!");
        }
        if (dias > MAX_DIAS) {
            throw new BusinessRule("par.dias.max", "Quantidade de dias não pode ser superior a 180 dias!");
        }
    }

    private Parametro load() {
        int empCod = tenant.getEmpresaCodigo();
        return repo.findById(empCod)
                .orElseThrow(() -> new NotFound("Parâmetros da empresa " + empCod + " não encontrados"));
    }

    private static ParametroResponse toResponse(Parametro p) {
        return new ParametroResponse(
                p.getEmpresaCod(), p.getValidadeReceita(), p.getValidadeReceitaSimplesDias(),
                p.getValidadeReceitaUsoContinuoDias(), p.getValidadeReceitaControleEspecialDias(),
                p.getInatividadeUsuarioDias(), p.getSenhaUsuarioDias(),
                p.getSecretaria(), p.getSecretariaEndereco(), p.getSecretariaCep(),
                p.getSecretariaFone1(), p.getSecretariaFone2(), p.getSecretariaEmail(),
                p.getCadastroSemCns(), p.getReciboComprador(),
                p.getExigeCid10Atestado(), p.getEstornarAtendimento(),
                p.getImprimeRiscoMaterno(), p.getAtendimentoHistorico());
    }
}
