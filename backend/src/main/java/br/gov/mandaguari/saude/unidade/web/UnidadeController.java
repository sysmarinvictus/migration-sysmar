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
@PreAuthorize("hasRole('SAUDE_CADASTRO')")
public class UnidadeController {

    private final UnidadeService service;

    public UnidadeController(UnidadeService service) {
        this.service = service;
    }

    @GetMapping
    public Page<UnidadeResponse> list(
            @RequestParam(required = false) String nome, Pageable pageable) {
        return service.list(nome, pageable);
    }

    @GetMapping("/lookup")
    public List<UnidadeLookupItem> lookup(
            @RequestParam(defaultValue = "") String q, Pageable pageable) {
        return service.lookup(q, pageable);
    }

    @GetMapping("/{codigo}")
    public UnidadeResponse get(@PathVariable Integer codigo) {
        return service.get(codigo);
    }

    @PostMapping
    public ResponseEntity<UnidadeResponse> create(@Valid @RequestBody UnidadeCreateRequest req) {
        UnidadeResponse body = service.create(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(body.codigo()).toUri();
        return ResponseEntity.created(location).body(body);
    }

    @PutMapping("/{codigo}")
    public UnidadeResponse update(@PathVariable Integer codigo,
                                  @Valid @RequestBody UnidadeUpdateRequest req) {
        return service.update(codigo, req);
    }

    @DeleteMapping("/{codigo}")
    public ResponseEntity<Void> delete(@PathVariable Integer codigo) {
        service.delete(codigo);
        return ResponseEntity.noContent().build();
    }
}
