package br.gov.mandaguari.saude.pessoa.service;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.common.validation.CnsValidator;
import br.gov.mandaguari.saude.common.validation.CpfValidator;
import br.gov.mandaguari.saude.pessoa.domain.Pessoa;
import br.gov.mandaguari.saude.pessoa.dto.PessoaDtos.*;
import br.gov.mandaguari.saude.pessoa.repository.PessoaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Person supertype (SYS_PES) read + the CENTRALIZED person validators (CPF/CNS check-digit, person-wide
 * CPF uniqueness). Read-only: persons are created/edited through the subtype slices (SAU_PRO/SAU_FUN),
 * which keep their own native access — this slice is ADDITIVE and changes no subtype behavior. The
 * validators here are available for a future centralization decision (SYS_PES OQ3), not yet wired in.
 *
 * <p>Single-record PHI reads are audited (LGPD); search/lookup follow the SAU_PRO/SAU_FUN precedent.
 */
@Service
@Transactional(readOnly = true)
public class PessoaService {

    private final PessoaRepository repo;
    private final AuditService audit;

    public PessoaService(PessoaRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    public PessoaResponse get(Long id) {
        Pessoa p = repo.findById(id).orElseThrow(() -> new NotFound("Pessoa " + id + " não encontrada"));
        audit.record("READ", "SYS_PES", id);        // PHI read
        return toResponse(p);
    }

    public Page<PessoaResponse> search(String nome, String cpf, String cns, Pageable pageable) {
        return repo.search(blank(nome), digits(cpf), digits(cns), pageable).map(PessoaService::toResponse);
    }

    public List<PessoaLookupItem> lookup(String q, Pageable pageable) {
        return repo.lookup(q == null ? "" : q, pageable).stream()
                .map(p -> new PessoaLookupItem(p.getId(), p.getNomeExibicao())).toList();
    }

    // --- centralized person validators (additive; reuse the shared check-digit logic) ---

    /** CPF/CNPJ check-digit validity (delegates to the shared {@link CpfValidator}). */
    public boolean cpfValido(String cpfCnpj) {
        return cpfCnpj == null || cpfCnpj.isBlank() || CpfValidator.isValidCpf(digits(cpfCnpj));
    }

    /** CNS check-digit validity (delegates to the shared {@link CnsValidator}). */
    public boolean cnsValido(String cns) {
        return cns == null || cns.isBlank() || CnsValidator.isValidCns(digits(cns));
    }

    /** R17: CPF/CNPJ available person-wide (no OTHER person already uses it). */
    public boolean cpfDisponivel(String cpfCnpj, Long selfId) {
        if (cpfCnpj == null || cpfCnpj.isBlank()) return true;
        return repo.findCpfOwners(digits(cpfCnpj), selfId == null ? -1L : selfId, PageRequest.of(0, 1)).isEmpty();
    }

    // --- helpers ---

    static PessoaResponse toResponse(Pessoa p) {
        return new PessoaResponse(
                p.getId(), p.getNome(), p.getNomeSocial(), Boolean.TRUE.equals(p.getUsaNomeSocial()),
                p.getNomeExibicao(), p.getNomeCompleto(), p.getCpfCnpj(), p.getCns(),
                p.getDataNascimento(), p.getSexo(), p.getTelefone(), p.getCelular(), p.getEmail());
    }

    private static String blank(String s) { return (s == null || s.isBlank()) ? null : s; }
    private static String digits(String s) { return (s == null || s.isBlank()) ? null : s.replaceAll("\\D", ""); }
}
