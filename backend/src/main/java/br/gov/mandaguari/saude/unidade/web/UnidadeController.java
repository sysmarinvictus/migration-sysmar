package br.gov.mandaguari.saude.unidade.web;

import br.gov.mandaguari.saude.unidade.dto.UnidadeDtos.*;
import br.gov.mandaguari.saude.unidade.service.UnidadeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/unidades")
public class UnidadeController {

    private final UnidadeService service;

    public UnidadeController(UnidadeService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@authz.can(authentication, 'SAU_UNI', 'CON', 'SAUDE_CADASTRO')")
    public Page<UnidadeResponse> list(
            @RequestParam(required = false) String nome, Pageable pageable) {
        return service.list(nome, pageable);
    }

    @GetMapping("/lookup")
    @PreAuthorize("@authz.can(authentication, 'SAU_UNI', 'CON', 'SAUDE_CADASTRO')")
    public List<UnidadeLookupItem> lookup(
            @RequestParam(defaultValue = "") String q, Pageable pageable) {
        return service.lookup(q, pageable);
    }

    @GetMapping("/{codigo}")
    @PreAuthorize("@authz.can(authentication, 'SAU_UNI', 'CON', 'SAUDE_CADASTRO')")
    public UnidadeResponse get(@PathVariable Integer codigo) {
        return service.get(codigo);
    }

    @PostMapping
    @PreAuthorize("@authz.can(authentication, 'SAU_UNI', 'INC', 'SAUDE_ADMIN')")
    public ResponseEntity<UnidadeResponse> create(@Valid @RequestBody UnidadeCreateRequest req) {
        UnidadeResponse body = service.create(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(body.codigo()).toUri();
        return ResponseEntity.created(location).body(body);
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("@authz.can(authentication, 'SAU_UNI', 'ALT', 'SAUDE_ADMIN')")
    public UnidadeResponse update(@PathVariable Integer codigo,
                                  @Valid @RequestBody UnidadeUpdateRequest req) {
        return service.update(codigo, req);
    }

    @DeleteMapping("/{codigo}")
    @PreAuthorize("@authz.can(authentication, 'SAU_UNI', 'EXC', 'SAUDE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Integer codigo) {
        service.delete(codigo);
        return ResponseEntity.noContent().build();
    }
}
