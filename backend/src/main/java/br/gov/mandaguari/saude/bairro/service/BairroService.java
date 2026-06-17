package br.gov.mandaguari.saude.bairro.service;

import br.gov.mandaguari.saude.bairro.domain.Bairro;
import br.gov.mandaguari.saude.bairro.dto.BairroDtos.*;
import br.gov.mandaguari.saude.bairro.mapper.BairroMapper;
import br.gov.mandaguari.saude.bairro.repository.BairroRepository;
import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Bairro business logic — rules mined from {@code sau_bai_impl.java} (see SLICE-SPEC SAU_BAI).
 * Each rule carries a {@code // R<n>} citation.
 */
@Service
@Transactional(readOnly = true)
public class BairroService {

    private final BairroRepository repo;
    private final BairroMapper mapper;
    private final AuditService audit;

    public BairroService(BairroRepository repo, BairroMapper mapper, AuditService audit) {
        this.repo = repo;
        this.mapper = mapper;
        this.audit = audit;
    }

    public Page<BairroResponse> list(String nome, Pageable pageable) {
        Page<Bairro> page = (nome == null || nome.isBlank())
                ? repo.findAll(pageable)
                : repo.findByNomeContainingIgnoreCase(nome, pageable);
        return page.map(mapper::toResponse);
    }

    public BairroResponse get(Integer codigo) {
        return mapper.toResponse(find(codigo));
    }

    public List<BairroLookupItem> lookup(String q, Pageable pageable) {
        return repo.lookup(q == null ? "" : q, pageable).stream().map(mapper::toLookupItem).toList();
    }

    @Transactional
    public BairroResponse create(BairroCreateRequest req) {
        String nome = req.nome().trim();
        requireNome(nome);                                            // R2
        if (repo.existsByNomeIgnoreCase(nome)) {                     // R3
            throw new Conflict("Bairro com este nome já está cadastrado");
        }

        Integer nextCodigo = repo.findMaxCodigo() + 1;               // R1: system-assigned MAX+1

        Bairro b = new Bairro();
        b.setCodigo(nextCodigo);
        b.setNome(nome);

        Bairro saved = repo.save(b);
        audit.record("CREATE", "SAU_BAI", saved.getCodigo());        // R6
        return mapper.toResponse(saved);
    }

    @Transactional
    public BairroResponse update(Integer codigo, BairroUpdateRequest req) {
        Bairro b = find(codigo);                                     // R1: codigo immutable (path id wins)
        String nome = req.nome().trim();
        requireNome(nome);                                            // R2
        if (repo.existsByNomeIgnoreCaseAndCodigoNot(nome, codigo)) { // R3
            throw new Conflict("Bairro com este nome já está cadastrado");
        }

        b.setNome(nome);

        Bairro saved = repo.save(b);
        audit.record("UPDATE", "SAU_BAI", saved.getCodigo());        // R6
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(Integer codigo) {
        Bairro b = find(codigo);
        if (repo.isReferencedByPessoa(codigo)) {                     // R4: block when used as person address
            throw new Conflict("Bairro está vinculado a uma pessoa e não pode ser excluído");
        }
        if (repo.isReferencedByDistrito(codigo)) {                   // R5: block when used in a district
            throw new Conflict("Bairro está vinculado a um distrito sanitário e não pode ser excluído");
        }
        repo.delete(b);
        audit.record("DELETE", "SAU_BAI", codigo);                   // R6
    }

    // --- helpers ---

    private Bairro find(Integer codigo) {
        return repo.findById(codigo)
                .orElseThrow(() -> new NotFound("Bairro " + codigo + " não encontrado"));
    }

    /** R2: nome is required by the transaction (DB column is nullable). */
    private static void requireNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new BusinessRule("bai.nome.required", "Informe o nome do bairro");
        }
    }
}
