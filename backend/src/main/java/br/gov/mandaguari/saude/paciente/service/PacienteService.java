package br.gov.mandaguari.saude.paciente.service;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.common.validation.CnsValidator;
import br.gov.mandaguari.saude.common.validation.CpfValidator;
import br.gov.mandaguari.saude.paciente.domain.Paciente;
import br.gov.mandaguari.saude.paciente.dto.PacienteDtos.*;
import br.gov.mandaguari.saude.paciente.repository.PacienteRepository;
import br.gov.mandaguari.saude.pessoa.domain.Pessoa;
import br.gov.mandaguari.saude.pessoa.repository.PessoaRepository;
import br.gov.mandaguari.saude.pessoa.service.PessoaNomeValidator;
import br.gov.mandaguari.saude.profissional.service.SoundexService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Paciente (SAU_PAC) — the patient. Composite over the SYS_PES person supertype (person via {@code Pessoa})
 * plus the SAU_PAC subtype row. Person-level validations are INHERITED from SAU_PESF ({@code PessoaNomeValidator},
 * CPF/CNS); this service adds the patient-specific rules R1-R20 (see SLICE-SPEC), cited {@code // R<n>}.
 *
 * <p><b>PHI (most sensitive slice):</b> every read and every write is audited (common/audit). Delete is
 * guarded by SAU_RECESP (Portaria 344/98, R14) and hard-deletes only the SAU_PAC subtype (SYS_PES kept, R15).
 */
@Service
@Transactional(readOnly = true)
public class PacienteService {

    private static final int TIPO_FISICA = 1;      // R1: patients are always pessoa física
    private static final short OBITO_NAO = 0;      // R6
    private static final short SITUACAO_ATIVO = 1; // R7
    private static final int RENDA_MAX = 8;        // R8

    private final PacienteRepository repo;
    private final PessoaRepository pessoaRepo;
    private final SoundexService soundex;
    private final AuditService audit;

    public PacienteService(PacienteRepository repo, PessoaRepository pessoaRepo,
                           SoundexService soundex, AuditService audit) {
        this.repo = repo;
        this.pessoaRepo = pessoaRepo;
        this.soundex = soundex;
        this.audit = audit;
    }

    // --- reads ---

    public Page<PacienteListItem> search(String nome, String nomeMae, String prontuario,
                                         String cpf, String cns, Pageable pageable) {
        return repo.search(blank(nome), blank(nomeMae), blank(prontuario), digits(cpf), digits(cns), pageable)
                .map(p -> new PacienteListItem(p.getId(), p.getNome(), p.getNomeMae(), trim(p.getProntuario()),
                        trim(p.getCpfCnpj()), trim(p.getCns()), p.getDataNascimento(), p.getSituacao(), p.getObito()));
    }

    public List<PacienteListItem> lookup(String q, Pageable pageable) {
        return repo.lookup(q == null ? "" : q, pageable).stream()
                .map(p -> new PacienteListItem(p.getId(), p.getNome(), p.getNomeMae(), trim(p.getProntuario()),
                        trim(p.getCpfCnpj()), trim(p.getCns()), p.getDataNascimento(), p.getSituacao(), p.getObito()))
                .toList();
    }

    public PacienteResponse get(Long id) {
        Paciente pac = find(id);
        Pessoa pes = pessoaRepo.findById(id).orElseThrow(() -> new NotFound("Pessoa " + id + " não encontrada"));
        audit.record("READ", "SAU_PAC", id);                  // R17 / LGPD — PHI read
        return toResponse(pes, pac);
    }

    // --- writes ---

    @Transactional
    public PacienteResponse create(PacienteWriteRequest req) {
        validate(req, null);

        long id = pessoaRepo.findMaxPesCod().map(m -> m + 1L).orElse(1L);   // person PK (psau_inc_pes)
        Pessoa pes = new Pessoa();
        pes.setId(id);
        pes.setTipoPessoa(TIPO_FISICA);                                     // R1
        pes.setDataCadastro(LocalDate.now());
        applyPerson(pes, req);
        pessoaRepo.save(pes);

        Paciente pac = new Paciente();
        pac.setId(id);
        pac.setObito(req.obito() != null ? req.obito() : (int) OBITO_NAO);  // R6
        pac.setSituacao(req.situacao() != null ? req.situacao() : (int) SITUACAO_ATIVO); // R7
        applyPatient(pac, req, true);
        repo.save(pac);

        audit.record("CREATE", "SAU_PAC", id);                             // R17
        return toResponse(pes, pac);
    }

    @Transactional
    public PacienteResponse update(Long id, PacienteWriteRequest req) {
        Paciente pac = find(id);
        Pessoa pes = pessoaRepo.findById(id).orElseThrow(() -> new NotFound("Pessoa " + id + " não encontrada"));
        validate(req, id);

        applyPerson(pes, req);                                             // SYS_PES write-back (R15 keeps person)
        pessoaRepo.save(pes);
        if (req.situacao() != null) pac.setSituacao(req.situacao());
        if (req.obito() != null) pac.setObito(req.obito());
        applyPatient(pac, req, false);
        repo.save(pac);

        audit.record("UPDATE", "SAU_PAC", id);                            // R17
        return toResponse(pes, pac);
    }

    @Transactional
    public void delete(Long id) {
        Paciente pac = find(id);
        // R14 (Portaria 344/98): block while a controlled-substance prescription references the patient.
        if (repo.referencedByReceituarioControleEspecial(id)) {
            throw new Conflict("Paciente não pode ser excluído: possui Receituário de Controle Especial (Portaria 344/98)");
        }
        repo.delete(pac);                                                 // R15: hard-delete SAU_PAC only (SYS_PES kept)
        audit.record("DELETE", "SAU_PAC", id);                           // R17
    }

    // --- validation ---

    private void validate(PacienteWriteRequest req, Long selfId) {
        PessoaNomeValidator.validateRequired(req.nome());                 // name quality (inherited)
        PessoaNomeValidator.validateOptional(req.nomePai(), "pac.nomePai", "Nome do Pai");
        PessoaNomeValidator.validateOptional(req.nomeMae(), "pac.nomeMae", "Nome da Mãe");
        PessoaNomeValidator.validateSocial(req.nomeSocial());
        if (Boolean.TRUE.equals(req.usaNomeSocial()) && isBlank(req.nomeSocial())) {  // R13
            throw new BusinessRule("pac.nomeSocial.required", "Informe o Nome Social!");
        }

        String cpf = digits(req.cpfCnpj());
        String cns = digits(req.cns());
        // R2: CPF or CNS required, unless the unidade allows CPF-less registration.
        if (cpf == null && cns == null && !unidadeIsentaCpf(req.unidadeCod())) {
            throw new BusinessRule("pac.documento.required", "Informe o CPF ou CNS!");
        }
        if (cpf != null) {
            if (!CpfValidator.isValidCpf(cpf)) throw new BusinessRule("pac.cpf.invalid", "CPF inválido! Tente novamente.");
            var owners = pessoaRepo.findCpfOwners(cpf, selfId == null ? -1L : selfId, PageRequest.of(0, 1));
            if (!owners.isEmpty()) {
                throw new Conflict("Este número de CPF está sendo utilizado pelo cadastro " + owners.get(0) + "! Por favor verifique.");
            }
        }
        if (cns != null) {
            if (!CnsValidator.isValidCns(cns)) throw new BusinessRule("pac.cns.invalid", "CNS inválido! Tente novamente."); // R4
            var owner = repo.findCnsOwnerAmongPatients(cns, selfId == null ? -1L : selfId);                                 // R3
            if (owner.isPresent()) {
                throw new Conflict("Este número de CNS está sendo utilizado pelo paciente " + owner.get() + "! Por favor verifique.");
            }
        }
        if (req.unidadeCod() != null && req.unidadeCod() != 0 && !repo.unidadeExists(req.unidadeCod())) { // R5
            throw new BusinessRule("pac.unidade.notfound", "Não existe Unidade");
        }
        if (req.rendaFamiliar() != null && (req.rendaFamiliar() < 0 || req.rendaFamiliar() > RENDA_MAX)) { // R8
            throw new BusinessRule("pac.renda.range", "Campo Renda Familiar fora do intervalo");
        }
    }

    /** R2 exemption: the patient's unidade may allow CPF-less registration (SAU_UNI.UniCadCPF). */
    private boolean unidadeIsentaCpf(Integer unidadeCod) {
        return unidadeCod != null && unidadeCod != 0 && repo.unidadePermiteCadastroSemCpf(unidadeCod);
    }

    private void applyPerson(Pessoa pes, PacienteWriteRequest r) {
        pes.setNome(trim(r.nome()));
        pes.setNomeSocial(trim(r.nomeSocial()));
        pes.setUsaNomeSocial(r.usaNomeSocial());
        pes.setNomeMae(trim(r.nomeMae()));
        pes.setNomePai(trim(r.nomePai()));
        pes.setCpfCnpj(r.cpfCnpj());
        pes.setCns(r.cns());
        pes.setRgIe(r.rg());
        pes.setDataNascimento(r.dataNascimento());
        pes.setSexo(r.sexo());
        pes.setCep(r.cep());
        pes.setEndereco(trim(r.endereco()));
        pes.setEnderecoNumero(trim(r.numero()));
        pes.setEnderecoComplemento(trim(r.complemento()));
        pes.setBairroCod(r.bairroCod());
        pes.setMunicipioCod(r.municipioCod());
        pes.setTelefone(r.telefone());
        pes.setCelular(r.celular());
        pes.setEmail(r.email());
        pes.setEtniaCod(r.etniaCod());
        pes.setPaisCod(r.paisCod());
        pes.setCboCod(r.cboCod());
        pes.setNomeSoundex(soundex.compute(r.nome()));           // R12 (person copy)
        pes.setNomeMaeSoundex(soundex.compute(r.nomeMae()));
        pes.setNomeSocialSoundex(soundex.compute(r.nomeSocial()));
    }

    private void applyPatient(Paciente pac, PacienteWriteRequest r, boolean insert) {
        pac.setUnidadeCod(r.unidadeCod());
        pac.setProntuario(r.prontuario());                       // R19 free/optional
        pac.setNumeroIdentificacao(r.numeroIdentificacao());
        pac.setAlergia(r.alergia());
        pac.setHistoricoDoencas(r.historicoDoencas());
        pac.setInconsciente(r.inconsciente());
        pac.setSituacaoRua(r.situacaoRua());
        pac.setSurtoPsiquiatrico(r.surtoPsiquiatrico());
        pac.setRendaFamiliar(r.rendaFamiliar());
        pac.setMeioTransporte(r.meioTransporte());
        pac.setBeneficioSocial(r.beneficioSocial());
        pac.setCnh(r.cnh());
        // R12 soundex mirrors the person names.
        pac.setNomeSoundex(soundex.compute(r.nome()));
        pac.setNomeMaeSoundex(soundex.compute(r.nomeMae()));
        pac.setNomeSocialSoundex(soundex.compute(r.nomeSocial()));
        // R11 audit columns (login/date) from the security context.
        String actor = currentActor();
        pac.setDataUltimaAlteracao(LocalDateTime.now());
        pac.setUsuarioAlteracao(actor);
        if (insert) pac.setUsuarioInclusao(actor);
        // R9/R10 (PacPesCadInsUniCod / PacPesCadAltUniCod = session unidade) are DEFERRED: the JWT auth
        // model carries no session unidade (SLICE-SPEC OQ5). These columns stay null until a unidade is
        // available in the security context. R18 (PacDocumentoLGPD masking) is likewise deferred (OQ3) —
        // full CPF/CNS are returned only to the SAUDE_CADASTRO role that already has full patient access.
    }

    // --- helpers ---

    private Paciente find(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFound("Paciente " + id + " não encontrado"));
    }

    private static PacienteResponse toResponse(Pessoa pes, Paciente pac) {
        return new PacienteResponse(
                pes.getId(), pes.getNome(), pes.getNomeSocial(), Boolean.TRUE.equals(pes.getUsaNomeSocial()),
                pes.getNomeMae(), pes.getNomePai(), pes.getCpfCnpj(), pes.getCns(), pes.getRgIe(),
                pes.getDataNascimento(), pes.getSexo(), pes.getCep(), pes.getEndereco(), pes.getEnderecoNumero(),
                pes.getEnderecoComplemento(), pes.getBairroCod(), pes.getMunicipioCod(), pes.getTelefone(),
                pes.getCelular(), pes.getEmail(),
                pac.getUnidadeCod(), pac.getProntuario(), pac.getNumeroIdentificacao(), pac.getAlergia(),
                pac.getHistoricoDoencas(), pac.getObito(), pac.getInconsciente(), pac.getSituacaoRua(),
                pac.getSurtoPsiquiatrico(), pac.getRendaFamiliar(), pac.getMeioTransporte(),
                pac.getBeneficioSocial(), pac.getCnh(), pac.getSituacao());
    }

    private static String currentActor() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return (a != null && a.isAuthenticated()) ? a.getName() : "system";
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String trim(String s) { return s == null ? null : s.trim(); }
    private static String blank(String s) { return (s == null || s.isBlank()) ? null : s; }
    private static String digits(String s) { return (s == null || s.isBlank()) ? null : s.replaceAll("\\D", ""); }
}
