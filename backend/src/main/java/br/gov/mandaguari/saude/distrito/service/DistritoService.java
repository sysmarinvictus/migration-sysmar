package br.gov.mandaguari.saude.distrito.service;

import br.gov.mandaguari.saude.bairro.repository.BairroRepository;
import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.distrito.domain.Distrito;
import br.gov.mandaguari.saude.distrito.dto.DistritoDtos.*;
import br.gov.mandaguari.saude.distrito.mapper.DistritoMapper;
import br.gov.mandaguari.saude.distrito.repository.DistritoRepository;
import br.gov.mandaguari.saude.tipologradouro.repository.TipoLogradouroRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Distrito Sanitário business logic — rules mined from {@code sau_dis_impl.java}
 * (checkExtendedTable0214 + delete-guard cursor T000217). Each rule carries a {@code // R<n>} citation.
 */
@Service
@Transactional(readOnly = true)
public class DistritoService {

    private final DistritoRepository repo;
    private final DistritoMapper mapper;
    private final AuditService audit;
    private final TipoLogradouroRepository tiplogRepo;
    private final BairroRepository bairroRepo;

    public DistritoService(DistritoRepository repo, DistritoMapper mapper, AuditService audit,
                           TipoLogradouroRepository tiplogRepo, BairroRepository bairroRepo) {
        this.repo = repo;
        this.mapper = mapper;
        this.audit = audit;
        this.tiplogRepo = tiplogRepo;
        this.bairroRepo = bairroRepo;
    }

    public Page<DistritoResponse> list(String nome, Pageable pageable) {
        Page<Distrito> page = (nome == null || nome.isBlank())
                ? repo.findAll(pageable)
                : repo.findByNomeContainingIgnoreCase(nome, pageable);
        return page.map(this::enrich);
    }

    public DistritoResponse get(Short codigo) {
        return enrich(find(codigo));
    }

    public List<DistritoLookupItem> lookup(String q, Pageable pageable) {
        return repo.lookup(q == null ? "" : q, pageable).stream().map(mapper::toLookupItem).toList();
    }

    @Transactional
    public DistritoResponse create(DistritoCreateRequest req) {
        String nome = req.nome().trim().toUpperCase();                // R4: stored uppercase
        requireNome(nome);                                            // R2
        validateTipLog(req.tipoLogradouroCodigo());                  // R5
        validateBairro(req.bairroCodigo());                         // R8
        validateDdd(req.ddd());                                      // R11

        int next = repo.findMaxCodigo() + 1;
        if (next > Short.MAX_VALUE) {                                 // R13: SMALLINT overflow guard
            throw new BusinessRule("dis.codigo.overflow", "Capacidade máxima de códigos atingida");
        }
        Short nextCodigo = (short) next;                              // R13: system-assigned MAX+1

        Distrito d = buildFrom(req.endereco(), req.numero(), req.complemento(),
                req.cep(), req.ddd(), req.telefone(), req.fax(),
                req.tipoLogradouroCodigo(), req.bairroCodigo());
        d.setCodigo(nextCodigo);
        d.setNome(nome);

        Distrito saved = repo.save(d);
        audit.record("CREATE", "SAU_DIS", saved.getCodigo());        // R17
        return enrich(saved);
    }

    @Transactional
    public DistritoResponse update(Short codigo, DistritoUpdateRequest req) {
        Distrito d = find(codigo);
        String nome = req.nome().trim().toUpperCase();                // R4: stored uppercase
        requireNome(nome);                                            // R3
        validateTipLog(req.tipoLogradouroCodigo());                  // R6
        validateBairro(req.bairroCodigo());                         // R9
        validateDdd(req.ddd());                                      // R12

        d.setNome(nome);
        d.setEndereco(nullableUpperTrim(req.endereco()));             // R4
        d.setNumero(req.numero());
        d.setComplemento(nullableUpperTrim(req.complemento()));       // R4
        d.setCep(req.cep());
        d.setDdd(nullableTrim(req.ddd()));
        d.setTelefone(req.telefone());
        d.setFax(req.fax());
        d.setTipoLogradouroCodigo(req.tipoLogradouroCodigo());
        d.setBairroCodigo(req.bairroCodigo());

        Distrito saved = repo.save(d);
        audit.record("UPDATE", "SAU_DIS", saved.getCodigo());        // R17
        return enrich(saved);
    }

    @Transactional
    public void delete(Short codigo) {
        Distrito d = find(codigo);
        if (repo.isReferencedByUnidade(codigo)) {                   // R16
            throw new Conflict("Distrito está vinculado a uma Unidade de Atendimento e não pode ser excluído");
        }
        repo.delete(d);
        audit.record("DELETE", "SAU_DIS", codigo);                  // R17
    }

    // --- helpers ---

    private Distrito find(Short codigo) {
        return repo.findById(codigo)
                .orElseThrow(() -> new NotFound("Distrito " + codigo + " não encontrado"));
    }

    /** R1: DisNom is required — "Informe o Nome do Distrito". */
    private static void requireNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new BusinessRule("dis.nome.required", "Informe o Nome do Distrito");
        }
    }

    /** R2: DisTipLogCod is validated when non-null and non-zero. */
    private void validateTipLog(Integer tipLogCod) {
        if (tipLogCod != null && tipLogCod != 0 && !repo.tipLogExists(tipLogCod)) {
            throw new BusinessRule("dis.tiplog.notfound", "Tipo de logradouro não encontrado");
        }
    }

    /** R3: DisBaiCod is validated when non-null and non-zero. */
    private void validateBairro(Integer bairroCod) {
        if (bairroCod != null && bairroCod != 0 && !repo.bairroExists(bairroCod)) {
            throw new BusinessRule("dis.bairro.notfound", "Bairro não encontrado");
        }
    }

    /** R4: DisDDD must consist entirely of digits when provided. */
    private static void validateDdd(String ddd) {
        if (ddd != null && !ddd.isBlank() && !ddd.matches("\\d+")) {
            throw new BusinessRule("dis.ddd.numerico", "Informe apenas Números!");
        }
    }

    private static String nullableTrim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** R4: uppercase + trim for address text fields. */
    private static String nullableUpperTrim(String s) {
        if (s == null) return null;
        String t = s.trim().toUpperCase();
        return t.isEmpty() ? null : t;
    }

    /** Builds entity from address + contact fields; applies R4 uppercase on endereco/complemento. */
    private static Distrito buildFrom(String endereco, Short numero, String complemento,
                                          Integer cep, String ddd, Integer telefone, Integer fax,
                                          Integer tipLogCod, Integer bairroCod) {
        Distrito d = new Distrito();
        d.setEndereco(nullableUpperTrim(endereco));                    // R4
        d.setNumero(numero);
        d.setComplemento(nullableUpperTrim(complemento));              // R4
        d.setCep(cep);
        d.setDdd(nullableTrim(ddd));
        d.setTelefone(telefone);
        d.setFax(fax);
        d.setTipoLogradouroCodigo(tipLogCod);
        d.setBairroCodigo(bairroCod);
        return d;
    }

    /** Enriches the mapped response with derived R7 (tiplogSigla) and R10 (bairroNome) fields. */
    private DistritoResponse enrich(Distrito d) {
        DistritoResponse base = mapper.toResponse(d);
        String tiplogSigla = (d.getTipoLogradouroCodigo() != null && d.getTipoLogradouroCodigo() > 0)
                ? tiplogRepo.findById(d.getTipoLogradouroCodigo()).map(t -> t.getSigla()).orElse(null)
                : null;
        String bairroNome = (d.getBairroCodigo() != null && d.getBairroCodigo() > 0)
                ? bairroRepo.findById(d.getBairroCodigo()).map(b -> b.getNome()).orElse(null)
                : null;
        return new DistritoResponse(
                base.codigo(), base.nome(), base.endereco(), base.numero(), base.complemento(),
                base.cep(), base.ddd(), base.telefone(), base.fax(),
                base.tipoLogradouroCodigo(), base.bairroCodigo(), tiplogSigla, bairroNome);
    }
}
