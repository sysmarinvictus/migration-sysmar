package br.gov.mandaguari.saude.funcionario.service;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.funcionario.domain.Funcionario;
import br.gov.mandaguari.saude.funcionario.dto.FuncionarioDtos.*;
import br.gov.mandaguari.saude.funcionario.repository.FuncionarioRepository;
import br.gov.mandaguari.saude.funcionario.repository.PersonProjection;
import br.gov.mandaguari.saude.profissional.service.SoundexService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Funcionário business logic — rules mined from {@code sau_fun_impl.java} (see SLICE-SPEC SAU_FUN). A
 * SYS_PES person-subtype like SAU_PRO. Notable divergences from SAU_PRO: <b>no CPF/CNS check-digit
 * validation</b> (R12), and the legacy had <b>no</b> audit — we add {@code common/audit} as an LGPD
 * carry-forward (R17). Person name is never logged (R18).
 */
@Service
@Transactional(readOnly = true)
public class FuncionarioService {

    /** Phone format (R6/R7/R8): {@code (NN) [9]NNNN-NNNN}. */
    private static final Pattern PHONE = Pattern.compile("^\\([0-9]{2}\\)\\s[9]?[0-9]{4}-[0-9]{4}$");
    private static final short ATIVO = 1;

    private final FuncionarioRepository repo;
    private final SoundexService soundex;
    private final AuditService audit;

    public FuncionarioService(FuncionarioRepository repo, SoundexService soundex, AuditService audit) {
        this.repo = repo;
        this.soundex = soundex;
        this.audit = audit;
    }

    public Page<FuncionarioResponse> list(Long id, String nome, String cpfCnpj, Short situacao, Pageable pageable) {
        return repo.search(id, blank(nome), blank(cpfCnpj), situacao, pageable).map(this::toResponse);
    }

    public FuncionarioResponse get(Long id) {
        Funcionario f = find(id);
        audit.record("READ", "SAU_FUN", id);   // R17/LGPD: audit single-record PHI read (mirrors SAU_PRO).
        return toResponse(f);                   // list/lookup follow the SAU_PRO precedent (not per-call audited).
    }

    public List<FuncionarioLookupItem> lookup(String q, Pageable pageable) {
        return repo.lookup(q == null ? "" : q, pageable).stream()
                .map(r -> new FuncionarioLookupItem(r.getId(), r.getNome())).toList();
    }

    @Transactional
    public FuncionarioResponse create(FuncionarioCreateRequest req) {
        Long id = req.id();
        if (!repo.personExists(id)) {                                   // R1
            throw new BusinessRule("funcionario.pessoa.inexistente",
                    "Não existe Pessoa para o código informado");
        }
        if (repo.existsById(id)) {                                      // subtype INS only once
            throw new Conflict("Já existe Funcionário para esta Pessoa");
        }
        validatePhones(req.telefoneTrabalho(), req.pessoa());          // R6/R7/R8

        Funcionario f = new Funcionario();
        f.setId(id);
        f.setTelefoneTrabalho(blank(req.telefoneTrabalho()));
        f.setRamal(blank(req.ramal()));                                // R9: free text, no validation
        f.setSituacao(req.situacao() != null ? req.situacao() : ATIVO); // R5: default Ativo
        f.setNomeSoundex(soundex.compute(resolveNome(id, req.pessoa()))); // R3

        repo.save(f);
        writeBackPerson(id, req.pessoa());                             // R2
        audit.record("CREATE", "SAU_FUN", id);                        // R17 (carry-forward)
        return toResponse(f);
    }

    @Transactional
    public FuncionarioResponse update(Long id, FuncionarioUpdateRequest req) {
        Funcionario f = find(id);
        validatePhones(req.telefoneTrabalho(), req.pessoa());          // R6/R7/R8

        f.setTelefoneTrabalho(blank(req.telefoneTrabalho()));
        f.setRamal(blank(req.ramal()));
        if (req.situacao() != null) f.setSituacao(req.situacao());     // keep existing when omitted
        f.setNomeSoundex(soundex.compute(resolveNome(id, req.pessoa()))); // R3 recompute

        repo.save(f);
        writeBackPerson(id, req.pessoa());                             // R2
        audit.record("UPDATE", "SAU_FUN", id);
        return toResponse(f);
    }

    @Transactional
    public void delete(Long id) {
        Funcionario f = find(id);
        if (repo.hasSystemUser(id)) {                                  // R13
            throw new Conflict("Funcionário está vinculado a um usuário do sistema e não pode ser excluído");
        }
        if (repo.hasControlledPrescription(id)) {                      // R14 (Portaria 344/98)
            throw new Conflict("Funcionário possui receituário de controle especial e não pode ser excluído; inative-o");
        }
        repo.delete(f);
        audit.record("DELETE", "SAU_FUN", id);
    }

    // --- helpers ---

    private Funcionario find(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFound("Funcionário " + id + " não encontrado"));
    }

    private FuncionarioResponse toResponse(Funcionario f) {
        PersonProjection p = repo.findPerson(f.getId()).orElse(null);
        return new FuncionarioResponse(
                f.getId(), f.getTelefoneTrabalho(), trim(f.getRamal()), f.getSituacao(),
                p == null ? null : p.getNome(),
                p == null ? null : trim(p.getCpfCnpj()),
                p == null ? null : p.getTelefone(),
                p == null ? null : p.getCelular());
    }

    /** Soundex source name: prefer the submitted person name, else the current SYS_PES name (R3). */
    private String resolveNome(Long id, PessoaSubRequest pessoa) {
        if (pessoa != null && pessoa.nome() != null && !pessoa.nome().isBlank()) return pessoa.nome();
        return repo.findPerson(id).map(PersonProjection::getNome).orElse(null);
    }

    private void writeBackPerson(Long id, PessoaSubRequest pessoa) {   // R2
        if (pessoa == null) return;
        repo.updatePerson(id, pessoa.nome(), pessoa.cpfCnpj(), pessoa.telefone(), pessoa.celular());
    }

    private void validatePhones(String telefoneTrabalho, PessoaSubRequest pessoa) {
        validatePhone(telefoneTrabalho, "Telefone de trabalho inválido!");   // R8
        if (pessoa != null) {
            validatePhone(pessoa.telefone(), "Telefone inválido!");          // R6
            validatePhone(pessoa.celular(), "Celular inválido!");            // R7
        }
    }

    private static void validatePhone(String value, String message) {
        if (value != null && !value.isBlank() && !PHONE.matcher(value.trim()).matches()) {
            throw new BusinessRule("funcionario.telefone.invalido", message);
        }
    }

    private static String trim(String s) { return s == null ? null : s.trim(); }
    private static String blank(String s) { return (s == null || s.isBlank()) ? null : s; }
}
