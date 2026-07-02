package br.gov.mandaguari.saude.formaapresentacao.web;

import br.gov.mandaguari.saude.formaapresentacao.dto.FormaApresentacaoDtos.*;
import br.gov.mandaguari.saude.formaapresentacao.service.FormaApresentacaoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/formas-apresentacao")
public class FormaApresentacaoController {

    private final FormaApresentacaoService service;

    public FormaApresentacaoController(FormaApresentacaoService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("@authz.can(authentication, 'SAU_APRREM', 'CON', 'SAUDE_CADASTRO')")
    public Page<FormaApresentacaoResponse> list(@RequestParam(required = false) String descricao,
                                                @PageableDefault(size = 20, sort = "descricao") Pageable pageable) {
        return service.list(descricao, pageable);
    }

    @GetMapping("/lookup")
    @PreAuthorize("@authz.can(authentication, 'SAU_APRREM', 'CON', 'SAUDE_CADASTRO')")
    public List<FormaApresentacaoLookupItem> lookup(@RequestParam(defaultValue = "") String q, Pageable pageable) {
        return service.lookup(q, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.can(authentication, 'SAU_APRREM', 'CON', 'SAUDE_CADASTRO')")
    public FormaApresentacaoResponse get(@PathVariable Integer id) {
        return service.get(id);
    }

    @PostMapping   // R12 → 201
    @PreAuthorize("@authz.can(authentication, 'SAU_APRREM', 'INC', 'SAUDE_CADASTRO')")
    public ResponseEntity<FormaApresentacaoResponse> create(@Valid @RequestBody FormaApresentacaoCreateRequest req) {
        FormaApresentacaoResponse body = service.create(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(body.id()).toUri();
        return ResponseEntity.created(location).body(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.can(authentication, 'SAU_APRREM', 'ALT', 'SAUDE_CADASTRO')")
    public FormaApresentacaoResponse update(@PathVariable Integer id,
                                            @Valid @RequestBody FormaApresentacaoUpdateRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.can(authentication, 'SAU_APRREM', 'EXC', 'SAUDE_CADASTRO')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
