package br.gov.mandaguari.saude.local.service;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.local.domain.Local;
import br.gov.mandaguari.saude.local.dto.LocalDtos.*;
import br.gov.mandaguari.saude.local.mapper.LocalMapper;
import br.gov.mandaguari.saude.local.repository.LocalRepository;
import br.gov.mandaguari.saude.local.repository.LocalRepository.MunicipioInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Local business logic — rules mined from {@code sau_loc_impl.java} (see SLICE-SPEC SAU_LOC).
 * Each rule carries a {@code // R<n>} citation. SYS_MUN is an un-migrated system table, so the
 * município FK is validated and its display fields derived via native queries (R4), mirroring the
 * SAU_ESP reference slice's SAU_CBOR handling.
 */
@Service
@Transactional(readOnly = true)
public class LocalService {

    private final LocalRepository repo;
    private final LocalMapper mapper;
    private final AuditService audit;

    public LocalService(LocalRepository repo, LocalMapper mapper, AuditService audit) {
        this.repo = repo;
        this.mapper = mapper;
        this.audit = audit;
    }

    public Page<LocalResponse> list(String nome, Pageable pageable) {
        Page<Local> page = (nome == null || nome.isBlank())
                ? repo.findAll(pageable)
                : repo.findByNomeContainingIgnoreCase(nome, pageable);
        return page.map(this::toResponseWithMunicipio);
    }

    public LocalResponse get(Integer codigo) {
        return toResponseWithMunicipio(find(codigo));
    }

    public List<LocalLookupItem> lookup(String q, Pageable pageable) {
        return repo.lookup(q == null ? "" : q, pageable).stream().map(mapper::toLookupItem).toList();
    }

    @Transactional
    public LocalResponse create(LocalCreateRequest req) {
        validateCodigo(req.codigo());                          // R1: range 0..999999
        if (repo.existsById(req.codigo())) {                   // R1: unique code on insert
            throw new Conflict("Já existe local com código " + req.codigo());
        }
        requireNome(req.nome());                               // R2
        requireMunicipio(req.municipioCodigo());               // R3
        validateMunicipioExists(req.municipioCodigo());        // R4

        Local l = new Local();
        l.setCodigo(req.codigo());
        l.setNome(req.nome().trim());
        l.setMunicipioCodigo(req.municipioCodigo());

        Local saved = repo.save(l);
        audit.record("CREATE", "SAU_LOC", saved.getCodigo());  // R6
        return toResponseWithMunicipio(saved);
    }

    @Transactional
    public LocalResponse update(Integer codigo, LocalUpdateRequest req) {
        Local l = find(codigo);                                // R1: codigo immutable (path id wins)
        requireNome(req.nome());                               // R2
        requireMunicipio(req.municipioCodigo());               // R3
        validateMunicipioExists(req.municipioCodigo());        // R4

        l.setNome(req.nome().trim());
        l.setMunicipioCodigo(req.municipioCodigo());

        Local saved = repo.save(l);
        audit.record("UPDATE", "SAU_LOC", saved.getCodigo());  // R6
        return toResponseWithMunicipio(saved);
    }

    @Transactional
    public void delete(Integer codigo) {
        Local l = find(codigo);
        repo.delete(l);                                        // R5: no delete guard — deletes freely
        audit.record("DELETE", "SAU_LOC", codigo);             // R6
    }

    // --- helpers ---

    private Local find(Integer codigo) {
        return repo.findById(codigo)
                .orElseThrow(() -> new NotFound("Local " + codigo + " não encontrado"));
    }

    private LocalResponse toResponseWithMunicipio(Local l) {
        LocalResponse base = mapper.toResponse(l);
        MunicipioInfo m = l.getMunicipioCodigo() == null ? null
                : repo.findMunicipio(l.getMunicipioCodigo()).orElse(null);   // R4: derive display
        return new LocalResponse(base.codigo(), base.nome(), base.municipioCodigo(),
                m == null ? null : m.getNome(), m == null ? null : m.getUf(),
                m == null ? null : m.getIbge());
    }

    /** R1: código is a GeneXus N(6,0) — present and in range 0..999999. */
    private static void validateCodigo(Integer codigo) {
        if (codigo == null || codigo < 0 || codigo > 999999) {
            throw new BusinessRule("loc.codigo.range", "O código deve estar entre 0 e 999999");
        }
    }

    /** R2: nome is required by the transaction (DB column is nullable). */
    private static void requireNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new BusinessRule("loc.nome.required", "Informe o nome do local");
        }
    }

    /** R3: município is required — GeneXus treats 0/absent as "not set". */
    private static void requireMunicipio(Integer municipioCodigo) {
        if (municipioCodigo == null || municipioCodigo == 0) {
            throw new BusinessRule("loc.municipio.required", "Informe o município");
        }
    }

    /** R4: município, when provided, must reference an existing SYS_MUN. */
    private void validateMunicipioExists(Integer municipioCodigo) {
        if (municipioCodigo != null && municipioCodigo != 0 && repo.findMunicipio(municipioCodigo).isEmpty()) {
            throw new BusinessRule("loc.municipio.unknown", "Município " + municipioCodigo + " não existe");
        }
    }
}
