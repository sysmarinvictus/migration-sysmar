package br.gov.mandaguari.saude.unidade.service;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.unidade.domain.*;
import br.gov.mandaguari.saude.unidade.dto.UnidadeSubDtos.*;
import br.gov.mandaguari.saude.unidade.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class UnidadeSubService {

    private final HiperdiaProfissionalRepository hiperdiaRepo;
    private final SisPreNatalProfissionalRepository sisPreNatalRepo;
    private final NutricionistaProfissionalRepository nutricionistaRepo;
    private final SalaRepository salaRepo;
    private final AuditService audit;

    public UnidadeSubService(HiperdiaProfissionalRepository hiperdiaRepo,
                             SisPreNatalProfissionalRepository sisPreNatalRepo,
                             NutricionistaProfissionalRepository nutricionistaRepo,
                             SalaRepository salaRepo,
                             AuditService audit) {
        this.hiperdiaRepo = hiperdiaRepo;
        this.sisPreNatalRepo = sisPreNatalRepo;
        this.nutricionistaRepo = nutricionistaRepo;
        this.salaRepo = salaRepo;
        this.audit = audit;
    }

    // ── SAU_UNI1 — Hiperdia ──────────────────────────────────────────────────

    public List<HiperdiaResponse> listHiperdia(Integer uniCod) {
        return hiperdiaRepo.findByUniCod(uniCod).stream().map(this::toHiperdiaResponse).toList();
    }

    @Transactional
    public HiperdiaResponse addHiperdia(Integer uniCod, HiperdiaCriarRequest req) {
        // R56: profissionalId required
        if (req.profissionalId() == null || req.profissionalId() == 0)
            throw new BusinessRule("hiperdia.profissional.required", "Não existe PROFISSONAL.");
        // R57: dataInclusao required
        if (req.dataInclusao() == null)
            throw new BusinessRule("hiperdia.datinc.required", "Informe a Data de Inclusão!");
        // R58: when inativo, dataDesativacao required
        if (req.status() != null && req.status() == 2 && req.dataDesativacao() == null)
            throw new BusinessRule("hiperdia.datdes.required", "Informe a Data de Desativação!");

        HiperdiaProfissional h = new HiperdiaProfissional();
        h.setUniCod(uniCod);
        h.setUniProPesCod(req.profissionalId());
        h.setDatInclusao(req.dataInclusao());
        h.setMatricula(req.matricula());
        h.setCbo(req.cbo());
        h.setStatus(req.status() != null ? req.status() : 1); // R59: default ATIVO
        h.setDatDesativacao(req.dataDesativacao());

        HiperdiaProfissional saved = hiperdiaRepo.save(h);
        audit.record("CREATE", "SAU_UNI1", uniCod);
        return toHiperdiaResponse(saved);
    }

    @Transactional
    public void removeHiperdia(Integer uniCod, Long profId) {
        HiperdiaProfissionalId id = new HiperdiaProfissionalId(uniCod, profId);
        if (!hiperdiaRepo.existsById(id))
            throw new NotFound("Profissional HIPERDIA " + profId + " não encontrado na unidade " + uniCod);
        hiperdiaRepo.deleteById(id);
        audit.record("DELETE", "SAU_UNI1", uniCod);
    }

    // ── SAU_UNI2 — SisPré-Natal ──────────────────────────────────────────────

    public List<SisPreNatalResponse> listSisPreNatal(Integer uniCod) {
        return sisPreNatalRepo.findByUniCod(uniCod).stream().map(this::toSisPreNatalResponse).toList();
    }

    @Transactional
    public SisPreNatalResponse addSisPreNatal(Integer uniCod, SisPreNatalCriarRequest req) {
        if (req.profissionalId() == null || req.profissionalId() == 0)
            throw new BusinessRule("sisprental.prof.required", "Não existe Profissional.");
        if (req.especialidadeId() == null || req.especialidadeId() == 0)
            throw new BusinessRule("sisprenatal.esp.required", "Não existe Profissional.");
        if (req.dataInclusao() == null)
            throw new BusinessRule("sisprenatal.datinc.required", "Informe a Data de Inclusão!");
        if (req.status() != null && req.status() == 2 && req.dataDesativacao() == null)
            throw new BusinessRule("sisprenatal.datdes.required", "Informe a Data de Desativação!");
        // R65: CBO validation deferred (requires SAU_PROESP lookup — Wave 4)

        SisPreNatalProfissional s = new SisPreNatalProfissional();
        s.setUniCod(uniCod);
        s.setUniGesProPesCod(req.profissionalId());
        s.setUniGesEspCod(req.especialidadeId());
        s.setDatInclusao(req.dataInclusao());
        s.setStatus(req.status() != null ? req.status() : 1);
        s.setDatDesativacao(req.dataDesativacao());

        SisPreNatalProfissional saved = sisPreNatalRepo.save(s);
        audit.record("CREATE", "SAU_UNI2", uniCod);
        return toSisPreNatalResponse(saved);
    }

    @Transactional
    public void removeSisPreNatal(Integer uniCod, Long profId, Integer espId) {
        SisPreNatalProfissionalId id = new SisPreNatalProfissionalId(uniCod, profId, espId);
        if (!sisPreNatalRepo.existsById(id))
            throw new NotFound("Profissional SISPRENATAL não encontrado");
        sisPreNatalRepo.deleteById(id);
        audit.record("DELETE", "SAU_UNI2", uniCod);
    }

    // ── SAU_UNI3 — Nutricionistas ─────────────────────────────────────────────

    public List<NutricionistaResponse> listNutricionistas(Integer uniCod) {
        return nutricionistaRepo.findByUniCod(uniCod).stream().map(this::toNutricionistaResponse).toList();
    }

    @Transactional
    public NutricionistaResponse addNutricionista(Integer uniCod, NutricionistaCriarRequest req) {
        if (req.profissionalId() == null || req.profissionalId() == 0)
            throw new BusinessRule("nutri.prof.required", "Não existe Profissional.");
        if (req.especialidadeId() == null || req.especialidadeId() == 0)
            throw new BusinessRule("nutri.esp.required", "Não existe Profissional.");
        if (req.dataInclusao() == null)
            throw new BusinessRule("nutri.datinc.required", "Informe a Data de Inclusão!");
        if (req.status() != null && req.status() == 2 && req.dataDesativacao() == null)
            throw new BusinessRule("nutri.datdes.required", "Informe a Data de Desativação!");

        NutricionistaProfissional n = new NutricionistaProfissional();
        n.setUniCod(uniCod);
        n.setUniNutProPesCod(req.profissionalId());
        n.setUniNutEspCod(req.especialidadeId());
        n.setDatInclusao(req.dataInclusao());
        n.setStatus(req.status() != null ? req.status() : 1);
        n.setDatDesativacao(req.dataDesativacao());

        NutricionistaProfissional saved = nutricionistaRepo.save(n);
        audit.record("CREATE", "SAU_UNI3", uniCod);
        return toNutricionistaResponse(saved);
    }

    @Transactional
    public void removeNutricionista(Integer uniCod, Long profId, Integer espId) {
        NutricionistaProfissionalId id = new NutricionistaProfissionalId(uniCod, profId, espId);
        if (!nutricionistaRepo.existsById(id))
            throw new NotFound("Nutricionista não encontrado");
        nutricionistaRepo.deleteById(id);
        audit.record("DELETE", "SAU_UNI3", uniCod);
    }

    // ── SAU_UNISALA — Salas ────────────────────────────────────────────────────

    public List<SalaResponse> listSalas(Integer uniCod) {
        return salaRepo.findByUniCodOrderBySalaCod(uniCod).stream().map(this::toSalaResponse).toList();
    }

    @Transactional
    public SalaResponse addSala(Integer uniCod, SalaCriarRequest req) {
        if (req.salaCodigo() == null)
            throw new BusinessRule("sala.cod.required", "Informe o código da sala.");
        // Duplicate (UniCod, SalaCod) must conflict — save() would otherwise merge/upsert silently.
        if (salaRepo.existsById(new SalaId(uniCod, req.salaCodigo())))
            throw new Conflict("Já existe uma sala com o código " + req.salaCodigo() + " nesta unidade");

        Sala sala = new Sala();
        sala.setUniCod(uniCod);
        sala.setSalaCod(req.salaCodigo());
        sala.setNome(req.nome());
        sala.setStatus(req.status());
        stampSala(sala);

        Sala saved = salaRepo.save(sala);
        audit.record("CREATE", "SAU_UNISALA", uniCod);
        return toSalaResponse(saved);
    }

    @Transactional
    public SalaResponse updateSala(Integer uniCod, Short salaCodigo, SalaAtualizarRequest req) {
        SalaId id = new SalaId(uniCod, salaCodigo);
        Sala sala = salaRepo.findById(id)
                .orElseThrow(() -> new NotFound("Sala " + salaCodigo + " não encontrada"));
        sala.setNome(req.nome());
        sala.setStatus(req.status());
        stampSala(sala);

        Sala saved = salaRepo.save(sala);
        audit.record("UPDATE", "SAU_UNISALA", uniCod);
        return toSalaResponse(saved);
    }

    @Transactional
    public void deleteSala(Integer uniCod, Short salaCodigo) {
        SalaId id = new SalaId(uniCod, salaCodigo);
        if (!salaRepo.existsById(id))
            throw new NotFound("Sala " + salaCodigo + " não encontrada");
        salaRepo.deleteById(id);
        audit.record("DELETE", "SAU_UNISALA", uniCod);
    }

    // ── cascade helpers (called by UnidadeService.delete) ────────────────────

    @Transactional
    public void cascadeDeleteForUnidade(Integer uniCod) {
        salaRepo.deleteByUniCod(uniCod);           // R52
        nutricionistaRepo.deleteByUniCod(uniCod);  // R53
        sisPreNatalRepo.deleteByUniCod(uniCod);    // R54
        hiperdiaRepo.deleteByUniCod(uniCod);       // R55
    }

    // ── mappers ──────────────────────────────────────────────────────────────

    private HiperdiaResponse toHiperdiaResponse(HiperdiaProfissional h) {
        return new HiperdiaResponse(h.getUniCod(), h.getUniProPesCod(), h.getDatInclusao(),
                h.getMatricula(), h.getCbo(), h.getStatus(), h.getDatDesativacao());
    }

    private SisPreNatalResponse toSisPreNatalResponse(SisPreNatalProfissional s) {
        return new SisPreNatalResponse(s.getUniCod(), s.getUniGesProPesCod(), s.getUniGesEspCod(),
                s.getDatInclusao(), s.getStatus(), s.getDatDesativacao());
    }

    private NutricionistaResponse toNutricionistaResponse(NutricionistaProfissional n) {
        return new NutricionistaResponse(n.getUniCod(), n.getUniNutProPesCod(), n.getUniNutEspCod(),
                n.getDatInclusao(), n.getStatus(), n.getDatDesativacao());
    }

    private SalaResponse toSalaResponse(Sala s) {
        return new SalaResponse(s.getUniCod(), s.getSalaCod(), s.getNome(),
                s.getStatus(), s.getDatAlteracao(), s.getUsuLogin());
    }

    /** R82: auto-stamp SalaDatAlt and SalaUsuLogin. */
    private static void stampSala(Sala sala) {
        sala.setDatAlteracao(LocalDateTime.now());
        var auth = SecurityContextHolder.getContext().getAuthentication();
        sala.setUsuLogin(auth != null ? auth.getName() : "sistema");
    }
}
