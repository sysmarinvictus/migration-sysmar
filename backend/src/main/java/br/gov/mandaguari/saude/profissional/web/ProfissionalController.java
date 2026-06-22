package br.gov.mandaguari.saude.profissional.web;

import br.gov.mandaguari.saude.profissional.dto.ProfissionalDtos.*;
import br.gov.mandaguari.saude.profissional.service.ProfissionalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * REST surface for Profissional — replaces the GeneXus sau_pro transaction + hwwsau_pro/hviewsau_pro/
 * hpromptsau_pro. NO endpoint here returns certificado, assinaturaImagem or certificadoSenha (R31;
 * SLICE-SPEC §Security). Auth uses the coarse {@code SAUDE_CADASTRO} role established by prior slices
 * (finer per-mode mapping pending OQ6).
 */
@RestController
@RequestMapping("/api/profissionais")
@Tag(name = "Profissionais")
public class ProfissionalController {

    private final ProfissionalService service;

    public ProfissionalController(ProfissionalService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @Operation(summary = "Listar/buscar profissionais (paginado). Nome = LIKE em SYS_PES.PesNom (R16).")
    public Page<ProfissionalResponse> list(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String cpfCnpj,
            @RequestParam(required = false) String numeroCns,
            @RequestParam(required = false) Short externo,
            @RequestParam(required = false) Short situacao,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.list(id, nome, cpfCnpj, numeroCns, externo, situacao, pageable);
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @Operation(summary = "Autocomplete de profissionais (seletor de prescritor)")
    public List<ProfissionalLookupItem> lookup(@RequestParam(required = false, defaultValue = "") String q,
                                               @PageableDefault(size = 20) Pageable pageable) {
        return service.lookup(q, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @Operation(summary = "Obter profissional por código (PesCod). Não retorna certificado/senha.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Encontrado"),
            @ApiResponse(responseCode = "404", description = "Não encontrado")
    })
    public ProfissionalResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @Operation(summary = "Cadastrar profissional. O corpo INCLUI id (código da Pessoa) — não é gerado (R1).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Criado"),
            @ApiResponse(responseCode = "422", description = "Violação de regra de negócio")
    })
    public ResponseEntity<ProfissionalResponse> create(@Valid @RequestBody ProfissionalCreateRequest req,
                                                        UriComponentsBuilder uri) {
        ProfissionalResponse created = service.create(req);
        URI location = uri.path("/api/profissionais/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @Operation(summary = "Atualizar profissional (pode gravar de volta nome/cpf/telefones em SYS_PES, R2)")
    public ProfissionalResponse update(@PathVariable Long id,
                                       @Valid @RequestBody ProfissionalUpdateRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir profissional (bloqueado por 9 referências, R20-R26)")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
