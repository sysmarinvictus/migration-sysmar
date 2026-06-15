package br.gov.mandaguari.saude.local.web;

import br.gov.mandaguari.saude.local.dto.LocalDtos.*;
import br.gov.mandaguari.saude.local.service.LocalService;
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
 * REST surface for Local — replaces the GeneXus sau_loc transaction + hpromptsau_loc.
 *
 * <p>NOTE: the legacy transaction disabled integrated security (sau_loc.java:33). Per project
 * convention the new app gates CRUD behind {@code SAUDE_CADASTRO} — confirm the exact permission
 * (SLICE-SPEC open question 2).
 */
@RestController
@RequestMapping("/api/locais")
@Tag(name = "Locais")
public class LocalController {

    private final LocalService service;

    public LocalController(LocalService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @Operation(summary = "Listar/buscar locais (paginado)")
    public Page<LocalResponse> list(@RequestParam(required = false) String nome,
                                    @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return service.list(nome, pageable);
    }

    @GetMapping("/{codigo}")
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @Operation(summary = "Obter local por código")
    public LocalResponse get(@PathVariable Integer codigo) {
        return service.get(codigo);
    }

    @GetMapping("/lookup")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Autocomplete de locais (FK picker)")
    public List<LocalLookupItem> lookup(@RequestParam(required = false, defaultValue = "") String q,
                                        @PageableDefault(size = 10) Pageable pageable) {
        return service.lookup(q, pageable);
    }

    @PostMapping
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @Operation(summary = "Criar local")
    public ResponseEntity<LocalResponse> create(@Valid @RequestBody LocalCreateRequest req,
                                                UriComponentsBuilder uri) {
        LocalResponse created = service.create(req);
        URI location = uri.path("/api/locais/{codigo}").buildAndExpand(created.codigo()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @Operation(summary = "Atualizar local")
    public LocalResponse update(@PathVariable Integer codigo, @Valid @RequestBody LocalUpdateRequest req) {
        return service.update(codigo, req);
    }

    @DeleteMapping("/{codigo}")
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir local")
    public void delete(@PathVariable Integer codigo) {
        service.delete(codigo);
    }
}
