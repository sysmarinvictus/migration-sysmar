package br.gov.mandaguari.saude.setor.service;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.setor.domain.UniSetor;
import br.gov.mandaguari.saude.setor.domain.UniSetorId;
import br.gov.mandaguari.saude.setor.dto.UniSetorDtos.*;
import br.gov.mandaguari.saude.setor.mapper.UniSetorMapper;
import br.gov.mandaguari.saude.setor.repository.UniSetorRepository;
import br.gov.mandaguari.saude.unidade.domain.Unidade;
import br.gov.mandaguari.saude.unidade.repository.UnidadeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Setor da Unidade business logic — rules mined from {@code sau_unisetor_impl.java}.
 * Each rule carries a {@code // R<n>} citation per SLICE-SPEC SAU_UNISETOR.
 */
@Service
@Transactional(readOnly = true)
public class UniSetorService {

    /** GeneXus uses this sentinel for "empty" timestamp (NOT NULL columns). */
    static final LocalDateTime GX_NULL_DATE = LocalDateTime.of(1900, 1, 1, 0, 0, 0);

    private final UniSetorRepository repo;
    private final UniSetorMapper mapper;
    private final AuditService audit;
    private final UnidadeRepository unidadeRepo;

    public UniSetorService(UniSetorRepository repo, UniSetorMapper mapper,
                           AuditService audit, UnidadeRepository unidadeRepo) {
        this.repo = repo;
        this.mapper = mapper;
        this.audit = audit;
        this.unidadeRepo = unidadeRepo;
    }

    public Page<UniSetorResponse> list(Integer uniCod, String nome, Pageable pageable) {
        Page<UniSetor> page = (nome == null || nome.isBlank())
                ? repo.findAllByUniCod(uniCod, pageable)
                : repo.findAllByUniCodAndNomeContainingIgnoreCase(uniCod, nome, pageable);
        return page.map(this::enrich);
    }

    public UniSetorResponse get(Integer uniCod, Integer setorCod) {
        return enrich(find(uniCod, setorCod));
    }

    public List<UniSetorLookupItem> lookup(Integer uniCod, String q, Pageable pageable) {
        return repo.lookup(uniCod, q == null ? "" : q, pageable)
                   .stream().map(mapper::toLookupItem).toList();
    }

    @Transactional
    public UniSetorResponse create(Integer uniCod, UniSetorCreateRequest req) {
        requireUnidade(uniCod);                                      // R7: FK check SAU_UNI
        if (repo.existsById(new UniSetorId(uniCod, req.setorCod()))) { // R9: unique composite PK
            throw new Conflict("Já existe um setor " + req.setorCod() + " na unidade " + uniCod);
        }

        UniSetor s = new UniSetor();
        s.setUniCod(uniCod);
        s.setSetorCod(req.setorCod());
        s.setNome(req.nome().trim().toUpperCase());                  // R11: uppercase
        s.setEstocador(req.estocador());
        s.setSituacao(req.situacao() != null ? req.situacao() : "");
        s.setDataInativo(toGxDate(req.dataInativo()));
        s.setHorarioInicio(toGxDate(req.horarioInicio()));
        s.setHorarioFim(toGxDate(req.horarioFim()));

        UniSetor saved = repo.save(s);
        audit.record("CREATE", "SAU_UNISETOR", uniCod + "/" + req.setorCod()); // R17
        return enrich(saved);
    }

    @Transactional
    public UniSetorResponse update(Integer uniCod, Integer setorCod, UniSetorUpdateRequest req) {
        UniSetor s = find(uniCod, setorCod);                        // R18: path params are authoritative PK
        s.setNome(req.nome().trim().toUpperCase());                  // R11: uppercase
        s.setEstocador(req.estocador());
        s.setSituacao(req.situacao() != null ? req.situacao() : "");
        s.setDataInativo(toGxDate(req.dataInativo()));
        s.setHorarioInicio(toGxDate(req.horarioInicio()));
        s.setHorarioFim(toGxDate(req.horarioFim()));

        UniSetor saved = repo.save(s);
        audit.record("UPDATE", "SAU_UNISETOR", uniCod + "/" + setorCod); // R17
        return enrich(saved);
    }

    @Transactional
    public void delete(Integer uniCod, Integer setorCod) {
        UniSetor s = find(uniCod, setorCod);
        if (repo.isReferencedByPar5(uniCod, setorCod)) {            // R12
            throw new Conflict("Setor está vinculado a Configuração Unidade Saldo de Medicamento e não pode ser excluído");
        }
        if (repo.isReferencedByUsuUni1(uniCod, setorCod)) {        // R13
            throw new Conflict("Setor está vinculado a Usuário/Unidade/Setor e não pode ser excluído");
        }
        if (repo.isReferencedByRemLot(uniCod, setorCod)) {         // R14
            throw new Conflict("Setor está vinculado a Lote de Medicamento e não pode ser excluído");
        }
        if (repo.isReferencedByRemUnisetor(uniCod, setorCod)) {    // R15
            throw new Conflict("Setor está vinculado a SAU_REM_UNISETOR e não pode ser excluído");
        }
        repo.delete(s);
        audit.record("DELETE", "SAU_UNISETOR", uniCod + "/" + setorCod); // R17
    }

    // --- helpers ---

    private UniSetor find(Integer uniCod, Integer setorCod) {
        return repo.findById(new UniSetorId(uniCod, setorCod))
                .orElseThrow(() -> new NotFound(
                        "Setor " + setorCod + " da unidade " + uniCod + " não encontrado"));
    }

    /** R7: UniCod must reference an existing SAU_UNI row. */
    private void requireUnidade(Integer uniCod) {
        if (!unidadeRepo.existsById(uniCod)) {
            throw new BusinessRule("setor.unidade.notfound",
                    "Não existe 'Unidade de Atendimento' com código " + uniCod);
        }
    }

    /** Populates the three read-only derived fields from SAU_UNI (R8, R20). */
    private UniSetorResponse enrich(UniSetor s) {
        UniSetorResponse base = mapper.toResponse(s);
        // Map GX null-date back to null for the response
        LocalDateTime dataInativo = fromGxDate(base.dataInativo());
        LocalDateTime horIni = fromGxDate(base.horarioInicio());
        LocalDateTime horFim = fromGxDate(base.horarioFim());

        Unidade uni = unidadeRepo.findById(s.getUniCod()).orElse(null);
        String uniNom = uni != null ? uni.getNome() : null;
        Integer uniCnes = uni != null ? uni.getCnes() : null;
        Short uniSit = uni != null ? uni.getSituacao() : null;

        return new UniSetorResponse(
                base.uniCod(), base.setorCod(), base.nome(), base.estocador(), base.situacao(),
                dataInativo, horIni, horFim, uniNom, uniCnes, uniSit);
    }

    /** Converts null → GX nullDate sentinel before persistence. */
    static LocalDateTime toGxDate(LocalDateTime dt) {
        return dt == null ? GX_NULL_DATE : dt;
    }

    /** Converts GX nullDate sentinel → null for DTO responses. */
    static LocalDateTime fromGxDate(LocalDateTime dt) {
        if (dt == null) return null;
        return GX_NULL_DATE.equals(dt) ? null : dt;
    }
}
