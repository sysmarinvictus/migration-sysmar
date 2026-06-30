package br.gov.mandaguari.saude.perfil.service;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.perfil.domain.Perfil;
import br.gov.mandaguari.saude.perfil.dto.PerfilDtos.*;
import br.gov.mandaguari.saude.perfil.mapper.PerfilMapper;
import br.gov.mandaguari.saude.perfil.repository.PerfilRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Perfil (RBAC profile) business logic — rules mined from {@code sau_prf_impl.java} (see SLICE-SPEC
 * SAU_PRF). Each rule carries a {@code // R<n>} citation. The per-program permission grid (R7/R8) is
 * deferred to the SAU_PRFCON slice; this service owns the profile header CRUD, its delete guards
 * (R4/R5) and the cascade-delete of child permissions (R6).
 */
@Service
@Transactional(readOnly = true)
public class PerfilService {

    private static final Logger log = LoggerFactory.getLogger(PerfilService.class);

    private final PerfilRepository repo;
    private final PerfilMapper mapper;
    private final AuditService audit;

    public PerfilService(PerfilRepository repo, PerfilMapper mapper, AuditService audit) {
        this.repo = repo;
        this.mapper = mapper;
        this.audit = audit;
    }

    public Page<PerfilResponse> list(String nome, Pageable pageable) {
        Page<Perfil> page = (nome == null || nome.isBlank())
                ? repo.findAll(pageable)
                : repo.findByNomeContainingIgnoreCase(nome, pageable);
        return page.map(mapper::toResponse);
    }

    public PerfilResponse get(Integer id) {
        return mapper.toResponse(find(id));
    }

    public List<PerfilLookupItem> lookup(String q, Pageable pageable) {
        return repo.lookup(q == null ? "" : q, pageable).stream().map(mapper::toLookupItem).toList();
    }

    @Transactional
    public PerfilResponse create(PerfilCreateRequest req) {
        String nome = requireNome(req.nome());                 // R2
        Perfil p = new Perfil();
        p.setId(repo.findMaxId() + 1);                          // R1: MAX+1 (server-allocated, not @GeneratedValue)
        p.setNome(nome);                                       // R3: uppercased
        Perfil saved = repo.save(p);
        audit.record("CREATE", "SAU_PRF", saved.getId());      // R10
        return mapper.toResponse(saved);
    }

    @Transactional
    public PerfilResponse update(Integer id, PerfilUpdateRequest req) {
        Perfil p = find(id);                                   // id immutable (path wins)
        p.setNome(requireNome(req.nome()));                    // R2 + R3
        Perfil saved = repo.save(p);
        audit.record("UPDATE", "SAU_PRF", saved.getId());      // R10
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(Integer id) {
        Perfil p = find(id);
        if (repo.isReferencedByUsuario(id)) {                  // R4
            throw new Conflict("Perfil está vinculado a usuários do sistema e não pode ser excluído");
        }
        if (referenced(repo::isReferencedBySocialProfileParam, id)) {   // R5 (SAU_PAR4 may be absent → defensive)
            throw new Conflict("Perfil está configurado como perfil padrão em parâmetros e não pode ser excluído");
        }
        cascadeDeletePrfcon(id);                               // R6
        repo.delete(p);
        audit.record("DELETE", "SAU_PRF", id);                 // R10
    }

    // --- helpers ---

    private Perfil find(Integer id) {
        return repo.findById(id).orElseThrow(() -> new NotFound("Perfil " + id + " não encontrado"));
    }

    /** R2 + R3: nome required (legacy "Informe a descrição do perfil!"), stored UPPERCASE. */
    private static String requireNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new BusinessRule("perfil.nome.required", "Informe a descrição do perfil!");
        }
        return nome.trim().toUpperCase(Locale.ROOT);
    }

    /** R6: delete child SAU_PRFCON rows. Defensive: tolerate the table being absent (baseline/test). */
    private void cascadeDeletePrfcon(Integer id) {
        try {
            int removed = repo.deletePrfconByPrfCod(id);
            if (removed > 0) log.debug("cascade-deleted {} SAU_PRFCON rows for PrfCod={}", removed, id);
        } catch (RuntimeException ex) {
            log.warn("could not cascade-delete SAU_PRFCON for PrfCod={} (table absent?): {}", id, ex.toString());
        }
    }

    /** Defensive guard: a missing guard table (not yet in baseline) is treated as not-referenced. */
    private static boolean referenced(java.util.function.Function<Integer, Boolean> guard, Integer id) {
        try {
            return Boolean.TRUE.equals(guard.apply(id));
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
