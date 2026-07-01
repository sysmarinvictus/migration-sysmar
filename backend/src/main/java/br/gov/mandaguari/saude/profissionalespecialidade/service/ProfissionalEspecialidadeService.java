package br.gov.mandaguari.saude.profissionalespecialidade.service;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.profissionalespecialidade.domain.ProfissionalEspecialidade;
import br.gov.mandaguari.saude.profissionalespecialidade.domain.ProfissionalEspecialidadeId;
import br.gov.mandaguari.saude.profissionalespecialidade.dto.ProfissionalEspecialidadeDtos.*;
import br.gov.mandaguari.saude.profissionalespecialidade.repository.ProfissionalEspecialidadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SAU_PROESP business logic — a professional's specialties. Rules mined from {@code sau_proesp_impl.java}
 * (see SLICE-SPEC SAU_PROESP), cited {@code // R<n>}. Every mutation is audited (common/audit, R8).
 */
@Service
@Transactional(readOnly = true)
public class ProfissionalEspecialidadeService {

    private static final short ATIVO = 1;   // R3
    private static final short SIM = 1;      // R4
    private static final short NAO = 0;      // R4

    private final ProfissionalEspecialidadeRepository repo;
    private final AuditService audit;

    public ProfissionalEspecialidadeService(ProfissionalEspecialidadeRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    /** All specialties of one professional (R1: professional must exist). */
    public List<EspecialidadeDoProfissionalResponse> list(Long profissionalId) {
        requireProfissional(profissionalId);
        return repo.findByProfissionalIdOrderByEspecialidadeId(profissionalId).stream()
                .map(ProfissionalEspecialidadeService::toResponse)
                .toList();
    }

    @Transactional
    public EspecialidadeDoProfissionalResponse add(Long profissionalId, EspecialidadeCreateRequest req) {
        requireProfissional(profissionalId);                                  // R1
        Integer espCod = req.especialidadeId();
        if (espCod == null || espCod == 0) {                                  // R2
            throw new BusinessRule("proesp.especialidade.required", "Informe o Código da Especialidade!");
        }
        if (!repo.especialidadeExists(espCod)) {                              // R2
            throw new BusinessRule("proesp.especialidade.notfound",
                    "Especialidade " + espCod + " não encontrada");
        }
        ProfissionalEspecialidadeId id = new ProfissionalEspecialidadeId(profissionalId, espCod);
        if (repo.existsById(id)) {                                            // R7 (composite unique)
            throw new Conflict("Profissional já possui a especialidade " + espCod);
        }

        ProfissionalEspecialidade pe = new ProfissionalEspecialidade();
        pe.setProfissionalId(profissionalId);
        pe.setEspecialidadeId(espCod);
        pe.setSituacao(ATIVO);                                               // R3
        pe.setPrioritario(Boolean.TRUE.equals(req.prioritario()) ? SIM : NAO); // R4
        pe.setAgendaManhaQtd(req.agendaManhaQtd());                          // R6
        pe.setAgendaTardeQtd(req.agendaTardeQtd());                         // R6
        pe.setAgendaNoiteQtd(req.agendaNoiteQtd());                         // R6

        ProfissionalEspecialidade saved = repo.save(pe);
        audit.record("CREATE", "SAU_PROESP", key(profissionalId, espCod));   // R8
        return toResponse(saved);
    }

    @Transactional
    public EspecialidadeDoProfissionalResponse update(Long profissionalId, Integer espCod,
                                                      EspecialidadeUpdateRequest req) {
        ProfissionalEspecialidade pe = find(profissionalId, espCod);
        if (req.prioritario() != null) {                                     // R4
            pe.setPrioritario(req.prioritario() ? SIM : NAO);
        }
        if (req.situacao() != null) {                                        // R3 (toggle active/inactive)
            pe.setSituacao(req.situacao());
        }
        if (req.agendaManhaQtd() != null) pe.setAgendaManhaQtd(req.agendaManhaQtd());
        if (req.agendaTardeQtd() != null) pe.setAgendaTardeQtd(req.agendaTardeQtd());
        if (req.agendaNoiteQtd() != null) pe.setAgendaNoiteQtd(req.agendaNoiteQtd());

        ProfissionalEspecialidade saved = repo.save(pe);
        audit.record("UPDATE", "SAU_PROESP", key(profissionalId, espCod));   // R8
        return toResponse(saved);
    }

    @Transactional
    public void remove(Long profissionalId, Integer espCod) {
        ProfissionalEspecialidade pe = find(profissionalId, espCod);
        // R5 delete-guard: an Impedimento for this pair blocks removal (impediments do NOT block prescriptions).
        if (repo.impedimentoExists(profissionalId, espCod)) {
            throw new Conflict("Especialidade não pode ser excluída: há Impedimento vinculado ao profissional");
        }
        repo.delete(pe);
        audit.record("DELETE", "SAU_PROESP", key(profissionalId, espCod));   // R8
    }

    // --- helpers ---

    private void requireProfissional(Long profissionalId) {
        if (profissionalId == null || profissionalId == 0L) {                // R1
            throw new BusinessRule("proesp.profissional.required", "Informe o Código do Profissional!");
        }
        if (!repo.profissionalExists(profissionalId)) {                      // R1
            throw new NotFound("Profissional " + profissionalId + " não encontrado");
        }
    }

    private ProfissionalEspecialidade find(Long profissionalId, Integer espCod) {
        return repo.findById(new ProfissionalEspecialidadeId(profissionalId, espCod))
                .orElseThrow(() -> new NotFound(
                        "Profissional " + profissionalId + " não possui a especialidade " + espCod));
    }

    private static String key(Long profissionalId, Integer espCod) {
        return profissionalId + "/" + espCod;
    }

    private static EspecialidadeDoProfissionalResponse toResponse(ProfissionalEspecialidade pe) {
        return new EspecialidadeDoProfissionalResponse(
                pe.getProfissionalId(), pe.getEspecialidadeId(), pe.isPrioritario(),
                pe.getSituacao(), pe.getAgendaManhaQtd(), pe.getAgendaTardeQtd(), pe.getAgendaNoiteQtd());
    }
}
