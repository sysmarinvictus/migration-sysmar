package br.gov.mandaguari.saude.profissional.service;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.common.validation.CnsValidator;
import br.gov.mandaguari.saude.common.validation.CpfValidator;
import br.gov.mandaguari.saude.conselhoclasse.domain.ConselhoClasse;
import br.gov.mandaguari.saude.conselhoclasse.repository.ConselhoClasseRepository;
import br.gov.mandaguari.saude.profissional.domain.Profissional;
import br.gov.mandaguari.saude.profissional.dto.ProfissionalDtos.*;
import br.gov.mandaguari.saude.profissional.mapper.ProfissionalMapper;
import br.gov.mandaguari.saude.profissional.repository.PersonProjection;
import br.gov.mandaguari.saude.profissional.repository.ProfissionalRepository;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profissional business logic — rules mined from {@code sau_pro_impl.java} (+ psau_soundex,
 * psau_val_cns, psau_val_cnpjcpf, psau_inc_log). Each rule is cited as {@code // R<n>}.
 *
 * <p>v1 scope: the signing certificate, signature image and certificate password are NOT part of the
 * create/update surface (SLICE-SPEC §Security). Those columns are left null/untouched here.
 */
@Service
@Transactional(readOnly = true)
public class ProfissionalService {

    /** R8/R9: phone & mobile format — verbatim regex from sau_pro_impl.java:2433-2446. */
    private static final Pattern PHONE = Pattern.compile("^(\\([0-9]{2}\\))\\s([9]{1})?([0-9]{4})-([0-9]{4})$");

    private static final short SITUACAO_ATIVO = 1;   // R12
    private static final short EXTERNO_DEFAULT = 0;  // R13

    private final ProfissionalRepository repo;
    private final ConselhoClasseRepository conselhoRepo;
    private final ProfissionalMapper mapper;
    private final SoundexService soundex;
    private final AuditService audit;

    public ProfissionalService(ProfissionalRepository repo, ConselhoClasseRepository conselhoRepo,
                               ProfissionalMapper mapper, SoundexService soundex, AuditService audit) {
        this.repo = repo;
        this.conselhoRepo = conselhoRepo;
        this.mapper = mapper;
        this.soundex = soundex;
        this.audit = audit;
    }

    // ── Queries ──────────────────────────────────────────────────────────────────────────────────

    public Page<ProfissionalResponse> list(Long id, String nome, String cpfCnpj, String numeroCns,
                                            Short externo, Short situacao, Pageable pageable) {
        // R16: name filter is a literal LIKE on SYS_PES.PesNom (NOT the soundex column).
        Page<Profissional> page = repo.search(id, blankToNull(nome), blankToNull(cpfCnpj),
                blankToNull(numeroCns), externo, situacao, pageable);
        return page.map(this::enrich);
    }

    public ProfissionalResponse get(Long id) {
        ProfissionalResponse resp = enrich(find(id));
        // PHI read of a single professional record (CNS / person data) → audit.
        audit.record("READ", "SAU_PRO", id); // R17 (+ real id, fix R18)
        return resp;
    }

    public List<ProfissionalLookupItem> lookup(String q, Pageable pageable) {
        return repo.search(null, blankToNull(q), null, null, null, null, pageable)
                .map(p -> {
                    PersonProjection person = repo.findPerson(p.getId()).orElse(null);
                    String sigla = siglaOf(p.getConselhoClasseCod());
                    return new ProfissionalLookupItem(p.getId(),
                            person != null ? person.getNome() : null, p.getNumeroCns(), sigla);
                }).getContent();
    }

    // ── Writes ───────────────────────────────────────────────────────────────────────────────────

    @Transactional
    public ProfissionalResponse create(ProfissionalCreateRequest req) {
        Long id = req.id();
        // R1: the person (SYS_PES) must already exist — SAU_PRO never auto-creates SYS_PES.
        if (id == null || !repo.personExists(id)) {
            throw new BusinessRule("pro.pessoa.notfound", "Não existe Profissional"); // legacy msg 101/ForeignKeyNotFound
        }
        if (repo.existsById(id)) {
            throw new BusinessRule("pro.duplicate", "Profissional " + id + " já cadastrado");
        }

        validateIdentifiers(req.numeroCns(), req.pessoa(), id);
        validateConselho(req.conselhoClasseCod()); // R10

        Profissional p = new Profissional();
        p.setId(id);
        p.setNumeroCns(req.numeroCns());
        p.setNumeroCr(req.numeroCr());            // R14: no validation
        p.setUfConselho(req.ufConselho());
        p.setConselhoClasseCod(req.conselhoClasseCod());
        p.setDataInicio(req.dataInicio());        // R14: no datini<=datfim check
        p.setDataFim(req.dataFim());
        p.setCnesId(req.cnesId());
        // R12/R13 defaults on INS
        p.setSituacao(req.situacao() != null ? req.situacao() : SITUACAO_ATIVO);
        p.setExterno(req.externo() != null ? req.externo() : EXTERNO_DEFAULT);
        p.setExportaEsus(req.exportaEsus() != null ? req.exportaEsus() : Boolean.FALSE);

        writeBackPersonAndSoundex(id, req.pessoa()); // R2 + R15
        applySoundex(p, id);

        Profissional saved = repo.save(p);
        audit.record("CREATE", "SAU_PRO", saved.getId()); // R17 + real id (fix R18)
        return enrich(saved);
    }

    @Transactional
    public ProfissionalResponse update(Long id, ProfissionalUpdateRequest req) {
        Profissional p = find(id);

        validateIdentifiers(req.numeroCns(), req.pessoa(), id);
        validateConselho(req.conselhoClasseCod()); // R10

        p.setNumeroCns(req.numeroCns());
        p.setNumeroCr(req.numeroCr());
        p.setUfConselho(req.ufConselho());
        p.setConselhoClasseCod(req.conselhoClasseCod());
        p.setDataInicio(req.dataInicio());
        p.setDataFim(req.dataFim());
        p.setCnesId(req.cnesId());
        if (req.situacao() != null) p.setSituacao(req.situacao());
        if (req.externo() != null) p.setExterno(req.externo());
        if (req.exportaEsus() != null) p.setExportaEsus(req.exportaEsus());

        writeBackPersonAndSoundex(id, req.pessoa()); // R2
        applySoundex(p, id);                         // R15

        Profissional saved = repo.save(p);
        audit.record("UPDATE", "SAU_PRO", saved.getId()); // R17 + real id (fix R18)
        return enrich(saved);
    }

    @Transactional
    public void delete(Long id) {
        Profissional p = find(id);
        // R19-R26: block delete when referenced anywhere. Portaria 344/98 (R26): a prescriber with
        // controlled-substance history should be soft-deactivated, never hard-deleted.
        if (repo.hasSpecialty(id))              throw referenced("possui especialidade(s) cadastrada(s)");  // R20
        if (repo.hasSystemUser(id))             throw referenced("está vinculado a um usuário do sistema"); // R21
        if (repo.hasUniNut(id))                 throw referenced("está vinculado a Uni Nut Pro Pes");       // R22
        if (repo.hasSisprenatal(id))            throw referenced("possui registros no SISPRENATAL");        // R23
        if (repo.hasHiperdia(id))               throw referenced("possui registros no HIPERDIA");           // R24
        if (repo.hasUnidadeRole(id))            throw referenced("exerce um papel em uma Unidade");         // R25
        if (repo.hasControlledPrescription(id)) throw referenced("possui receituário de controle especial (Portaria 344/98)"); // R26

        repo.delete(p);
        audit.record("DELETE", "SAU_PRO", id); // R17 + real id (fix R18)
    }

    // ── Rule helpers ──────────────────────────────────────────────────────────────────────────────

    private void validateIdentifiers(String numeroCns, PessoaSubRequest pessoa, Long selfId) {
        // R3: CNS required.
        String cns = numeroCns == null ? null : numeroCns.trim();
        if (cns == null || cns.isBlank()) {
            throw new BusinessRule("pro.cns.required", "Informe o Número do CNS!");
        }
        // R4: CNS check-digit/format (15-digit mod-11). NOTE: exact regulatory algorithm pending
        //     OQ-CNS sign-off; reuses the shared CnsValidator (PSAU_VER_CNS port).
        if (!CnsValidator.isValidCns(cns.replaceAll("\\D", ""))) {
            throw new BusinessRule("pro.cns.invalid", "CNS inválido");
        }
        // R5: CNS unique across people (excludes current id).
        repo.findCnsOwner(cns, selfId).ifPresent(owner -> {
            throw new BusinessRule("pro.cns.duplicate",
                    "Este número de CNS está sendo utilizado pelo cadastro " + owner + "!");
        });

        if (pessoa != null) {
            String cpf = pessoa.cpfCnpj();
            if (cpf != null && !cpf.isBlank()) {
                String digits = cpf.replaceAll("\\D", "");
                // R6: CPF validated only when present (CPF/CNPJ — 11 digits treated as CPF here).
                if (digits.length() == 11 && !CpfValidator.isValidCpf(digits)) {
                    throw new BusinessRule("pro.cpf.invalid", "CPF inválido!");
                }
                // R7: CPF/CNPJ unique across people (excludes current id).
                repo.findCpfCnpjOwner(cpf, selfId).ifPresent(owner -> {
                    throw new BusinessRule("pro.cpf.duplicate",
                            "Este número de CPF está sendo utilizado pelo cadastro " + owner + "!");
                });
            }
            // R8: phone format when present.
            if (isPresent(pessoa.telefone()) && !PHONE.matcher(pessoa.telefone().trim()).matches()) {
                throw new BusinessRule("pro.telefone.invalid", "Telefone inválido!");
            }
            // R9: mobile format when present.
            if (isPresent(pessoa.celular()) && !PHONE.matcher(pessoa.celular().trim()).matches()) {
                throw new BusinessRule("pro.celular.invalid", "Celular inválido!");
            }
        }
        // R11: SYS_PES extended-table lookups (TipLog/Mun/Bai/Etn/Pais/OrgEmi) are best-effort and
        //      belong to the Wave-0 SYS_PES slice; not enforced here (person panel is out of scope, OQ7).
    }

    /** R10: ConClaCod must exist in SAU_CONCLA when != 0; 0/null = none (optional). */
    private void validateConselho(Short conclacod) {
        if (conclacod != null && conclacod != 0 && !conselhoRepo.existsById(conclacod)) {
            throw new BusinessRule("pro.conselho.notfound", "Não existe Conselho de Classe.");
        }
    }

    /** R2: write back the editable person fields (name/cpf/phones) to SYS_PES. */
    private void writeBackPersonAndSoundex(Long id, PessoaSubRequest pessoa) {
        if (pessoa == null) return;
        repo.updatePerson(id, pessoa.nome(), pessoa.cpfCnpj(), pessoa.telefone(), pessoa.celular());
    }

    /** R15: recompute the phonetic key from the current SYS_PES name on every confirm. */
    private void applySoundex(Profissional p, Long id) {
        String nome = repo.findPerson(id).map(PersonProjection::getNome).orElse(null);
        p.setNomeSoundex(soundex.compute(nome));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────────────────────

    private Profissional find(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFound("Profissional " + id + " não encontrado"));
    }

    private BusinessRule referenced(String why) {
        // Maps to the legacy "CannotDeleteReferencedRecord" — handled as 422 by GlobalExceptionHandler.
        return new BusinessRule("pro.delete.referenced", "Não é possível excluir: o profissional " + why + ".");
    }

    private ProfissionalResponse enrich(Profissional p) {
        ProfissionalResponse base = mapper.toResponse(p);
        PersonProjection person = repo.findPerson(p.getId()).orElse(null);
        String nome = person != null ? person.getNome() : null;
        String cpfCnpj = person != null ? person.getCpfCnpj() : null;
        String rgIe = person != null ? person.getRgIe() : null;
        String sexo = person != null ? person.getSexo() : null;
        var dataNasc = person != null ? person.getDataNascimento() : null;
        String endereco = person != null ? person.getEndereco() : null;
        String telefone = person != null ? person.getTelefone() : null;
        String celular = person != null ? person.getCelular() : null;

        ConselhoClasse cc = (p.getConselhoClasseCod() != null && p.getConselhoClasseCod() != 0)
                ? conselhoRepo.findById(p.getConselhoClasseCod()).orElse(null) : null;

        return new ProfissionalResponse(
                base.id(), base.numeroCns(), base.numeroCr(), base.ufConselho(),
                base.conselhoClasseCod(), cc != null ? cc.getNome() : null, cc != null ? cc.getSigla() : null,
                base.dataInicio(), base.dataFim(), base.cnesId(), base.exportaEsus(), base.externo(), base.situacao(),
                nome, cpfCnpj, rgIe, sexo, dataNasc, endereco, telefone, celular);
    }

    private String siglaOf(Short conclacod) {
        if (conclacod == null || conclacod == 0) return null;
        return conselhoRepo.findById(conclacod).map(ConselhoClasse::getSigla).orElse(null);
    }

    private static boolean isPresent(String s) { return s != null && !s.isBlank(); }

    private static String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
}
