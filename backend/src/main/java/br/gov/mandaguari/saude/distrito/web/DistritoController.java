package br.gov.mandaguari.saude.distrito.web;

import br.gov.mandaguari.saude.distrito.dto.DistritoDtos.*;
import br.gov.mandaguari.saude.distrito.service.DistritoService;
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

/** REST surface for Distrito Sanitário — replaces sau_dis transaction. */
@RestController
@RequestMapping("/api/distritos")
@Tag(name = "Distritos")
public class DistritoController {

    private final DistritoService service;

    public DistritoController(DistritoService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @Operation(summary = "Listar/buscar distritos sanitários (paginado)")
    public Page<DistritoResponse> list(@RequestParam(required = false) String nome,
                                       @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return service.list(nome, pageable);
    }

    @GetMapping("/{codigo}")
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @Operation(summary = "Obter distrito por código")
    public DistritoResponse get(@PathVariable Short codigo) {
        return service.get(codigo);
    }

    @GetMapping("/lookup")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Autocomplete de distritos (FK picker)")
    public List<DistritoLookupItem> lookup(
            @RequestParam(required = false, defaultValue = "") String q,
            @PageableDefault(size = 10) Pageable pageable) {
        return service.lookup(q, pageable);
    }

    @PostMapping
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @Operation(summary = "Criar distrito sanitário (código auto-atribuído)")
    public ResponseEntity<DistritoResponse> create(@Valid @RequestBody DistritoCreateRequest req,
                                                   UriComponentsBuilder uri) {
        DistritoResponse created = service.create(req);
        URI location = uri.path("/api/distritos/{codigo}").buildAndExpand(created.codigo()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @Operation(summary = "Atualizar distrito sanitário")
    public DistritoResponse update(@PathVariable Short codigo,
                                   @Valid @RequestBody DistritoUpdateRequest req) {
        return service.update(codigo, req);
    }

    @DeleteMapping("/{codigo}")
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir distrito sanitário")
    public void delete(@PathVariable Short codigo) {
        service.delete(codigo);
    }
}
