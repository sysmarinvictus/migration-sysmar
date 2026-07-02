package br.gov.mandaguari.saude.setor.web;

import br.gov.mandaguari.saude.setor.dto.UniSetorDtos.*;
import br.gov.mandaguari.saude.setor.service.UniSetorService;
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

/** REST surface for Setor da Unidade — replaces sau_unisetor transaction. */
@RestController
@RequestMapping("/api/unidades/{unidadeId}/setores")
@Tag(name = "Setores")
public class UniSetorController {

    private final UniSetorService service;

    public UniSetorController(UniSetorService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("@authz.can(authentication, 'SAU_UNISETOR', 'CON', 'SAUDE_CADASTRO')")
    @Operation(summary = "Listar setores de uma unidade (paginado)")
    public Page<UniSetorResponse> list(@PathVariable Integer unidadeId,
                                       @RequestParam(required = false) String nome,
                                       @PageableDefault(size = 20, sort = "setorCod") Pageable pageable) {
        return service.list(unidadeId, nome, pageable);
    }

    @GetMapping("/{setorId}")
    @PreAuthorize("@authz.can(authentication, 'SAU_UNISETOR', 'CON', 'SAUDE_CADASTRO')")
    @Operation(summary = "Obter setor por código composto")
    public UniSetorResponse get(@PathVariable Integer unidadeId, @PathVariable Integer setorId) {
        return service.get(unidadeId, setorId);
    }

    @GetMapping("/lookup")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Autocomplete de setores (FK picker)")
    public List<UniSetorLookupItem> lookup(@PathVariable Integer unidadeId,
                                           @RequestParam(required = false, defaultValue = "") String q,
                                           @PageableDefault(size = 10) Pageable pageable) {
        return service.lookup(unidadeId, q, pageable);
    }

    @PostMapping
    @PreAuthorize("@authz.can(authentication, 'SAU_UNISETOR', 'INC', 'SAUDE_CADASTRO')")
    @Operation(summary = "Criar setor para a unidade")
    public ResponseEntity<UniSetorResponse> create(@PathVariable Integer unidadeId,
                                                   @Valid @RequestBody UniSetorCreateRequest req,
                                                   UriComponentsBuilder uri) {
        UniSetorResponse created = service.create(unidadeId, req);
        URI location = uri.path("/api/unidades/{u}/setores/{s}")
                .buildAndExpand(unidadeId, created.setorCod()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{setorId}")
    @PreAuthorize("@authz.can(authentication, 'SAU_UNISETOR', 'ALT', 'SAUDE_CADASTRO')")
    @Operation(summary = "Atualizar setor")
    public UniSetorResponse update(@PathVariable Integer unidadeId,
                                   @PathVariable Integer setorId,
                                   @Valid @RequestBody UniSetorUpdateRequest req) {
        return service.update(unidadeId, setorId, req);
    }

    @DeleteMapping("/{setorId}")
    @PreAuthorize("@authz.can(authentication, 'SAU_UNISETOR', 'EXC', 'SAUDE_CADASTRO')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir setor")
    public void delete(@PathVariable Integer unidadeId, @PathVariable Integer setorId) {
        service.delete(unidadeId, setorId);
    }
}
