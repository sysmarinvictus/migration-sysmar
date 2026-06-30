package br.gov.mandaguari.saude.pessoa.web;

import br.gov.mandaguari.saude.pessoa.dto.PessoaDtos.*;
import br.gov.mandaguari.saude.pessoa.service.PessoaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Read-only person-resolution API over the SYS_PES supertype. NO mutation surface — persons are
 * created/edited through the subtype slices (SAU_PRO/SAU_FUN/SAU_PAC). PHI → SAUDE_CADASTRO + audited;
 * secrets (PesSenha) are unmapped and never exposed.
 */
@RestController
@RequestMapping("/api/pessoas")
@Tag(name = "Pessoas")
@PreAuthorize("hasRole('SAUDE_CADASTRO')")
public class PessoaController {

    private final PessoaService service;

    public PessoaController(PessoaService service) { this.service = service; }

    @GetMapping("/{id}")
    @Operation(summary = "Obter pessoa por código (PesCod) — honra o nome social (LGPD)")
    public PessoaResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar pessoas por nome (registro/social), CPF/CNPJ ou CNS")
    public Page<PessoaResponse> search(@RequestParam(required = false) String nome,
                                       @RequestParam(required = false) String cpf,
                                       @RequestParam(required = false) String cns,
                                       @PageableDefault(size = 20) Pageable pageable) {
        return service.search(nome, cpf, cns, pageable);
    }

    @GetMapping("/lookup")
    @Operation(summary = "Autocomplete de pessoas (resolução p/ formulários de subtipo)")
    public List<PessoaLookupItem> lookup(@RequestParam(required = false, defaultValue = "") String q,
                                         @PageableDefault(size = 10) Pageable pageable) {
        return service.lookup(q, pageable);
    }
}
