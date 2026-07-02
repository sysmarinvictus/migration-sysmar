package br.gov.mandaguari.saude.pessoa.web;

import br.gov.mandaguari.saude.pessoa.dto.PessoaDtos.*;
import br.gov.mandaguari.saude.pessoa.service.PessoaCadastroService;
import br.gov.mandaguari.saude.pessoa.service.PessoaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Person API over the SYS_PES supertype. Read/resolution (get/search/lookup) plus the SAU_PESF cadastro
 * write path (create/update/delete — the transaction that feeds the SAU_PRO/SAU_FUN subtypes). PHI →
 * SAUDE_CADASTRO + audited; secrets (PesSenha) are unmapped and never exposed. Subtype auto-provisioning
 * is out of scope (SAU_PESF OQ1).
 */
@RestController
@RequestMapping("/api/pessoas")
@Tag(name = "Pessoas")
public class PessoaController {

    private final PessoaService service;
    private final PessoaCadastroService cadastro;

    public PessoaController(PessoaService service, PessoaCadastroService cadastro) {
        this.service = service;
        this.cadastro = cadastro;
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.can(authentication, 'SAU_PESF', 'CON', 'SAUDE_CADASTRO')")
    @Operation(summary = "Obter pessoa por código (PesCod) — honra o nome social (LGPD)")
    public PessoaResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/search")
    @PreAuthorize("@authz.can(authentication, 'SAU_PESF', 'CON', 'SAUDE_CADASTRO')")
    @Operation(summary = "Buscar pessoas por nome (registro/social), CPF/CNPJ ou CNS")
    public Page<PessoaResponse> search(@RequestParam(required = false) String nome,
                                       @RequestParam(required = false) String cpf,
                                       @RequestParam(required = false) String cns,
                                       @PageableDefault(size = 20) Pageable pageable) {
        return service.search(nome, cpf, cns, pageable);
    }

    @GetMapping("/lookup")
    @PreAuthorize("@authz.can(authentication, 'SAU_PESF', 'CON', 'SAUDE_CADASTRO')")
    @Operation(summary = "Autocomplete de pessoas (resolução p/ formulários de subtipo)")
    public List<PessoaLookupItem> lookup(@RequestParam(required = false, defaultValue = "") String q,
                                         @PageableDefault(size = 10) Pageable pageable) {
        return service.lookup(q, pageable);
    }

    // --- SAU_PESF cadastro write path ---

    @GetMapping("/{id}/cadastro")
    @PreAuthorize("@authz.can(authentication, 'SAU_PESF', 'CON', 'SAUDE_CADASTRO')")
    @Operation(summary = "Obter o cadastro completo de uma pessoa (p/ edição)")
    public PessoaCadastroResponse getCadastro(@PathVariable Long id) {
        return cadastro.getCadastro(id);
    }

    @PostMapping
    @PreAuthorize("@authz.can(authentication, 'SAU_PESF', 'INC', 'SAUDE_CADASTRO')")
    @Operation(summary = "Cadastrar pessoa física (SAU_PESF) — valida CPF/CNS, endereço, nacionalidade, etc.")
    public ResponseEntity<PessoaCadastroResponse> create(@RequestBody PessoaCadastroRequest req,
                                                         UriComponentsBuilder uri) {
        PessoaCadastroResponse created = cadastro.create(req);
        URI location = uri.path("/api/pessoas/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.can(authentication, 'SAU_PESF', 'ALT', 'SAUDE_CADASTRO')")
    @Operation(summary = "Atualizar o cadastro de uma pessoa física")
    public PessoaCadastroResponse update(@PathVariable Long id, @RequestBody PessoaCadastroRequest req) {
        return cadastro.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.can(authentication, 'SAU_PESF', 'EXC', 'SAUDE_CADASTRO')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir pessoa (bloqueado se há Profissional/Funcionário/Paciente vinculado)")
    public void delete(@PathVariable Long id) {
        cadastro.delete(id);
    }
}
