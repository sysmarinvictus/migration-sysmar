package br.gov.mandaguari.saude.bairro.web;

import br.gov.mandaguari.saude.bairro.dto.BairroDtos.*;
import br.gov.mandaguari.saude.bairro.service.BairroService;
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

/** REST surface for Bairro — replaces sau_bai transaction. */
@RestController
@RequestMapping("/api/bairros")
@Tag(name = "Bairros")
public class BairroController {

    private final BairroService service;

    public BairroController(BairroService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("@authz.can(authentication, 'SAU_BAI', 'CON', 'SAUDE_CADASTRO')")
    @Operation(summary = "Listar/buscar bairros (paginado)")
    public Page<BairroResponse> list(@RequestParam(required = false) String nome,
                                     @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return service.list(nome, pageable);
    }

    @GetMapping("/{codigo}")
    @PreAuthorize("@authz.can(authentication, 'SAU_BAI', 'CON', 'SAUDE_CADASTRO')")
    @Operation(summary = "Obter bairro por código")
    public BairroResponse get(@PathVariable Integer codigo) {
        return service.get(codigo);
    }

    @GetMapping("/lookup")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Autocomplete de bairros (FK picker)")
    public List<BairroLookupItem> lookup(
            @RequestParam(required = false, defaultValue = "") String q,
            @PageableDefault(size = 10) Pageable pageable) {
        return service.lookup(q, pageable);
    }

    @PostMapping
    @PreAuthorize("@authz.can(authentication, 'SAU_BAI', 'INC', 'SAUDE_CADASTRO')")
    @Operation(summary = "Criar bairro (código auto-atribuído)")
    public ResponseEntity<BairroResponse> create(@Valid @RequestBody BairroCreateRequest req,
                                                 UriComponentsBuilder uri) {
        BairroResponse created = service.create(req);
        URI location = uri.path("/api/bairros/{codigo}").buildAndExpand(created.codigo()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("@authz.can(authentication, 'SAU_BAI', 'ALT', 'SAUDE_CADASTRO')")
    @Operation(summary = "Atualizar bairro")
    public BairroResponse update(@PathVariable Integer codigo,
                                 @Valid @RequestBody BairroUpdateRequest req) {
        return service.update(codigo, req);
    }

    @DeleteMapping("/{codigo}")
    @PreAuthorize("@authz.can(authentication, 'SAU_BAI', 'EXC', 'SAUDE_CADASTRO')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir bairro")
    public void delete(@PathVariable Integer codigo) {
        service.delete(codigo);
    }
}
