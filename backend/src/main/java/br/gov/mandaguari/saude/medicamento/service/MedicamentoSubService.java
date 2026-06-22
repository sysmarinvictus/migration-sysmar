package br.gov.mandaguari.saude.medicamento.service;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.medicamento.domain.*;
import br.gov.mandaguari.saude.medicamento.dto.MedicamentoSubDtos.*;
import br.gov.mandaguari.saude.medicamento.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Sub-levels of SAU_REM: SAU_REM1, SAU_REM2, SAU_REM_UNISETOR, SAU_REMPOSO. */
@Service
@Transactional(readOnly = true)
public class MedicamentoSubService {

    private final MedicamentoRepository remRepo;
    private final MedicamentoUnidadeRepository unidadeRepo;
    private final MedicamentoEan13Repository ean13Repo;
    private final MedicamentoUnidadeSetorRepository uniSetorRepo;
    private final MedicamentoPosologiaRepository posologiaRepo;
    private final AuditService audit;

    public MedicamentoSubService(MedicamentoRepository remRepo,
                                 MedicamentoUnidadeRepository unidadeRepo,
                                 MedicamentoEan13Repository ean13Repo,
                                 MedicamentoUnidadeSetorRepository uniSetorRepo,
                                 MedicamentoPosologiaRepository posologiaRepo,
                                 AuditService audit) {
        this.remRepo = remRepo;
        this.unidadeRepo = unidadeRepo;
        this.ean13Repo = ean13Repo;
        this.uniSetorRepo = uniSetorRepo;
        this.posologiaRepo = posologiaRepo;
        this.audit = audit;
    }

    // ── SAU_REM1 — Unidade do Medicamento ─────────────────────────────────────
    public List<UnidadeResponse> listUnidades(Integer remCod) {
        return unidadeRepo.findByRemCodOrderByRemUniCod(remCod).stream().map(this::toUnidade).toList();
    }

    @Transactional
    public UnidadeResponse addUnidade(Integer remCod, UnidadeCriarRequest req) {
        requireMedicamento(remCod);
        if (req.unidadeCodigo() == null || req.unidadeCodigo() == 0)
            throw new BusinessRule("rem1.unidade.required", "Informe a Unidade");
        if (!remRepo.unidadeExists(req.unidadeCodigo()))                     // R32
            throw new BusinessRule("rem1.unidade.notfound", "Não existe Unidade");
        validateSit("rem1", req.situacao());                                // R40
        if (unidadeRepo.existsById(new MedicamentoUnidadeId(remCod, req.unidadeCodigo())))
            throw new Conflict("Unidade " + req.unidadeCodigo() + " já vinculada a este medicamento");
        MedicamentoUnidade u = new MedicamentoUnidade();
        u.setRemCod(remCod);
        u.setRemUniCod(req.unidadeCodigo());
        u.setEstoqueMinimo(req.estoqueMinimo());
        u.setSituacao(req.situacao());
        UnidadeResponse res = toUnidade(unidadeRepo.save(u));
        audit.record("CREATE", "SAU_REM1", remCod);
        return res;
    }

    /** R46: only estoqueMinimo + situacao mutable (PK RemUniCod immutable). */
    @Transactional
    public UnidadeResponse updateUnidade(Integer remCod, Integer uniCod, UnidadeAtualizarRequest req) {
        MedicamentoUnidade u = unidadeRepo.findById(new MedicamentoUnidadeId(remCod, uniCod))
                .orElseThrow(() -> new NotFound("Vínculo de unidade não encontrado"));
        validateSit("rem1", req.situacao());                                // R40
        u.setEstoqueMinimo(req.estoqueMinimo());
        u.setSituacao(req.situacao());
        UnidadeResponse res = toUnidade(unidadeRepo.save(u));
        audit.record("UPDATE", "SAU_REM1", remCod);
        return res;
    }

