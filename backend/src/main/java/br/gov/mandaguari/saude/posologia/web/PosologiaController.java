package br.gov.mandaguari.saude.posologia.web;

import br.gov.mandaguari.saude.posologia.dto.PosologiaDtos.*;
import br.gov.mandaguari.saude.posologia.service.PosologiaService;
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

/** REST surface for Posologia — replaces sau_remobs transaction + hpromptsau_remobs. */
@RestController
@RequestMapping("/api/posologias")
@Tag(name = "Posologias")
public class PosologiaController {

    private final PosologiaService service;

    public PosologiaController(PosologiaService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("@authz.can(authentication, 'SAU_REMOBS', 'CON', 'SAUDE_CADASTRO')")
    @Operation(summary = "Listar/buscar posologias (paginado)")
    public Page<PosologiaResponse> list(@RequestParam(required = false) String descricao,
                                        @PageableDefault(size = 20, sort = "descricao") Pageable pageable) {
        return service.list(descricao, pageable);
    }

    @GetMapping("/{codigo}")
    @PreAuthorize("@authz.can(authentication, 'SAU_REMOBS', 'CON', 'SAUDE_CADASTRO')")
    @Operation(summary = "Obter posologia por código")
    public PosologiaResponse get(@PathVariable Integer codigo) {
        return service.get(codigo);
    }

    @GetMapping("/lookup")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Autocomplete de posologias (FK picker)")
    public List<PosologiaLookupItem> lookup(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false, defaultValue = "0") Integer posologiaIndividual,
            @PageableDefault(size = 10) Pageable pageable) {
        return service.lookup(q, pageable);
    }

    @PostMapping
    @PreAuthorize("@authz.can(authentication, 'SAU_REMOBS', 'INC', 'SAUDE_CADASTRO')")
    @Operation(summary = "Criar posologia (código auto-atribuído)")
    public ResponseEntity<PosologiaResponse> create(@Valid @RequestBody PosologiaCreateRequest req,
                                                    UriComponentsBuilder uri) {
        PosologiaResponse created = service.create(req);
        URI location = uri.path("/api/posologias/{codigo}").buildAndExpand(created.codigo()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("@authz.can(authentication, 'SAU_REMOBS', 'ALT', 'SAUDE_CADASTRO')")
    @Operation(summary = "Atualizar posologia")
    public PosologiaResponse update(@PathVariable Integer codigo,
                                    @Valid @RequestBody PosologiaUpdateRequest req) {
        return service.update(codigo, req);
    }

    @DeleteMapping("/{codigo}")
    @PreAuthorize("@authz.can(authentication, 'SAU_REMOBS', 'EXC', 'SAUDE_CADASTRO')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir posologia")
    public void delete(@PathVariable Integer codigo) {
        service.delete(codigo);
    }
}
