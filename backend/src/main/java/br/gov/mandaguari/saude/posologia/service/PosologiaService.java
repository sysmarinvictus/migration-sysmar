package br.gov.mandaguari.saude.posologia.service;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.posologia.domain.Posologia;
import br.gov.mandaguari.saude.posologia.dto.PosologiaDtos.*;
import br.gov.mandaguari.saude.posologia.mapper.PosologiaMapper;
import br.gov.mandaguari.saude.posologia.repository.PosologiaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Posologia business logic — rules mined from {@code sau_remobs_impl.java} (see SLICE-SPEC
 * SAU_REMOBS). Each rule carries a {@code // R<n>} citation.
 */
@Service
@Transactional(readOnly = true)
public class PosologiaService {

    private final PosologiaRepository repo;
    private final PosologiaMapper mapper;
    private final AuditService audit;

    public PosologiaService(PosologiaRepository repo, PosologiaMapper mapper, AuditService audit) {
        this.repo = repo;
        this.mapper = mapper;
        this.audit = audit;
    }

    public Page<PosologiaResponse> list(String descricao, Pageable pageable) {
        Page<Posologia> page = (descricao == null || descricao.isBlank())
                ? repo.findAll(pageable)
                : repo.findByDescricaoContainingIgnoreCase(descricao, pageable);
        return page.map(mapper::toResponse);
    }

    public PosologiaResponse get(Integer codigo) {
        return mapper.toResponse(find(codigo));
    }

    public List<PosologiaLookupItem> lookup(String q, Pageable pageable) {
        return repo.lookup(q == null ? "" : q, pageable).stream().map(mapper::toLookupItem).toList();
    }

    @Transactional
    public PosologiaResponse create(PosologiaCreateRequest req) {
        requireDescricao(req.descricao());                   // R2

        Integer nextCodigo = repo.findMaxCodigo() + 1;      // R1: system-assigned MAX+1

        Posologia p = new Posologia();
        p.setCodigo(nextCodigo);
        p.setDescricao(req.descricao().trim());
        p.setInternamento(req.internamento());
        p.setQuantidadeDose(req.quantidadeDose());
        p.setMedidaDose(req.medidaDose());
        p.setIntervaloHoras(req.intervaloHoras() != null ? req.intervaloHoras().shortValue() : null);
        p.setDuracaoDias(req.duracaoDias() != null ? req.duracaoDias().shortValue() : null);
        // R5: usuarioCodigo = current user integer code; null in Wave 1 (SAU_USU un-migrated)

        Posologia saved = repo.save(p);
        audit.record("CREATE", "SAU_REMOBS", saved.getCodigo()); // R6
        return mapper.toResponse(saved);
    }

    @Transactional
    public PosologiaResponse update(Integer codigo, PosologiaUpdateRequest req) {
        Posologia p = find(codigo);                          // R1: codigo immutable (path id wins)
        requireDescricao(req.descricao());                   // R2

        p.setDescricao(req.descricao().trim());
        p.setInternamento(req.internamento());
        p.setQuantidadeDose(req.quantidadeDose());
        p.setMedidaDose(req.medidaDose());
        p.setIntervaloHoras(req.intervaloHoras() != null ? req.intervaloHoras().shortValue() : null);
        p.setDuracaoDias(req.duracaoDias() != null ? req.duracaoDias().shortValue() : null);

        Posologia saved = repo.save(p);
        audit.record("UPDATE", "SAU_REMOBS", saved.getCodigo()); // R6
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(Integer codigo) {
        Posologia p = find(codigo);
        if (repo.isReferencedByRemposo(codigo)) {           // R3: block when used in medication-posology
            throw new Conflict("Posologia está vinculada a posologia de medicamento e não pode ser excluída");
        }
        if (repo.isReferencedByRecesp1(codigo)) {           // R4: block when used in controlled prescription
            throw new Conflict("Posologia está vinculada a itens do receituário controle especial e não pode ser excluída");
        }
        repo.delete(p);
        audit.record("DELETE", "SAU_REMOBS", codigo);       // R6
    }

    // --- helpers ---

    private Posologia find(Integer codigo) {
        return repo.findById(codigo)
                .orElseThrow(() -> new NotFound("Posologia " + codigo + " não encontrada"));
    }

    /** R2: descricao is required by the transaction (DB column is nullable). */
    private static void requireDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            throw new BusinessRule("remobs.descricao.required", "Informe a observação do medicamento");
        }
    }
}
