package br.gov.mandaguari.saude.conselhoclasse.web;

import br.gov.mandaguari.saude.conselhoclasse.dto.ConselhoClasseDtos.*;
import br.gov.mandaguari.saude.conselhoclasse.service.ConselhoClasseService;
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
 * REST surface for Conselho de Classe — replaces the GeneXus sau_concla transaction + hpromptsau_concla.
 *
 * <p>NOTE: the legacy transaction disabled integrated security ({@code IntegratedSecurityEnabled()}
 * → false). Per project convention the new app gates CRUD behind {@code SAUDE_CADASTRO} — confirm the
 * exact role/permission (SLICE-SPEC open question 1).
 */
@RestController
@RequestMapping("/api/conselhos-classe")
@Tag(name = "Conselhos de Classe")
public class ConselhoClasseController {

    private final ConselhoClasseService service;

    public ConselhoClasseController(ConselhoClasseService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("@authz.can(authentication, 'SAU_CONCLA', 'CON', 'SAUDE_CADASTRO')")
    @Operation(summary = "Listar/buscar conselhos de classe (paginado)")
    public Page<ConselhoClasseResponse> list(@RequestParam(required = false) String q,
                                             @PageableDefault(size = 20, sort = "codigo") Pageable pageable) {
        return service.list(q, pageable);
    }

    @GetMapping("/{codigo}")
    @PreAuthorize("@authz.can(authentication, 'SAU_CONCLA', 'CON', 'SAUDE_CADASTRO')")
    @Operation(summary = "Obter conselho de classe por código")
    public ConselhoClasseResponse get(@PathVariable Short codigo) {
        return service.get(codigo);
    }

    @GetMapping("/lookup")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Autocomplete de conselhos de classe (FK picker)")
    public List<ConselhoClasseLookupItem> lookup(@RequestParam(required = false, defaultValue = "") String q,
                                                 @PageableDefault(size = 10) Pageable pageable) {
        return service.lookup(q, pageable);
    }

    @PostMapping
    @PreAuthorize("@authz.can(authentication, 'SAU_CONCLA', 'INC', 'SAUDE_CADASTRO')")
    @Operation(summary = "Criar conselho de classe")
    public ResponseEntity<ConselhoClasseResponse> create(@Valid @RequestBody ConselhoClasseCreateRequest req,
                                                         UriComponentsBuilder uri) {
        ConselhoClasseResponse created = service.create(req);
        URI location = uri.path("/api/conselhos-classe/{codigo}").buildAndExpand(created.codigo()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("@authz.can(authentication, 'SAU_CONCLA', 'ALT', 'SAUDE_CADASTRO')")
    @Operation(summary = "Atualizar conselho de classe")
    public ConselhoClasseResponse update(@PathVariable Short codigo,
                                         @Valid @RequestBody ConselhoClasseUpdateRequest req) {
        return service.update(codigo, req);
    }

    @DeleteMapping("/{codigo}")
    @PreAuthorize("@authz.can(authentication, 'SAU_CONCLA', 'EXC', 'SAUDE_CADASTRO')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir conselho de classe")
    public void delete(@PathVariable Short codigo) {
        service.delete(codigo);
    }
}