    @Transactional
    public void removeUnidade(Integer remCod, Integer uniCod) {
        MedicamentoUnidadeId id = new MedicamentoUnidadeId(remCod, uniCod);
        if (!unidadeRepo.existsById(id)) throw new NotFound("Vínculo de unidade não encontrado");
        if (remRepo.isRem1ReferencedByRemlot(remCod, uniCod))               // R27
            throw new Conflict("Unidade vinculada a Lote de Medicamento e não pode ser removida");
        unidadeRepo.deleteById(id);
        audit.record("DELETE", "SAU_REM1", remCod);
    }

    // ── SAU_REM2 — EAN-13 ─────────────────────────────────────────────────────
    public List<Ean13Response> listEan13(Integer remCod) {
        return ean13Repo.findByRemCodOrderByEan13(remCod).stream().map(this::toEan13).toList();
    }

    @Transactional
    public Ean13Response addEan13(Integer remCod, Ean13CriarRequest req) {
        requireMedicamento(remCod);
        if (req.ean13() == null) throw new BusinessRule("rem2.ean13.required", "Informe o código de barras");
        if (ean13Repo.existsById(new MedicamentoEan13Id(remCod, req.ean13())))   // R33
            throw new Conflict("Código de barras " + req.ean13() + " já cadastrado para este medicamento");
        MedicamentoEan13 e = new MedicamentoEan13();
        e.setRemCod(remCod);
        e.setEan13(req.ean13());
        Ean13Response res = toEan13(ean13Repo.save(e));
        audit.record("CREATE", "SAU_REM2", remCod);
        return res;
    }

    @Transactional
    public void removeEan13(Integer remCod, Long ean13) {
        MedicamentoEan13Id id = new MedicamentoEan13Id(remCod, ean13);
        if (!ean13Repo.existsById(id)) throw new NotFound("Código de barras não encontrado");
        ean13Repo.deleteById(id);
        audit.record("DELETE", "SAU_REM2", remCod);
    }

    // ── SAU_REM_UNISETOR — Medicamento por Unidade+Setor ──────────────────────
    public List<UnidadeSetorResponse> listUnidadeSetores(Integer remCod) {
        return uniSetorRepo.findByRemCodOrderBySequencia(remCod).stream().map(this::toUniSetor).toList();
    }

    @Transactional
    public UnidadeSetorResponse addUnidadeSetor(Integer remCod, UnidadeSetorCriarRequest req) {
        Medicamento parent = requireMedicamento(remCod);
        if (req.unidadeCodigo() == null || req.unidadeCodigo() == 0)
            throw new BusinessRule("remunisetor.unidade.required", "Informe a Unidade");
        if (!remRepo.unidadeExists(req.unidadeCodigo()))                    // R28
            throw new BusinessRule("remunisetor.unidade.notfound", "Não existe Vinculo de Medicamento com Unidade e Setor");
        if (req.setorCodigo() != null && req.setorCodigo() != 0
                && !remRepo.unidadeSetorExists(req.unidadeCodigo(), req.setorCodigo()))   // R29
            throw new BusinessRule("remunisetor.pair.notfound", "Não existe Vinculo de Medicamento com Unidade e Setor");
        validateSit("remunisetor", req.situacao());                        // R39
        if (uniSetorRepo.countByUnidadeSetorRem(req.unidadeCodigo(), req.setorCodigo(), remCod) > 0)  // R30
            throw new Conflict("Cód.Uni.,Cód.Setor,Código já cadastrado");

        int novaSeq = (parent.getUltimaSequenciaUnidadeSetor() == null ? 0
                : parent.getUltimaSequenciaUnidadeSetor()) + 1;            // R14
        parent.setUltimaSequenciaUnidadeSetor(novaSeq);
        remRepo.save(parent);

        MedicamentoUnidadeSetor s = new MedicamentoUnidadeSetor();
        s.setRemCod(remCod);
        s.setSequencia(novaSeq);
        s.setUnidadeCodigo(req.unidadeCodigo());
        s.setSetorCodigo(req.setorCodigo());
        s.setEstoqueMinimo(req.estoqueMinimo());
        s.setSituacao(req.situacao() != null ? req.situacao() : 1);
        UnidadeSetorResponse res = toUniSetor(uniSetorRepo.save(s));
        syncUnidadesFromUnidadeSetor(remCod);                              // R36
        audit.record("CREATE", "SAU_REM_UNISETOR", remCod);
        return res;
    }

