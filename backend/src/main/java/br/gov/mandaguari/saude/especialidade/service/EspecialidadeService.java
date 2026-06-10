package br.gov.mandaguari.saude.especialidade.service;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.especialidade.domain.Especialidade;
import br.gov.mandaguari.saude.especialidade.dto.EspecialidadeDtos.*;
import br.gov.mandaguari.saude.especialidade.mapper.EspecialidadeMapper;
import br.gov.mandaguari.saude.especialidade.repository.EspecialidadeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Especialidade business logic — rules mined from {@code sau_esp_impl.java} (see SLICE-SPEC SAU_ESP).
 * Each rule is implemented with a {@code // R<n>} citation. Rules R4/R5 are medium/low confidence and
 * flagged for KB verification before this slice is marked {@code verified}.
 */
@Service
@Transactional(readOnly = true)
public class EspecialidadeService {

    private final EspecialidadeRepository repo;
    private final EspecialidadeMapper mapper;
    private final AuditService audit;

    public EspecialidadeService(EspecialidadeRepository repo, EspecialidadeMapper mapper, AuditService audit) {
        this.repo = repo;
        this.mapper = mapper;
        this.audit = audit;
    }

    public Page<EspecialidadeResponse> list(String nome, Pageable pageable) {
        Page<Especialidade> page = (nome == null || nome.isBlank())
                ? repo.findAll(pageable)
                : repo.findByNomeContainingIgnoreCase(nome, pageable);
        return page.map(this::toResponseWithCbor);
    }

    public EspecialidadeResponse get(Integer codigo) {
        return toResponseWithCbor(find(codigo));
    }

    public List<EspecialidadeLookupItem> lookup(String q, Pageable pageable) {
        return repo.lookup(q == null ? "" : q, pageable).stream().map(mapper::toLookupItem).toList();
    }

    @Transactional
    public EspecialidadeResponse create(EspecialidadeCreateRequest req) {
        requireNome(req.nome());                                   // R1
        if (repo.existsById(req.codigo())) {                       // R2: unique code on insert
            throw new Conflict("Já existe especialidade com código " + req.codigo());
        }
        validateAgenda(req.agenda());                              // R5
        validateCbor(req.cborCodigo());                            // R3

        Especialidade e = new Especialidade();
        e.setCodigo(req.codigo());
        e.setNome(req.nome().trim());
        e.setSituacao(req.situacao());
        e.setAuxiliar(req.auxiliar());
        e.setCborCodigo(req.cborCodigo());
        mapper.applyAgenda(e, req.agenda());

        Especialidade saved = repo.save(e);
        audit.record("CREATE", "SAU_ESP", saved.getCodigo());     // R6
        return toResponseWithCbor(saved);
    }

    @Transactional
    public EspecialidadeResponse update(Integer codigo, EspecialidadeUpdateRequest req) {
        Especialidade e = find(codigo);                            // R2: codigo immutable (path id wins)
        requireNome(req.nome());                                   // R1
        validateAgenda(req.agenda());                              // R5
        validateCbor(req.cborCodigo());                            // R3

        e.setNome(req.nome().trim());
        e.setSituacao(req.situacao());
        e.setAuxiliar(req.auxiliar());
        e.setCborCodigo(req.cborCodigo());
        mapper.applyAgenda(e, req.agenda());

        Especialidade saved = repo.save(e);
        audit.record("UPDATE", "SAU_ESP", saved.getCodigo());     // R6
        return toResponseWithCbor(saved);
    }

    @Transactional
    public void delete(Integer codigo) {
        Especialidade e = find(codigo);
        if (repo.isReferencedByProfissional(codigo)) {            // R4: block delete when referenced
            throw new Conflict("Especialidade está vinculada a profissionais e não pode ser excluída");
        }
        repo.delete(e);
        audit.record("DELETE", "SAU_ESP", codigo);                // R6
    }

    // --- helpers ---

    private Especialidade find(Integer codigo) {
        return repo.findById(codigo)
                .orElseThrow(() -> new NotFound("Especialidade " + codigo + " não encontrada"));
    }

    private EspecialidadeResponse toResponseWithCbor(Especialidade e) {
        EspecialidadeResponse base = mapper.toResponse(e);
        String cborDes = e.getCborCodigo() == null ? null
                : repo.findCborDescricao(e.getCborCodigo()).orElse(null);   // R3: derive description
        return new EspecialidadeResponse(base.codigo(), base.nome(), base.situacao(), base.auxiliar(),
                base.cborCodigo(), cborDes, base.agenda());
    }

    private static void requireNome(String nome) {                // R1
        if (nome == null || nome.isBlank()) {
            throw new BusinessRule("esp.nome.required", "O nome da especialidade é obrigatório");
        }
    }

    private void validateCbor(Integer cborCodigo) {               // R3
        if (cborCodigo != null && !repo.cborExists(cborCodigo)) {
            throw new BusinessRule("esp.cbor.unknown", "Ocupação (CBO) " + cborCodigo + " não existe");
        }
    }

    /** R5 (confidence: low — verify in KB): each vaga *Min must be ≤ its *Max. */
    private static void validateAgenda(AgendaParametros a) {
        if (a == null) return;
        checkMinMax(a.vagaMuitoUrgenteMin(), a.vagaMuitoUrgenteMax(), "muito urgente");
        checkMinMax(a.vagaUrgenteMin(), a.vagaUrgenteMax(), "urgente");
        checkMinMax(a.vagaPrioritarioMin(), a.vagaPrioritarioMax(), "prioritário");
        checkMinMax(a.vagaNormalMin(), a.vagaNormalMax(), "normal");
    }

    private static void checkMinMax(Integer min, Integer max, String tier) {
        if (min != null && max != null && min > max) {
            throw new BusinessRule("esp.agenda.minmax",
                    "Vaga mínima não pode ser maior que a máxima (" + tier + ")");
        }
    }
}
