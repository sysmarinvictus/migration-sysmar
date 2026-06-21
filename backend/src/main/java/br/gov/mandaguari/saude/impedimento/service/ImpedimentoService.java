package br.gov.mandaguari.saude.impedimento.service;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.especialidade.domain.Especialidade;
import br.gov.mandaguari.saude.especialidade.repository.EspecialidadeRepository;
import br.gov.mandaguari.saude.impedimento.domain.Impedimento;
import br.gov.mandaguari.saude.impedimento.dto.ImpedimentoDtos.*;
import br.gov.mandaguari.saude.impedimento.mapper.ImpedimentoMapper;
import br.gov.mandaguari.saude.impedimento.repository.ImpedimentoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Impedimento business logic — rules mined from {@code psau_imp_impl.java} (see SLICE-SPEC SAU_IMP).
 * Each rule is cited as {@code // R<n>}.
 */
@Service
@Transactional(readOnly = true)
public class ImpedimentoService {

    private final ImpedimentoRepository repo;
    private final EspecialidadeRepository especialidadeRepo;
    private final ImpedimentoMapper mapper;
    private final AuditService audit;

    public ImpedimentoService(ImpedimentoRepository repo, EspecialidadeRepository especialidadeRepo,
                               ImpedimentoMapper mapper, AuditService audit) {
        this.repo = repo;
        this.especialidadeRepo = especialidadeRepo;
        this.mapper = mapper;
        this.audit = audit;
    }

    public Page<ImpedimentoResponse> list(String profissionalNome, Long profissionalId,
                                           Integer especialidadeId, LocalDate dataInicioFrom,
                                           LocalDate dataFimAte, Pageable pageable) {
        Page<Impedimento> page = (profissionalNome != null && !profissionalNome.isBlank())
                ? repo.findByFiltersWithNome(profissionalNome, profissionalId, especialidadeId,
                        dataInicioFrom, dataFimAte, pageable)
                : repo.findByFilters(profissionalId, especialidadeId, dataInicioFrom, dataFimAte, pageable);
        return page.map(this::enrichResponse);
    }

    public ImpedimentoResponse get(Integer codigo) {
        return enrichResponse(find(codigo));
    }

    @Transactional
    public ImpedimentoResponse create(ImpedimentoCreateRequest req) {
        // R7: profissionalCodigo must be non-zero
        if (req.profissionalCodigo() == 0L) {
            throw new BusinessRule("imp.profissional.required", "Informe o código do Profissional!");
        }
        // R8: profissional must exist in SAU_PRO
        if (!repo.profissionalExists(req.profissionalCodigo())) {
            throw new BusinessRule("imp.profissional.notfound",
                    "Profissional " + req.profissionalCodigo() + " não encontrado");
        }
        // R9: especialidadeCodigo must be non-zero
        if (req.especialidadeCodigo() == 0) {
            throw new BusinessRule("imp.especialidade.required", "Informe o código da Especialidade!");
        }
        // R10: especialidade must exist
        Especialidade esp = especialidadeRepo.findById(req.especialidadeCodigo())
                .orElseThrow(() -> new BusinessRule("imp.especialidade.notfound",
                        "Especialidade " + req.especialidadeCodigo() + " não encontrada"));
        // R11: profissional+especialidade pair must exist in SAU_PROESP
        if (!repo.proEspExists(req.profissionalCodigo(), req.especialidadeCodigo())) {
            throw new BusinessRule("imp.proesp.notfound",
                    "Profissional não possui a especialidade informada");
        }

        int nextCodigo = repo.findMaxCodigo().map(max -> max + 1).orElse(1); // R4

        Impedimento imp = new Impedimento();
        imp.setCodigo(nextCodigo);
        imp.setDataCadastro(req.dataCadastro() != null ? req.dataCadastro() : LocalDate.now()); // R6
        imp.setDataInicio(req.dataInicio());  // R13
        imp.setDataFim(req.dataFim());        // R14
        imp.setProfissionalCodigo(req.profissionalCodigo());
        imp.setEspecialidade(esp);

        Impedimento saved = repo.save(imp);
        audit.record("CREATE", "SAU_IMP", saved.getCodigo()); // R22
        return enrichResponse(saved);
    }

    @Transactional
    public ImpedimentoResponse update(Integer codigo, ImpedimentoUpdateRequest req) {
        Impedimento imp = find(codigo);

        // R8
        if (!repo.profissionalExists(req.profissionalCodigo())) {
            throw new BusinessRule("imp.profissional.notfound",
                    "Profissional " + req.profissionalCodigo() + " não encontrado");
        }
        // R10
        Especialidade esp = especialidadeRepo.findById(req.especialidadeCodigo())
                .orElseThrow(() -> new BusinessRule("imp.especialidade.notfound",
                        "Especialidade " + req.especialidadeCodigo() + " não encontrada"));
        // R11
        if (!repo.proEspExists(req.profissionalCodigo(), req.especialidadeCodigo())) {
            throw new BusinessRule("imp.proesp.notfound",
                    "Profissional não possui a especialidade informada");
        }

        imp.setDataCadastro(req.dataCadastro());
        imp.setDataInicio(req.dataInicio());
        imp.setDataFim(req.dataFim());
        imp.setProfissionalCodigo(req.profissionalCodigo());
        imp.setEspecialidade(esp);

        Impedimento saved = repo.save(imp);
        audit.record("UPDATE", "SAU_IMP", saved.getCodigo()); // R22
        return enrichResponse(saved);
    }

    @Transactional
    public void delete(Integer codigo) {
        Impedimento imp = find(codigo);
        repo.delete(imp);
        audit.record("DELETE", "SAU_IMP", codigo); // R22
    }

    // --- helpers ---

    private Impedimento find(Integer codigo) {
        return repo.findById(codigo)
                .orElseThrow(() -> new NotFound("Impedimento " + codigo + " não encontrado"));
    }

    private ImpedimentoResponse enrichResponse(Impedimento imp) {
        ImpedimentoResponse base = mapper.toResponse(imp);
        String profNome = repo.findProfissionalNome(imp.getProfissionalCodigo()).orElse(null);
        Integer profSit = repo.findProfissionalSituacao(imp.getProfissionalCodigo()).orElse(null);
        Integer espCod = imp.getEspecialidade() != null ? imp.getEspecialidade().getCodigo() : null;
        String cboCode = (imp.getEspecialidade() != null) ? imp.getEspecialidade().getCborCodigo() : null;
        String cboDes = espCod != null ? repo.findCborDescricao(espCod).orElse(null) : null;
        return new ImpedimentoResponse(
                base.codigo(), base.dataCadastro(), base.dataInicio(), base.dataFim(),
                base.profissionalCodigo(), profNome, profSit,
                base.especialidadeCodigo(), base.especialidadeNome(),
                cboCode, cboDes);
    }
}
