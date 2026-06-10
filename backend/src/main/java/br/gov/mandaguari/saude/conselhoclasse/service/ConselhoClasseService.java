package br.gov.mandaguari.saude.conselhoclasse.service;

import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.conselhoclasse.domain.ConselhoClasse;
import br.gov.mandaguari.saude.conselhoclasse.dto.ConselhoClasseDtos.*;
import br.gov.mandaguari.saude.conselhoclasse.mapper.ConselhoClasseMapper;
import br.gov.mandaguari.saude.conselhoclasse.repository.ConselhoClasseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Conselho de Classe business logic — rules mined from {@code sau_concla_impl.java} (see SLICE-SPEC
 * SAU_CONCLA). Each rule carries a {@code // R<n>} citation.
 *
 * <p>No audit is emitted: this transaction writes no {@code SAU_LOG} and carries no PHI
 * (reference/lookup data) — see SLICE-SPEC {@code phi_fields: []}.
 */
@Service
@Transactional(readOnly = true)
public class ConselhoClasseService {

    private final ConselhoClasseRepository repo;
    private final ConselhoClasseMapper mapper;

    public ConselhoClasseService(ConselhoClasseRepository repo, ConselhoClasseMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public Page<ConselhoClasseResponse> list(String q, Pageable pageable) {
        Page<ConselhoClasse> page = (q == null || q.isBlank())
                ? repo.findAll(pageable)
                : repo.search(q, pageable);
        return page.map(mapper::toResponse);
    }

    public ConselhoClasseResponse get(Short codigo) {
        return mapper.toResponse(find(codigo));
    }

    public List<ConselhoClasseLookupItem> lookup(String q, Pageable pageable) {
        return repo.lookup(q == null ? "" : q, pageable).stream().map(mapper::toLookupItem).toList();
    }

    @Transactional
    public ConselhoClasseResponse create(ConselhoClasseCreateRequest req) {
        validateCodigo(req.codigo());                          // R1: range 0..999
        if (repo.existsById(req.codigo())) {                   // R2: unique code on insert
            throw new Conflict("Já existe conselho de classe com código " + req.codigo());
        }
        ConselhoClasse c = new ConselhoClasse();
        c.setCodigo(req.codigo());
        c.setSigla(trimToNull(req.sigla()));                   // R5: sigla/nome optional
        c.setNome(trimToNull(req.nome()));
        return mapper.toResponse(repo.save(c));
    }

    @Transactional
    public ConselhoClasseResponse update(Short codigo, ConselhoClasseUpdateRequest req) {
        ConselhoClasse c = find(codigo);                       // R2: codigo immutable (path id wins)
        c.setSigla(trimToNull(req.sigla()));
        c.setNome(trimToNull(req.nome()));
        return mapper.toResponse(repo.save(c));
    }

    @Transactional
    public void delete(Short codigo) {
        ConselhoClasse c = find(codigo);
        if (repo.isReferencedByProfissional(codigo)) {         // R3: block delete when referenced
            throw new Conflict(
                    "Conselho de classe está vinculado a profissionais e não pode ser excluído");
        }
        repo.delete(c);
    }

    // --- helpers ---

    private ConselhoClasse find(Short codigo) {
        return repo.findById(codigo)
                .orElseThrow(() -> new NotFound("Conselho de classe " + codigo + " não encontrado"));
    }

    /** R1: código is a GeneXus N(3,0) — must be present and in range 0..999. */
    private static void validateCodigo(Short codigo) {
        if (codigo == null || codigo < 0 || codigo > 999) {
            throw new BusinessRule("concla.codigo.range", "O código deve estar entre 0 e 999");
        }
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
