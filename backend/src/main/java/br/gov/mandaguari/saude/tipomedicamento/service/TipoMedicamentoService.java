package br.gov.mandaguari.saude.tipomedicamento.service;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.tipomedicamento.domain.TipoMedicamento;
import br.gov.mandaguari.saude.tipomedicamento.dto.TipoMedicamentoDtos.*;
import br.gov.mandaguari.saude.tipomedicamento.mapper.TipoMedicamentoMapper;
import br.gov.mandaguari.saude.tipomedicamento.repository.TipoMedicamentoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Tipo de Medicamento business logic — rules mined from {@code sau_tiprem_impl.java} (see SLICE-SPEC
 * SAU_TIPREM). Each rule carries a {@code // R<n>} citation.
 */
@Service
@Transactional(readOnly = true)
public class TipoMedicamentoService {

    private final TipoMedicamentoRepository repo;
    private final TipoMedicamentoMapper mapper;
    private final AuditService audit;

    public TipoMedicamentoService(TipoMedicamentoRepository repo, TipoMedicamentoMapper mapper, AuditService audit) {
        this.repo = repo;
        this.mapper = mapper;
        this.audit = audit;
    }

    public Page<TipoMedicamentoResponse> list(String descricao, Pageable pageable) {
        Page<TipoMedicamento> page = (descricao == null || descricao.isBlank())
                ? repo.findAll(pageable)
                : repo.findByDescricaoContainingIgnoreCase(descricao, pageable);
        return page.map(mapper::toResponse);
    }

    public TipoMedicamentoResponse get(Integer codigo) {
        return mapper.toResponse(find(codigo));
    }

    public List<TipoMedicamentoLookupItem> lookup(String q, Pageable pageable) {
        return repo.lookup(q == null ? "" : q, pageable).stream().map(mapper::toLookupItem).toList();
    }

    @Transactional
    public TipoMedicamentoResponse create(TipoMedicamentoCreateRequest req) {
        validateCodigo(req.codigo());                          // R1: range 0..999999
        if (repo.existsById(req.codigo())) {                   // R1: unique code on insert
            throw new Conflict("Já existe tipo de medicamento com código " + req.codigo());
        }
        requireDescricao(req.descricao());                     // R2

        TipoMedicamento t = new TipoMedicamento();
        t.setCodigo(req.codigo());
        t.setDescricao(req.descricao().trim());

        TipoMedicamento saved = repo.save(t);
        audit.record("CREATE", "SAU_TIPREM", saved.getCodigo());   // R4
        return mapper.toResponse(saved);
    }

    @Transactional
    public TipoMedicamentoResponse update(Integer codigo, TipoMedicamentoUpdateRequest req) {
        TipoMedicamento t = find(codigo);                      // R1: codigo immutable (path id wins)
        requireDescricao(req.descricao());                     // R2

        t.setDescricao(req.descricao().trim());

        TipoMedicamento saved = repo.save(t);
        audit.record("UPDATE", "SAU_TIPREM", saved.getCodigo());   // R4
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(Integer codigo) {
        TipoMedicamento t = find(codigo);
        if (repo.isReferencedByMedicamento(codigo)) {          // R3: block delete when referenced
            throw new Conflict(
                    "Tipo de medicamento está vinculado a medicamentos e não pode ser excluído");
        }
        repo.delete(t);
        audit.record("DELETE", "SAU_TIPREM", codigo);          // R4
    }

    // --- helpers ---

    private TipoMedicamento find(Integer codigo) {
        return repo.findById(codigo)
                .orElseThrow(() -> new NotFound("Tipo de medicamento " + codigo + " não encontrado"));
    }

    /** R1: código is a GeneXus N(6,0) — present and in range 0..999999. */
    private static void validateCodigo(Integer codigo) {
        if (codigo == null || codigo < 0 || codigo > 999999) {
            throw new BusinessRule("tiprem.codigo.range", "O código deve estar entre 0 e 999999");
        }
    }

    /** R2: descricao is required by the transaction (DB column is nullable). */
    private static void requireDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            throw new BusinessRule("tiprem.descricao.required", "Informe a descrição do tipo de medicamento");
        }
    }
}