    /** R31: only estoqueMinimo + situacao mutable. */
    @Transactional
    public UnidadeSetorResponse updateUnidadeSetor(Integer remCod, Integer seq, Integer uniCod,
                                                   UnidadeSetorAtualizarRequest req) {
        MedicamentoUnidadeSetor s = uniSetorRepo.findById(new MedicamentoUnidadeSetorId(remCod, seq, uniCod))
                .orElseThrow(() -> new NotFound("Vínculo unidade+setor não encontrado"));
        validateSit("remunisetor", req.situacao());                        // R39
        s.setEstoqueMinimo(req.estoqueMinimo());
        s.setSituacao(req.situacao() != null ? req.situacao() : s.getSituacao());
        UnidadeSetorResponse res = toUniSetor(uniSetorRepo.save(s));
        syncUnidadesFromUnidadeSetor(remCod);                              // R36
        audit.record("UPDATE", "SAU_REM_UNISETOR", remCod);
        return res;
    }

    @Transactional
    public void removeUnidadeSetor(Integer remCod, Integer seq, Integer uniCod) {
        MedicamentoUnidadeSetorId id = new MedicamentoUnidadeSetorId(remCod, seq, uniCod);
        if (!uniSetorRepo.existsById(id)) throw new NotFound("Vínculo unidade+setor não encontrado");
        uniSetorRepo.deleteById(id);
        // R14: RemUniSetorSeqUlt is a monotonic high-water mark — NOT decremented on delete
        // (decrementing would let the next insert reuse a live RemUniSetorSeq → PK collision).
        syncUnidadesFromUnidadeSetor(remCod);                              // R36
        audit.record("DELETE", "SAU_REM_UNISETOR", remCod);
    }

    /**
     * R36 (MedicamentoAtualizaUniAtendimento): reconcile SAU_REM1 with the current set of
     * SAU_REM_UNISETOR unidades. Non-destructive (resolves OQ-7): existing SAU_REM1 rows are
     * upserted in place — preserving estoqueMinimo and only propagating situação — and rows for
     * unidades no longer present are removed ONLY when not referenced by SAU_REMLOT (R27 guard).
     * Runs within the caller's transaction.
     */
    private void syncUnidadesFromUnidadeSetor(Integer remCod) {
        // target: distinct unidades from REM_UNISETOR → situação (first occurrence wins)
        var target = new java.util.LinkedHashMap<Integer, Short>();
        for (MedicamentoUnidadeSetor s : uniSetorRepo.findByRemCodOrderBySequencia(remCod))
            target.putIfAbsent(s.getUnidadeCodigo(), s.getSituacao());

        // remove SAU_REM1 rows whose unidade dropped out — but never one guarded by SAU_REMLOT (R27)
        for (MedicamentoUnidade u : unidadeRepo.findByRemCodOrderByRemUniCod(remCod)) {
            if (!target.containsKey(u.getRemUniCod())
                    && !remRepo.isRem1ReferencedByRemlot(remCod, u.getRemUniCod())) {
                unidadeRepo.deleteById(new MedicamentoUnidadeId(remCod, u.getRemUniCod()));
            }
        }
        unidadeRepo.flush();

        // upsert target unidades (preserve estoqueMinimo on existing rows; set situação)
        for (var e : target.entrySet()) {
            MedicamentoUnidade u = unidadeRepo.findById(new MedicamentoUnidadeId(remCod, e.getKey()))
                    .orElseGet(() -> {
                        MedicamentoUnidade n = new MedicamentoUnidade();
                        n.setRemCod(remCod);
                        n.setRemUniCod(e.getKey());
                        return n;
                    });
            u.setSituacao(e.getValue());
            unidadeRepo.save(u);
        }
    }

