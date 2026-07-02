package br.gov.mandaguari.saude.funcionario.web;

import br.gov.mandaguari.saude.funcionario.dto.FuncionarioDtos.*;
import br.gov.mandaguari.saude.funcionario.service.FuncionarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
 * REST surface for Funcionário — replaces the GeneXus sau_fun transaction + hwwsau_fun/hviewsau_fun/
 * hpromptsau_fun. SYS_PES person-subtype (id = PesCod). Coarse {@code SAUDE_CADASTRO} role (R16; finer
 * per-mode mapping pending OQ5). The lookup returns person names (PII) → also SAUDE_CADASTRO, never
 * isAuthenticated.
 */
@RestController
@RequestMapping("/api/funcionarios")
@Tag(name = "Funcionários")
public class FuncionarioController {

    private final FuncionarioService service;

    public FuncionarioController(FuncionarioService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("@authz.can(authentication, 'SAU_FUN', 'CON', 'SAUDE_CADASTRO')")
    @Operation(summary = "Listar/buscar funcionários (paginado). Nome = LIKE em SYS_PES.PesNom.")
    public Page<FuncionarioResponse> list(@RequestParam(required = false) Long id,
                                          @RequestParam(required = false) String nome,
                                          @RequestParam(required = false) String cpfCnpj,
                                          @RequestParam(required = false) Short situacao,
                                          @PageableDefault(size = 20) Pageable pageable) {
        return service.list(id, nome, cpfCnpj, situacao, pageable);
    }

    @GetMapping("/lookup")
    @PreAuthorize("@authz.can(authentication, 'SAU_FUN', 'CON', 'SAUDE_CADASTRO')")
    @Operation(summary = "Autocomplete de funcionários (seletor)")
    public List<FuncionarioLookupItem> lookup(@RequestParam(required = false, defaultValue = "") String q,
                                              @PageableDefault(size = 20) Pageable pageable) {
        return service.lookup(q, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.can(authentication, 'SAU_FUN', 'CON', 'SAUDE_CADASTRO')")
    @Operation(summary = "Obter funcionário por código (PesCod)")
    public FuncionarioResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("@authz.can(authentication, 'SAU_FUN', 'INC', 'SAUDE_CADASTRO')")
    @Operation(summary = "Cadastrar funcionário. O corpo INCLUI id (código da Pessoa) — não é gerado (R1).")
    public ResponseEntity<FuncionarioResponse> create(@Valid @RequestBody FuncionarioCreateRequest req,
                                                      UriComponentsBuilder uri) {
        FuncionarioResponse created = service.create(req);
        URI location = uri.path("/api/funcionarios/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.can(authentication, 'SAU_FUN', 'ALT', 'SAUDE_CADASTRO')")
    @Operation(summary = "Atualizar funcionário (grava de volta nome/cpf/telefones em SYS_PES, R2)")
    public FuncionarioResponse update(@PathVariable Long id, @Valid @RequestBody FuncionarioUpdateRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.can(authentication, 'SAU_FUN', 'EXC', 'SAUDE_CADASTRO')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir funcionário (bloqueado por SAU_USU/SAU_RECESP, R13/R14)")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
