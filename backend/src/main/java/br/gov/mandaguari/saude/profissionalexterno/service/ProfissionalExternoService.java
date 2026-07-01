package br.gov.mandaguari.saude.profissionalexterno.service;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.common.validation.CnsValidator;
import br.gov.mandaguari.saude.conselhoclasse.repository.ConselhoClasseRepository;
import br.gov.mandaguari.saude.pessoa.domain.Pessoa;
import br.gov.mandaguari.saude.pessoa.repository.PessoaRepository;
import br.gov.mandaguari.saude.pessoa.service.PessoaNomeValidator;
import br.gov.mandaguari.saude.profissional.domain.Profissional;
import br.gov.mandaguari.saude.profissional.repository.ProfissionalRepository;
import br.gov.mandaguari.saude.profissionalexterno.dto.ProfissionalExternoDtos.*;
import br.gov.mandaguari.saude.profissional.service.SoundexService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * SAU_PESF_PROFEXT ("Cadastro de Profissional Externo") — a LEAN composite create over two tables:
 * a person row in SYS_PES (PesTip=1) and an external professional row in SAU_PRO (ProExt=1). Rules
 * mined from {@code sau_pesf_profext_impl.java} + {@code psau_pesf_pro.java} (see SLICE-SPEC), cited
 * {@code // R<n>}.
 *
 * <p><b>Atomicity (intentional improvement, spec OQ):</b> the legacy commits SYS_PES then SAU_PRO in
 * two separate transactions (a failed SAU_PRO insert leaves an orphan person). Here both writes run in
 * ONE {@code @Transactional} so the pair is all-or-nothing. External professionals have NO certificate
 * (R30) — cert fields are left null.
 */
@Service
@Transactional(readOnly = true)
public class ProfissionalExternoService {

    private static final short EXTERNO = 1;   // R29
    private static final short ATIVO = 1;     // R29
    private static final int PESTIP_PROFISSIONAL = 1; // R24

    private final PessoaRepository pessoaRepo;
    private final ProfissionalRepository profissionalRepo;
    private final ConselhoClasseRepository conselhoRepo;
    private final SoundexService soundex;
    private final AuditService audit;

    public ProfissionalExternoService(PessoaRepository pessoaRepo, ProfissionalRepository profissionalRepo,
                                      ConselhoClasseRepository conselhoRepo, SoundexService soundex,
                                      AuditService audit) {
        this.pessoaRepo = pessoaRepo;
        this.profissionalRepo = profissionalRepo;
        this.conselhoRepo = conselhoRepo;
        this.soundex = soundex;
        this.audit = audit;
    }

    @Transactional
    public ProfissionalExternoResponse create(ProfissionalExternoCreateRequest req) {
        // R3-R7 name quality (R8/R9/R10 name micro-rules deferred as in SAU_PESF); R12 uppercase.
        PessoaNomeValidator.validateRequired(req.nome());
        String nome = req.nome().trim().toUpperCase();

        // R13 CNS required; R14-R16 check-digit/format valid.
        String cns = digits(req.cns());
        if (cns == null) throw new BusinessRule("profext.cns.required", "Informe o Número do CNS!");
        if (!CnsValidator.isValidCns(cns)) throw new BusinessRule("profext.cns.invalid", "CNS inválido! Tente novamente.");
        // R17 CNS unique among professionals (INS-only).
        var cnsOwner = profissionalRepo.findCnsOwner(cns, -1L);
        if (cnsOwner.isPresent()) {
            throw new Conflict("Este número de CNS está sendo utilizado pelo cadastro " + cnsOwner.get() + "! Por favor verifique.");
        }

        // R18 município required; R19 must exist.
        if (req.municipioCod() == null || req.municipioCod() == 0) {
            throw new BusinessRule("profext.municipio.required", "Informe o Município!");
        }
        if (!pessoaRepo.municipioExists(req.municipioCod())) {
            throw new BusinessRule("profext.municipio.notfound", "Não existe Município.");
        }

        // R20 nº do conselho required; R21 conselho required (+ exists — safe improvement over the legacy
        // soft lookup, which would insert a dangling FK; real data always comes from the picker).
        if (isBlank(req.numeroConselho())) throw new BusinessRule("profext.numeroConselho.required", "Informe o Número do Conselho de Classe!");
        if (req.conselhoClasseCod() == null || req.conselhoClasseCod() == 0) {
            throw new BusinessRule("profext.conselho.required", "Informe o Conselho de Classe!");
        }
        if (!conselhoRepo.existsById(req.conselhoClasseCod())) {
            throw new BusinessRule("profext.conselho.notfound", "Não existe Conselho de Classe.");
        }

        LocalDate hoje = LocalDate.now();
        String nomeSoundex = soundex.compute(nome);              // R26

        // R25 PesCod = MAX+1; R27 lean SYS_PES insert (PesTip=1).
        long pesCod = pessoaRepo.findMaxPesCod().map(max -> max + 1L).orElse(1L);
        Pessoa pessoa = new Pessoa();
        pessoa.setId(pesCod);
        pessoa.setTipoPessoa(PESTIP_PROFISSIONAL);               // R24
        pessoa.setNome(nome);                                    // R12
        pessoa.setNomeSoundex(nomeSoundex);                      // R26
        pessoa.setCns(cns);
        pessoa.setMunicipioCod(req.municipioCod());
        pessoa.setDataCadastro(hoje);                            // R23
        pessoaRepo.save(pessoa);

        // R28-R30 external professional (SAU_PRO, ProExt=1, no certificate).
        Profissional pro = new Profissional();
        pro.setId(pesCod);
        pro.setNumeroCns(cns);
        pro.setNomeSoundex(nomeSoundex);
        pro.setConselhoClasseCod(req.conselhoClasseCod());
        pro.setNumeroCr(req.numeroConselho().trim());
        pro.setDataInicio(hoje);
        pro.setDataFim(req.dataFim());
        pro.setExterno(EXTERNO);                                 // R29
        pro.setSituacao(ATIVO);                                  // R29
        profissionalRepo.save(pro);

        audit.record("CREATE", "SAU_PESF_PROFEXT", pesCod);      // R34 / LGPD
        return toResponse(pessoa, pro);
    }

    public ProfissionalExternoResponse get(Long id) {
        Pessoa pessoa = pessoaRepo.findById(id)
                .orElseThrow(() -> new NotFound("Profissional externo " + id + " não encontrado"));
        Profissional pro = profissionalRepo.findById(id)
                .orElseThrow(() -> new NotFound("Profissional externo " + id + " não encontrado"));
        audit.record("READ", "SAU_PESF_PROFEXT", id);            // LGPD
        return toResponse(pessoa, pro);
    }

    private static ProfissionalExternoResponse toResponse(Pessoa pessoa, Profissional pro) {
        return new ProfissionalExternoResponse(
                pessoa.getId(), pessoa.getNome(), pro.getNumeroCns(), pessoa.getMunicipioCod(),
                pro.getConselhoClasseCod(), pro.getNumeroCr(), pro.getDataInicio(), pro.getDataFim(),
                pro.getSituacao(), pro.getExterno());
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String digits(String s) { return (s == null || s.isBlank()) ? null : s.replaceAll("\\D", ""); }
}
