package br.gov.mandaguari.saude.medicamento.service;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.medicamento.domain.Medicamento;
import br.gov.mandaguari.saude.medicamento.dto.MedicamentoDtos.*;
import br.gov.mandaguari.saude.medicamento.mapper.MedicamentoMapper;
import br.gov.mandaguari.saude.medicamento.repository.MedicamentoRepository;
import br.gov.mandaguari.saude.medicamento.repository.RenameAtualFlags;
import br.gov.mandaguari.saude.medicamento.repository.RenameDisplay;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Medicamento (SAU_REM) business logic — rules mined from {@code sau_rem_impl.java}. Each rule
 * carries a {@code // R<n>} citation. Optimistic locking (R47/R48) is deferred: the physical
 * SAU_REM has no version column (see open_questions). Sub-levels are managed by
 * {@link MedicamentoSubService}.
 */
@Service
@Transactional(readOnly = true)
public class MedicamentoService {

    private final MedicamentoRepository repo;
    private final MedicamentoMapper mapper;
    private final AuditService audit;
    private final MedicamentoSubService subService;

    public MedicamentoService(MedicamentoRepository repo, MedicamentoMapper mapper,
                              AuditService audit, MedicamentoSubService subService) {
        this.repo = repo;
        this.mapper = mapper;
        this.audit = audit;
        this.subService = subService;
    }

    /** List + filter (hwwsau_rem): nome LIKE, tipoMedicamentoCodigo, situacao, psicotropico, controleEspecial, etico. */
    public Page<MedicamentoResponse> list(String nome, Integer tipoMedicamentoCodigo, Short situacao,
                                          Short psicotropico, Short controleEspecial, Short etico,
                                          Pageable pageable) {
        Specification<Medicamento> spec = (root, query, cb) -> {
            var ps = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (nome != null && !nome.isBlank())
                ps.add(cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%"));
            if (nz(tipoMedicamentoCodigo)) ps.add(cb.equal(root.get("tipoMedicamentoCodigo"), tipoMedicamentoCodigo));
            if (situacao != null) ps.add(cb.equal(root.get("situacao"), situacao));
            if (psicotropico != null) ps.add(cb.equal(root.get("psicotropico"), psicotropico));
            if (controleEspecial != null) ps.add(cb.equal(root.get("controleEspecial"), controleEspecial));
            if (etico != null) ps.add(cb.equal(root.get("etico"), etico));
            return cb.and(ps.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return repo.findAll(spec, pageable).map(this::toResponse);
    }

    public MedicamentoResponse get(Integer id) {
        return toResponse(find(id));
    }

    public List<MedicamentoLookupItem> lookup(String q, Pageable pageable) {
        return repo.lookup(q == null ? "" : q, pageable).stream().map(mapper::toLookupItem).toList();
    }

    @Transactional
    public MedicamentoResponse create(MedicamentoCreateRequest req) {
        Medicamento m = new Medicamento();
        applyCreate(m, req);
        validate(m);                              // R1-R12, R37, R38, R42
        // R50: boolean flags default to false/0 on insert
        if (m.getPsicotropico() == null) m.setPsicotropico((short) 0);
        if (m.getControleEspecial() == null) m.setControleEspecial((short) 0);
        if (m.getUsarPosologia() == null) m.setUsarPosologia(false);

        m.setId(repo.nextCodigo());               // R15
        stampUser(m);                             // R16
        Medicamento saved = repo.save(m);
        audit.record("CREATE", "SAU_REM", saved.getId());   // R18
        return toResponse(saved);
    }

    @Transactional
    public MedicamentoResponse update(Integer id, MedicamentoUpdateRequest req) {
        Medicamento m = find(id);
        boolean prevMpp = Boolean.TRUE.equals(m.getMedicamentoPotencialmentePerigoso());
        Short prevConEsp = m.getControleEspecial();

        applyUpdate(m, req);
        validate(m);                              // R1-R12, R37, R38, R42

        // R44: MPP true → false requires motivo; stamp login + data
        if (prevMpp && !Boolean.TRUE.equals(m.getMedicamentoPotencialmentePerigoso())) {
            if (isBlank(req.mppCancelamentoMotivo()))
                throw new BusinessRule("rem.mpp.motivo.required", "Informe o motivo de cancelamento do MPP");
            m.setMppCancelamentoMotivo(req.mppCancelamentoMotivo());
            m.setMppCancelamentoData(LocalDateTime.now());
            m.setMppCancelamentoLogin(currentLogin());
        }
        // R53: cannot unset controle especial while active SAU_RECESP prescriptions exist
        if (isTrue(prevConEsp) && !isTrue(m.getControleEspecial()) && repo.isReferencedByRecespItens(id))
            throw new BusinessRule("rem.conesp.bloqueado",
                    "Não é possível remover o controle especial: existem receituários ativos para este medicamento");

        stampUser(m);                             // R16
        Medicamento saved = repo.save(m);
        audit.record("UPDATE", "SAU_REM", saved.getId());   // R18
        return toResponse(saved);
    }

    @Transactional
    public void delete(Integer id) {
        Medicamento m = find(id);
        guardDelete(id);                          // R19-R22
        subService.cascadeDeleteForMedicamento(id); // R23-R26 (REMPOSO→REM2→REM_UNISETOR→REM1)
        repo.delete(m);
        audit.record("DELETE", "SAU_REM", id);    // R18
    }

    // ── validation ────────────────────────────────────────────────────────────

    private void validate(Medicamento m) {
        // R1
        if (isBlank(m.getNome()))
            throw new BusinessRule("rem.nome.required", "Informe o Nome do Medicamento!");
        // R2
        if (nz(m.getTipoMedicamentoCodigo()) && !repo.tipoMedicamentoExists(m.getTipoMedicamentoCodigo()))
            throw new BusinessRule("rem.tiprem.notfound", "Não existe Tipos de Medicamento");
        // R3
        if (!isBlank(m.getDcbCodigo()) && !repo.dcbExists(m.getDcbCodigo()))
            throw new BusinessRule("rem.dcb.notfound", "Não existe DCB");
        // R4
        if (nz(m.getApresentacaoCodigo()) && !repo.apresentacaoExists(m.getApresentacaoCodigo()))
            throw new BusinessRule("rem.aprrem.notfound", "Não existe Forma de Apresentação");
        // R6
        if (!isBlank(m.getRenameCodigo()) && !repo.renameExists(m.getRenameCodigo()))
            throw new BusinessRule("rem.rename.notfound", "Não existe SAU_ RENAME");
        // R8
        if (!isBlank(m.getObmCodigo()) && !repo.obmExists(m.getObmCodigo()))
            throw new BusinessRule("rem.obm.notfound", "Não existe OBM");

        // R11: when semRename, bypass R5/R7/R9/R10
        boolean semRename = Boolean.TRUE.equals(m.getSemRename());
        if (!semRename && !isBlank(m.getRenameAtualCodigo())) {
            // R5
            if (!repo.renameAtualExists(m.getRenameAtualCodigo()))
                throw new BusinessRule("rem.renameatual.notfound",
                        "Não existe rename atualizado com as tabelas do webservice do Hórus");
            // R7: de-para pair (only when both present)
            if (!isBlank(m.getRenameCodigo())
                    && !repo.renameDeparaExists(m.getRenameCodigo(), m.getRenameAtualCodigo()))
                throw new BusinessRule("rem.depara.notfound", "Não existe Tabela de-para RENAME");
            // R9/R10: type compatibility + derivation from RenameAtual flags
            reconcileTipoProduto(m);
        }
        // R37
        if (m.getSituacao() != null && m.getSituacao() != 1 && m.getSituacao() != 2)
            throw new BusinessRule("rem.situacao.invalida", "Situação inválida (1=ATIVO, 2=INATIVO)");
        // R38
        if (m.getTipoProduto() != null && (m.getTipoProduto() < 0 || m.getTipoProduto() > 4))
            throw new BusinessRule("rem.tipoproduto.invalido", "Tipo de Produto inválido (0-4)");
        // R42
        if (isTrue(m.getPsicotropico()) && isBlank(m.getPortariaPsicotropico()))
            throw new BusinessRule("rem.portaria.required",
                    "Informe a Portaria do medicamento psicotrópico (Portaria SVS/MS 344/98)");
    }

    /** R9 (reject incompatible) + R10 (derive from flags) for the selected RenameAtual record. */
    private void reconcileTipoProduto(Medicamento m) {
        RenameAtualFlags f = repo.renameAtualFlags(m.getRenameAtualCodigo()).orElse(null);
        if (f == null) return;
        short expected = (short) (
                Boolean.TRUE.equals(f.getBasico()) ? 1
              : Boolean.TRUE.equals(f.getEstrategico()) ? 2
              : Boolean.TRUE.equals(f.getProprio()) ? 3
              : Boolean.TRUE.equals(f.getEspecializado()) ? 4 : 0);
        Short provided = m.getTipoProduto();
        if (provided != null && provided != 0 && provided != expected)
            throw new BusinessRule("rem.tipoproduto.incompativel",
                    "Tipo não é compativel, esperado " + expected);   // R9
        m.setTipoProduto(expected);                                    // R10
    }

    /** R19-R22: block delete when referenced. */
    private void guardDelete(Integer id) {
        if (repo.isReferencedByInteracaoRem1(id))
            throw new Conflict("Medicamento está vinculado a Interação Medicamentosa (REM1) e não pode ser excluído");  // R20
        if (repo.isReferencedByInteracaoRem2(id))
            throw new Conflict("Medicamento está vinculado a Interação Medicamentosa (REM2) e não pode ser excluído");  // R19
        if (repo.isReferencedByRecespItens(id))
            throw new Conflict("Medicamento está vinculado a Itens do Receituário Controle Especial e não pode ser excluído"); // R21
        if (repo.isReferencedByRemlot(id))
            throw new Conflict("Medicamento está vinculado a Lote de Medicamento e não pode ser excluído");            // R22
    }

    // ── apply request → entity ──────────────────────────────────────────────────

    private static void applyCreate(Medicamento m, MedicamentoCreateRequest r) {
        m.setNome(trim(r.nome()));
        m.setTipoMedicamentoCodigo(r.tipoMedicamentoCodigo());
        m.setDcbCodigo(trim(r.dcbCodigo()));
        m.setRenameCodigo(trim(r.renameCodigo()));
        m.setRenameAtualCodigo(trim(r.renameAtualCodigo()));
        m.setApresentacaoCodigo(r.apresentacaoCodigo());
        m.setObmCodigo(trim(r.obmCodigo()));
        m.setTipoProduto(r.tipoProduto());
        m.setConcentracao(trim(r.concentracao()));
        m.setFarmaciaBasica(r.farmaciaBasica());
        m.setPsicotropico(r.psicotropico());
        m.setControleEspecial(r.controleEspecial());
        m.setEtico(r.etico());
        m.setValorHospitalar(r.valorHospitalar());
        m.setValorUnitario(r.valorUnitario());
        m.setSemRename(r.semRename());
        m.setPortariaPsicotropico(trim(r.portariaPsicotropico()));
        m.setSituacao(r.situacao());
        m.setOmitirSaldo(r.omitirSaldo());
        m.setUsarPosologia(r.usarPosologia());
        m.setMedicamentoPotencialmentePerigoso(r.medicamentoPotencialmentePerigoso());
        m.setMppEfeitos(trim(r.mppEfeitos()));
    }

    private static void applyUpdate(Medicamento m, MedicamentoUpdateRequest r) {
        m.setNome(trim(r.nome()));
        m.setTipoMedicamentoCodigo(r.tipoMedicamentoCodigo());
        m.setDcbCodigo(trim(r.dcbCodigo()));
        m.setRenameCodigo(trim(r.renameCodigo()));
        m.setRenameAtualCodigo(trim(r.renameAtualCodigo()));
        m.setApresentacaoCodigo(r.apresentacaoCodigo());
        m.setObmCodigo(trim(r.obmCodigo()));
        m.setTipoProduto(r.tipoProduto());
        m.setConcentracao(trim(r.concentracao()));
        m.setFarmaciaBasica(r.farmaciaBasica());
        m.setPsicotropico(r.psicotropico());
        m.setControleEspecial(r.controleEspecial());
        m.setEtico(r.etico());
        m.setValorHospitalar(r.valorHospitalar());
        m.setValorUnitario(r.valorUnitario());
        m.setSemRename(r.semRename());
        m.setPortariaPsicotropico(trim(r.portariaPsicotropico()));
        m.setSituacao(r.situacao());
        m.setOmitirSaldo(r.omitirSaldo());
        m.setUsarPosologia(r.usarPosologia());
        m.setMedicamentoPotencialmentePerigoso(r.medicamentoPotencialmentePerigoso());
        m.setMppEfeitos(trim(r.mppEfeitos()));
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    private Medicamento find(Integer id) {
        return repo.findById(id).orElseThrow(() -> new NotFound("Medicamento " + id + " não encontrado"));
    }

    private MedicamentoResponse toResponse(Medicamento m) {
        String renameDescricao = deriveRenameDescricao(m);          // R12
        long posologiaCount = subService.countPosologias(m.getId()); // R13
        return mapper.toResponse(m, renameDescricao, posologiaCount);
    }

    /** R12: RENAMEPrincAt, RENAMEConc, RENAMEFormFarm, RENAMEVol, RENAMEUnd joined by ', '. */
    private String deriveRenameDescricao(Medicamento m) {
        if (isBlank(m.getRenameCodigo())) return null;
        RenameDisplay d = repo.renameDisplay(m.getRenameCodigo()).orElse(null);
        if (d == null) return null;
        return String.join(", ",
                nvl(d.getPrincipioAtivo()), nvl(d.getConcentracao()), nvl(d.getFormaFarmaceutica()),
                nvl(d.getVolume()), nvl(d.getUnidade()));
    }

    private static void stampUser(Medicamento m) { m.setUsuarioLogin(currentLogin()); }

    private static String currentLogin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "sistema";
    }

    private static boolean isTrue(Short s) { return s != null && s == 1; }
    private static boolean nz(Integer i) { return i != null && i != 0; }
    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String nvl(String s) { return s == null ? "" : s; }
    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
