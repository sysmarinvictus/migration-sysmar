package br.gov.mandaguari.saude.perfil.web;

import br.gov.mandaguari.saude.perfil.dto.PerfilDtos.*;
import br.gov.mandaguari.saude.perfil.service.PerfilService;
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
 * REST surface for Perfil (RBAC profiles) — replaces the GeneXus sau_prf transaction + hpromptsau_prf.
 * Maintenance is an administrative function → {@code SAUDE_ADMIN} (R9; matches SAU_USU admin CRUD).
 * The FK lookup is available to any authenticated user (used by the user-admin form's profile picker).
 */
@RestController
@RequestMapping("/api/perfis")
@Tag(name = "Perfis")
public class PerfilController {

    private final PerfilService service;

    public PerfilController(PerfilService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasRole('SAUDE_ADMIN')")
    @Operation(summary = "Listar/buscar perfis (paginado)")
    public Page<PerfilResponse> list(@RequestParam(required = false) String nome,
                                     @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return service.list(nome, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SAUDE_ADMIN')")
    @Operation(summary = "Obter perfil por código")
    public PerfilResponse get(@PathVariable Integer id) {
        return service.get(id);
    }

    @GetMapping("/lookup")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Autocomplete de perfis (FK picker)")
    public List<PerfilLookupItem> lookup(@RequestParam(required = false, defaultValue = "") String q,
                                         @PageableDefault(size = 10) Pageable pageable) {
        return service.lookup(q, pageable);
    }

    @PostMapping
    @PreAuthorize("hasRole('SAUDE_ADMIN')")
    @Operation(summary = "Criar perfil (código auto-gerado)")
    public ResponseEntity<PerfilResponse> create(@Valid @RequestBody PerfilCreateRequest req,
                                                 UriComponentsBuilder uri) {
        PerfilResponse created = service.create(req);
        URI location = uri.path("/api/perfis/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SAUDE_ADMIN')")
    @Operation(summary = "Atualizar perfil")
    public PerfilResponse update(@PathVariable Integer id, @Valid @RequestBody PerfilUpdateRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SAUDE_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir perfil (bloqueado se referenciado; cascateia permissões)")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
