package br.gov.mandaguari.saude.formaapresentacao.service;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.formaapresentacao.domain.FormaApresentacao;
import br.gov.mandaguari.saude.formaapresentacao.dto.FormaApresentacaoDtos.*;
import br.gov.mandaguari.saude.formaapresentacao.mapper.FormaApresentacaoMapper;
import br.gov.mandaguari.saude.formaapresentacao.repository.FormaApresentacaoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Forma de Apresentação (SAU_APRREM) business logic — rules mined from {@code sau_aprrem_impl.java}.
 * Leaf lookup: code + descrição + abreviação. Each rule carries a {@code // R<n>} citation.
 */
@Service
@Transactional(readOnly = true)
public class FormaApresentacaoService {

    private final FormaApresentacaoRepository repo;
    private final FormaApresentacaoMapper mapper;
    private final AuditService audit;

    public FormaApresentacaoService(FormaApresentacaoRepository repo, FormaApresentacaoMapper mapper,
                                    AuditService audit) {
        this.repo = repo;
        this.mapper = mapper;
        this.audit = audit;
    }

    public Page<FormaApresentacaoResponse> list(String descricao, Pageable pageable) {
        Page<FormaApresentacao> page = (descricao == null || descricao.isBlank())
                ? repo.findAll(pageable)
                : repo.findByDescricaoContainingIgnoreCase(descricao, pageable);
        return page.map(mapper::toResponse);
    }

    public FormaApresentacaoResponse get(Integer id) {
        return mapper.toResponse(find(id));
    }

    public List<FormaApresentacaoLookupItem> lookup(String q, Pageable pageable) {
        return repo.lookup(q == null ? "" : q, pageable).stream().map(mapper::toLookupItem).toList();
    }

    @Transactional
    public FormaApresentacaoResponse create(FormaApresentacaoCreateRequest req) {
        FormaApresentacao f = new FormaApresentacao();
        f.setDescricao(upperTrim(req.descricao()));   // R4
        f.setAbreviacao(upperTrim(req.abreviacao()));  // R5
        validate(f);                                   // R2, R3

        Integer codigo = repo.nextCodigo();            // R1
        validateCodigo(codigo);                        // R6
        f.setId(codigo);
        if (repo.existsById(codigo))                   // R8 (defensive)
            throw new Conflict("Forma de Apresentação " + codigo + " já existe");

        FormaApresentacao saved = repo.save(f);
        audit.record("CREATE", "SAU_APRREM", saved.getId());   // R11
        return mapper.toResponse(saved);
    }

    @Transactional
    public FormaApresentacaoResponse update(Integer id, FormaApresentacaoUpdateRequest req) {
        FormaApresentacao f = find(id);                // R9
        f.setDescricao(upperTrim(req.descricao()));    // R4
        f.setAbreviacao(upperTrim(req.abreviacao()));  // R5
        validate(f);                                   // R2, R3

        FormaApresentacao saved = repo.save(f);
        audit.record("UPDATE", "SAU_APRREM", saved.getId());   // R11
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(Integer id) {
        FormaApresentacao f = find(id);
        if (repo.isReferencedByMedicamento(id))        // R7
            throw new Conflict("Forma de Apresentação vinculada a Medicamento e não pode ser excluída");
        repo.delete(f);
        audit.record("DELETE", "SAU_APRREM", id);      // R11
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private FormaApresentacao find(Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFound("Forma de Apresentação " + id + " não encontrada"));
    }

    private static void validate(FormaApresentacao f) {
        // R2
        if (f.getDescricao() == null || f.getDescricao().isBlank())
            throw new BusinessRule("aprrem.descricao.required", "Informe a Descrição da Forma de Apresentação!");
        // R3
        if (f.getAbreviacao() == null || f.getAbreviacao().isBlank())
            throw new BusinessRule("aprrem.abreviacao.required", "Informe a Abreviação!");
    }

    /** R6: AprRemCod must fit the GeneXus 6-digit numeric domain (0..999999). */
    private static void validateCodigo(Integer codigo) {
        if (codigo == null || codigo < 0 || codigo > 999999)
            throw new BusinessRule("aprrem.codigo.range", "Código da Forma de Apresentação fora da faixa permitida (0-999999)");
    }

    private static String upperTrim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t.toUpperCase();
    }
}
