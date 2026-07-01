package br.gov.mandaguari.saude.receituarioespecial.service;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.receituarioespecial.domain.ReceituarioEspecial;
import br.gov.mandaguari.saude.receituarioespecial.domain.ReceituarioEspecialId;
import br.gov.mandaguari.saude.receituarioespecial.domain.ReceituarioEspecialItem;
import br.gov.mandaguari.saude.receituarioespecial.dto.ReceituarioEspecialDtos.*;
import br.gov.mandaguari.saude.receituarioespecial.repository.PatientInfoProjection;
import br.gov.mandaguari.saude.receituarioespecial.repository.ReceituarioEspecialItemRepository;
import br.gov.mandaguari.saude.receituarioespecial.repository.ReceituarioEspecialRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Receituário Controle Especial (SAU_RECESP + SAU_RECESP1) — controlled-substance prescription, Portaria
 * SVS/MS 344/98. Rules mined from {@code sau_recesp_impl.java} (see SLICE-SPEC SAU_RECESP), cited
 * {@code // R<n>}. Every read and write is audited (R28, common/audit).
 *
 * <p><b>Regulatory-gated (CLAUDE.md Rule 5) — conservative choices pending CRF/regulatory sign-off:</b>
 * <ul>
 *   <li><b>R29 delete:</b> the legacy hard-deletes master + children with NO retention guard. That is
 *       almost certainly non-compliant, so {@link #delete} is <b>blocked</b> (409) rather than ported.
 *       Revisit once the retention/void model is decided (SLICE-SPEC OQ2).</li>
 *   <li><b>R14 deceased-patient block:</b> the legacy transaction has NO {@code PacObi}/óbito check — only
 *       the inactive-patient block (R12) exists. We reproduce it faithfully (no deceased block) and leave a
 *       marked hook; whether prescribing for a deceased patient must be blocked is SLICE-SPEC OQ3.</li>
 *   <li><b>R24 quantity:</b> no quantity/ceiling validation exists in the legacy — ported as optional.
 *       Portaria 344/98 quantity ceilings are SLICE-SPEC OQ6.</li>
 *   <li><b>R27 interaction/MPP advisory + R32-R36 print:</b> DEFERRED (see report proc; regulatory).</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class ReceituarioEspecialService {

    private static final int PACIENTE_INATIVO = 2;   // R12 (SAU_PAC.PacSit: 1=ativo, 2=inativo)
    private static final int MAX_OBS = 300;          // R7
    private static final int ALLOCATE_RETRIES = 5;   // R2: retry on per-unit code collision

    private final ReceituarioEspecialRepository repo;
    private final ReceituarioEspecialItemRepository itemRepo;
    private final AuditService audit;

    public ReceituarioEspecialService(ReceituarioEspecialRepository repo,
                                      ReceituarioEspecialItemRepository itemRepo, AuditService audit) {
        this.repo = repo;
        this.itemRepo = itemRepo;
        this.audit = audit;
    }

    // --- reads ---

    public Page<ReceituarioEspecialListItem> search(String nome, Integer unidade, Long paciente, Pageable pageable) {
        return repo.search(blank(nome), unidade, paciente, pageable)
                .map(p -> new ReceituarioEspecialListItem(p.getUnidadeCodigo(), p.getNumero(), p.getData(),
                        p.getPacienteCodigo(), p.getPacienteNome(), p.getPrescritorCodigo()));
    }

    public ReceituarioEspecialResponse get(Integer unidade, Long numero) {
        ReceituarioEspecial r = find(unidade, numero);
        audit.record("READ", "SAU_RECESP", key(unidade, numero));   // R28 / LGPD — PHI read
        return toResponse(r, new ArrayList<>());
    }

    // --- writes ---

    @Transactional
    public ReceituarioEspecialResponse create(ReceituarioEspecialWriteRequest req) {
        Integer unidade = req.unidadeCodigo();
        if (unidade == null || unidade == 0) {                       // R8 (unit is part of the PK)
            throw new BusinessRule("recesp.unidade.required", "Informe o Código da Unidade!");
        }
        List<String> avisos = validateMaster(req, unidade);

        ReceituarioEspecial r = new ReceituarioEspecial();
        r.setUnidadeId(unidade);
        applyMaster(r, req);
        r.setUsuarioLogin(currentActor());                           // R17
        r.setSequenciaUltima(0);                                     // R26 counter starts at 0
        ReceituarioEspecial saved = allocateAndSave(r, unidade);     // R1 + R2 (atomic retry)

        List<ReceituarioEspecialItem> itens = replaceItems(saved, req.itens());
        repo.save(saved);                                            // persist bumped RecEspSeqUlt (R26)

        audit.record("CREATE", "SAU_RECESP", key(unidade, saved.getCodigo()));   // R28
        return toResponse(saved, itens, avisos);
    }

    @Transactional
    public ReceituarioEspecialResponse update(Integer unidade, Long numero, ReceituarioEspecialWriteRequest req) {
        ReceituarioEspecial r = find(unidade, numero);
        List<String> avisos = validateMaster(req, unidade);

        applyMaster(r, req);
        r.setUsuarioLogin(currentActor());                           // R17
        List<ReceituarioEspecialItem> itens = replaceItems(r, req.itens());
        repo.save(r);

        audit.record("UPDATE", "SAU_RECESP", key(unidade, numero));  // R28
        return toResponse(r, itens, avisos);
    }

    /**
     * R29 (regulatory): the legacy hard-deletes master + all children with no retention guard. For a
     * controlled-substance prescription (Portaria 344/98) that is almost certainly non-compliant, so we
     * BLOCK the delete pending the retention/void decision (SLICE-SPEC OQ2) rather than silently porting it.
     */
    @Transactional
    public void delete(Integer unidade, Long numero) {
        find(unidade, numero);   // 404 if absent
        audit.record("DELETE_BLOCKED", "SAU_RECESP", key(unidade, numero));
        throw new Conflict("Receituário de Controle Especial não pode ser excluído: retenção obrigatória "
                + "(Portaria 344/98). A exclusão/cancelamento depende de definição regulatória.");
    }

    /**
     * R31 "Copiar Receituário": clone a prescription within the same unit — new server-allocated numero,
     * copy of the master fields and every line (preserving each line's own attributes). Per the mined note,
     * the copy re-stamps the acting user + date (the legacy preserved the original login/date).
     */
    @Transactional
    public ReceituarioEspecialResponse copy(Integer unidade, Long numero) {
        ReceituarioEspecial src = find(unidade, numero);
        List<ReceituarioEspecialItem> srcItens =
                itemRepo.findByUnidadeIdAndCodigoOrderBySequencia(unidade, numero);

        ReceituarioEspecial copy = new ReceituarioEspecial();
        copy.setUnidadeId(unidade);
        copy.setData(LocalDate.now());                               // re-stamp date
        copy.setPacienteCodigo(src.getPacienteCodigo());
        copy.setPrescritorCodigo(src.getPrescritorCodigo());
        copy.setFuncionarioCodigo(src.getFuncionarioCodigo());
        copy.setSituacao(src.getSituacao());
        copy.setTipoReceituario(src.getTipoReceituario());
        copy.setObservacao(src.getObservacao());
        copy.setUsuarioLogin(currentActor());                        // re-stamp actor
        copy.setSequenciaUltima(0);
        ReceituarioEspecial saved = allocateAndSave(copy, unidade);

        int seq = 0;
        List<ReceituarioEspecialItem> itens = new ArrayList<>();
        for (ReceituarioEspecialItem s : srcItens) {
            ReceituarioEspecialItem it = new ReceituarioEspecialItem();
            it.setUnidadeId(unidade);
            it.setCodigo(saved.getCodigo());
            it.setSequencia(++seq);
            it.setMedicamentoCodigo(s.getMedicamentoCodigo());
            it.setPrescricao(s.getPrescricao());
            it.setQuantidade(s.getQuantidade());
            it.setQuantidadeTipo(s.getQuantidadeTipo());
            it.setPosologiaCodigo(s.getPosologiaCodigo());
            it.setObservacao(s.getObservacao());
            it.setTipoReceita(s.getTipoReceita());
            it.setTipoUso(s.getTipoUso());
            it.setUsoContinuo(s.getUsoContinuo());
            it.setIndeferido(s.getIndeferido() != null ? s.getIndeferido() : Boolean.FALSE);
            itens.add(itemRepo.save(it));
        }
        saved.setSequenciaUltima(seq);                               // R26
        repo.save(saved);

        audit.record("CREATE", "SAU_RECESP", key(unidade, saved.getCodigo()));   // R28
        return toResponse(saved, itens, new ArrayList<>());
    }

    // --- validation ---

    /** Master validations (R4-R13); returns the non-blocking warnings (R13). */
    private List<String> validateMaster(ReceituarioEspecialWriteRequest req, Integer unidade) {
        List<String> avisos = new ArrayList<>();

        if (req.data() == null) {                                    // R4
            throw new BusinessRule("recesp.data.required", "Informe a Data do Receituário!");
        }
        if (!repo.unidadeExists(unidade)) {                          // R8
            throw new BusinessRule("recesp.unidade.notfound", "Não existe Unidade");
        }
        if (req.funcionarioCodigo() != null && req.funcionarioCodigo() != 0
                && !repo.funcionarioExists(req.funcionarioCodigo())) {   // R9 (0 allowed)
            throw new BusinessRule("recesp.funcionario.notfound", "Não existe Funcionários");
        }

        Long paciente = req.pacienteCodigo();
        if (paciente == null || paciente == 0) {                     // R5
            throw new BusinessRule("recesp.paciente.required", "Informe o Código do Paciente!");
        }
        if (!repo.pacienteExists(paciente)) {                        // R10
            throw new BusinessRule("recesp.paciente.notfound", "Não existe Paciente");
        }
        Optional<PatientInfoProjection> info = repo.findPatientInfo(paciente);
        if (info.isPresent()) {
            PatientInfoProjection pac = info.get();
            if (pac.getSituacao() != null && pac.getSituacao() == PACIENTE_INATIVO) {   // R12
                throw new BusinessRule("recesp.paciente.inativo", "Não é Possível Incluir Paciente Inativo!");
            }
            if (isBlank(pac.getCns())) {                             // R13 — WARNING, not blocking
                avisos.add("Paciente não possui Número do CNS Informado, Favor Atualizar o Cadastro!");
            }
            // R14: the legacy has NO deceased-patient (PacObi) block here — reproduced faithfully.
            // Whether prescribing for a deceased patient must be blocked is SLICE-SPEC OQ3 (regulatory).
        }

        Long prescritor = req.prescritorCodigo();
        if (prescritor == null || prescritor == 0) {                 // R6
            throw new BusinessRule("recesp.profissional.required", "Informe o Código do Profissional!");
        }
        if (!repo.prescritorExists(prescritor)) {                    // R11
            throw new BusinessRule("recesp.profissional.notfound", "Não existe Profissional");
        }
        if (req.observacao() != null && req.observacao().length() > MAX_OBS) {   // R7
            throw new BusinessRule("recesp.obs.length", "Número de caracteres não pode ser maior que 300!");
        }
        return avisos;
    }

    // --- apply / persist helpers ---

    private void applyMaster(ReceituarioEspecial r, ReceituarioEspecialWriteRequest req) {
        r.setData(req.data());
        r.setPacienteCodigo(req.pacienteCodigo());
        r.setPrescritorCodigo(req.prescritorCodigo());
        r.setFuncionarioCodigo(req.funcionarioCodigo());
        r.setSituacao(req.situacao());                               // R30 pass-through
        r.setTipoReceituario(req.tipoReceituario());                 // R30 pass-through
        r.setObservacao(req.observacao());
    }

    /** R1 + R2: allocate RecEspCod = MAX(per-unit) + 1, atomically retrying on a concurrent collision. */
    private ReceituarioEspecial allocateAndSave(ReceituarioEspecial r, Integer unidade) {
        for (int attempt = 0; attempt < ALLOCATE_RETRIES; attempt++) {
            long next = repo.findMaxCodigoForUnit(unidade).orElse(0L) + 1L;   // R1 (or 1 when none)
            r.setCodigo(next);
            try {
                return repo.saveAndFlush(r);
            } catch (DataIntegrityViolationException ex) {
                // another insert grabbed this number → recompute MAX and retry (R2)
            }
        }
        throw new Conflict("Não foi possível alocar o número do receituário na unidade " + unidade
                + " (concorrência). Tente novamente.");
    }

    /** Replace all lines of the prescription; validate + assign RecEspSeq and bump RecEspSeqUlt (R26). */
    private List<ReceituarioEspecialItem> replaceItems(ReceituarioEspecial master, List<ItemRequest> reqItems) {
        itemRepo.deleteByMaster(master.getUnidadeId(), master.getCodigo());
        List<ReceituarioEspecialItem> saved = new ArrayList<>();
        int seq = master.getSequenciaUltima() != null ? master.getSequenciaUltima() : 0;
        if (reqItems != null) {
            for (ItemRequest ir : reqItems) {
                saved.add(itemRepo.save(buildItem(master, ++seq, ir)));   // R26
            }
        }
        master.setSequenciaUltima(seq);                              // R26 monotonic counter
        return saved;
    }

    private ReceituarioEspecialItem buildItem(ReceituarioEspecial master, int seq, ItemRequest ir) {
        Integer remCod = ir.medicamentoCodigo();
        if (remCod != null && remCod != 0 && !itemRepo.medicamentoExists(remCod)) {   // R18 (0 allowed)
            throw new BusinessRule("recesp.item.medicamento.notfound", "Não existe Medicamento");
        }
        String prescricao = trim(ir.prescricao());
        if (remCod != null && remCod != 0) {                         // R19: default text from drug name
            String remNom = itemRepo.medicamentoNome(remCod).map(String::trim).orElse(null);
            if (!isBlank(remNom)) prescricao = remNom;
        }
        if (isBlank(prescricao)) {                                   // R20
            throw new BusinessRule("recesp.item.prescricao.required", "Informe a Prescrição!");
        }
        Integer remObsCod = ir.posologiaCodigo();
        if (remObsCod != null && remObsCod != 0 && !itemRepo.posologiaExists(remObsCod)) {   // R21 (0 allowed)
            throw new BusinessRule("recesp.item.posologia.notfound", "Não existe Posologia");
        }
        String obs = ir.observacao();
        if (remObsCod != null && remObsCod != 0) {                   // R22: default obs from posology
            String desc = itemRepo.posologiaDescricao(remObsCod).map(String::trim).orElse(null);
            if (!isBlank(desc)) obs = desc;
        }
        Integer tipo = ir.tipoReceita();
        if (tipo == null || tipo == 0) {                             // R23
            throw new BusinessRule("recesp.item.tipo.required", "Informe o Tipo do receituário!");
        }

        ReceituarioEspecialItem it = new ReceituarioEspecialItem();
        it.setUnidadeId(master.getUnidadeId());
        it.setCodigo(master.getCodigo());
        it.setSequencia(seq);
        it.setMedicamentoCodigo(remCod);
        it.setPrescricao(prescricao);
        it.setQuantidade(ir.quantidade());                          // R24 optional, unbounded
        it.setQuantidadeTipo(ir.quantidadeTipo());
        it.setPosologiaCodigo(remObsCod);
        it.setObservacao(obs);
        it.setTipoReceita(tipo);
        it.setTipoUso(ir.tipoUso());
        it.setUsoContinuo(ir.usoContinuo());
        it.setIndeferido(ir.indeferido() != null ? ir.indeferido() : Boolean.FALSE);   // R25 default false
        return it;
    }

    // --- helpers ---

    private ReceituarioEspecial find(Integer unidade, Long numero) {
        return repo.findById(new ReceituarioEspecialId(unidade, numero))
                .orElseThrow(() -> new NotFound("Receituário " + key(unidade, numero) + " não encontrado"));
    }

    /** Build the response, loading the items when not already at hand. */
    private ReceituarioEspecialResponse toResponse(ReceituarioEspecial r, List<ReceituarioEspecialItem> preloaded) {
        List<ReceituarioEspecialItem> itens = (preloaded != null && !preloaded.isEmpty())
                ? preloaded
                : itemRepo.findByUnidadeIdAndCodigoOrderBySequencia(r.getUnidadeId(), r.getCodigo());
        return toResponse(r, itens, new ArrayList<>());
    }

    private ReceituarioEspecialResponse toResponse(ReceituarioEspecial r, List<ReceituarioEspecialItem> itens,
                                                   List<String> avisos) {
        String nome = null, exibicao = null;
        Integer idade = null;
        if (r.getPacienteCodigo() != null) {
            Optional<PatientInfoProjection> info = repo.findPatientInfo(r.getPacienteCodigo());
            if (info.isPresent()) {
                PatientInfoProjection p = info.get();
                nome = p.getNome();                                  // R16 base
                exibicao = displayName(p);                           // R16
                idade = ageAt(p.getDataNascimento(), r.getData());   // R15
            }
        }
        List<ItemResponse> lines = itens.stream().map(ReceituarioEspecialService::toItemResponse).toList();
        return new ReceituarioEspecialResponse(
                r.getUnidadeId(), r.getCodigo(), r.getData(),
                r.getPacienteCodigo(), nome, exibicao, idade,
                r.getPrescritorCodigo(), r.getFuncionarioCodigo(),
                r.getSituacao(), r.getTipoReceituario(), r.getObservacao(),
                lines, avisos);
    }

    private static ItemResponse toItemResponse(ReceituarioEspecialItem it) {
        return new ItemResponse(it.getSequencia(), it.getMedicamentoCodigo(), it.getPrescricao(),
                it.getQuantidade(), it.getQuantidadeTipo(), it.getPosologiaCodigo(), it.getObservacao(),
                it.getTipoReceita(), it.getTipoUso(), it.getUsoContinuo(),
                Boolean.TRUE.equals(it.getIndeferido()));
    }

    /** R16: social name display — "NomeSocial (NomeCivil)" when the patient uses a social name. */
    private static String displayName(PatientInfoProjection p) {
        if (Boolean.TRUE.equals(p.getUsaNomeSocial()) && !isBlank(p.getNomeSocial())) {
            return p.getNomeSocial().trim() + " (" + safe(p.getNome()) + ")";
        }
        return p.getNome();
    }

    /** R15: patient age = age(birth, prescription date). */
    private static Integer ageAt(LocalDate nascimento, LocalDate data) {
        if (nascimento == null || data == null) return null;
        return Period.between(nascimento, data).getYears();
    }

    private static String key(Integer unidade, Long numero) { return unidade + "/" + numero; }
    private static String currentActor() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return (a != null && a.isAuthenticated()) ? a.getName() : "system";
    }
    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String trim(String s) { return s == null ? null : s.trim(); }
    private static String blank(String s) { return (s == null || s.isBlank()) ? null : s; }
    private static String safe(String s) { return s == null ? "" : s.trim(); }
}