    // ── SAU_REMPOSO — Posologia ────────────────────────────────────────────────
    public List<PosologiaResponse> listPosologias(Integer remCod) {
        return posologiaRepo.findByRemCodOrderByPosologiaCodigo(remCod).stream().map(this::toPosologia).toList();
    }

    public long countPosologias(Integer remCod) { return posologiaRepo.countByRemCod(remCod); } // R13

    @Transactional
    public PosologiaResponse addPosologia(Integer remCod, PosologiaCriarRequest req) {
        Medicamento parent = requireMedicamento(remCod);
        // R45: only when usarPosologia=true
        if (!Boolean.TRUE.equals(parent.getUsarPosologia()))
            throw new BusinessRule("remposo.usarposologia.off",
                    "Este medicamento não utiliza posologia (RemUsarPosologia desativado)");
        if (req.posologiaCodigo() == null || req.posologiaCodigo() == 0)
            throw new BusinessRule("remposo.posologia.required", "Informe a Posologia");
        if (!remRepo.posologiaExists(req.posologiaCodigo()))                // R34
            throw new BusinessRule("remposo.posologia.notfound",
                    "Não existe Vinculo de medicamento com a tabela de posologia");
        if (posologiaRepo.existsById(new MedicamentoPosologiaId(remCod, req.posologiaCodigo())))  // R35
            throw new Conflict("Posologia " + req.posologiaCodigo() + " já vinculada a este medicamento");
        MedicamentoPosologia p = new MedicamentoPosologia();
        p.setRemCod(remCod);
        p.setPosologiaCodigo(req.posologiaCodigo());
        PosologiaResponse res = toPosologia(posologiaRepo.save(p));
        audit.record("CREATE", "SAU_REMPOSO", remCod);
        return res;
    }

    @Transactional
    public void removePosologia(Integer remCod, Integer posologiaCodigo) {
        MedicamentoPosologiaId id = new MedicamentoPosologiaId(remCod, posologiaCodigo);
        if (!posologiaRepo.existsById(id)) throw new NotFound("Vínculo de posologia não encontrado");
        posologiaRepo.deleteById(id);
        audit.record("DELETE", "SAU_REMPOSO", remCod);
    }

    // ── cascade (R23-R26): REMPOSO → REM2 → REM_UNISETOR → REM1 ────────────────
    @Transactional
    public void cascadeDeleteForMedicamento(Integer remCod) {
        posologiaRepo.deleteByRemCod(remCod);  // R23
        ean13Repo.deleteByRemCod(remCod);      // R24
        uniSetorRepo.deleteByRemCod(remCod);   // R25
        unidadeRepo.deleteByRemCod(remCod);    // R26
    }

    // ── helpers ─────────────────────────────────────────────────────────────────
    private Medicamento requireMedicamento(Integer remCod) {
        return remRepo.findById(remCod)
                .orElseThrow(() -> new NotFound("Medicamento " + remCod + " não encontrado"));
    }

    private static void validateSit(String prefix, Short sit) {
        if (sit != null && sit != 1 && sit != 2)
            throw new BusinessRule(prefix + ".situacao.invalida", "Situação inválida (1=ATIVO, 2=INATIVO)");
    }

    private UnidadeResponse toUnidade(MedicamentoUnidade u) {
        return new UnidadeResponse(u.getRemCod(), u.getRemUniCod(), u.getEstoqueMinimo(), u.getSituacao());
    }
    private Ean13Response toEan13(MedicamentoEan13 e) {
        return new Ean13Response(e.getRemCod(), e.getEan13());
    }
    private UnidadeSetorResponse toUniSetor(MedicamentoUnidadeSetor s) {
        return new UnidadeSetorResponse(s.getRemCod(), s.getSequencia(), s.getUnidadeCodigo(),
                s.getSetorCodigo(), s.getEstoqueMinimo(), s.getSituacao());
    }
    private PosologiaResponse toPosologia(MedicamentoPosologia p) {
        return new PosologiaResponse(p.getRemCod(), p.getPosologiaCodigo());
    }
}
