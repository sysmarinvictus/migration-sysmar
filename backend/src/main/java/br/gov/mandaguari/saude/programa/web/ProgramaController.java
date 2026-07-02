package br.gov.mandaguari.saude.programa.web;

import br.gov.mandaguari.saude.programa.dto.ProgramaDtos.*;
import br.gov.mandaguari.saude.programa.service.ProgramaService;
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
 * REST surface for the program catalog (SAU_PRG) + groups (SAU_PRGGRP). Administrative — SAUDE_ADMIN.
 * Programs are the permission-space keys consumed by the RBAC matrices.
 */
@RestController
@RequestMapping("/api/programas")
@Tag(name = "Programas")
public class ProgramaController {

    private final ProgramaService service;

    public ProgramaController(ProgramaService service) { this.service = service; }

    @GetMapping("/grupos")
    @PreAuthorize("@authz.can(authentication, 'SAU_PRG', 'CON', 'SAUDE_ADMIN')")
    @Operation(summary = "Listar grupos de programas (SAU_PRGGRP)")
    public List<GrupoProgramaResponse> grupos() {
        return service.listGrupos();
    }

    @GetMapping
    @PreAuthorize("@authz.can(authentication, 'SAU_PRG', 'CON', 'SAUDE_ADMIN')")
    @Operation(summary = "Listar/buscar programas (paginado)")
    public Page<ProgramaResponse> list(@RequestParam(required = false) String q,
                                       @RequestParam(required = false) Integer grupoId,
                                       @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return service.list(q, grupoId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.can(authentication, 'SAU_PRG', 'CON', 'SAUDE_ADMIN')")
    @Operation(summary = "Obter programa por código")
    public ProgramaResponse get(@PathVariable String id) {
        return service.get(id);
    }

    @GetMapping("/lookup")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Autocomplete de programas (FK picker)")
    public List<ProgramaLookupItem> lookup(@RequestParam(required = false, defaultValue = "") String q,
                                           @PageableDefault(size = 10) Pageable pageable) {
        return service.lookup(q, pageable);
    }

    @PostMapping
    @PreAuthorize("@authz.can(authentication, 'SAU_PRG', 'INC', 'SAUDE_ADMIN')")
    @Operation(summary = "Criar programa")
    public ResponseEntity<ProgramaResponse> create(@Valid @RequestBody ProgramaCreateRequest req,
                                                   UriComponentsBuilder uri) {
        ProgramaResponse created = service.create(req);
        URI location = uri.path("/api/programas/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.can(authentication, 'SAU_PRG', 'ALT', 'SAUDE_ADMIN')")
    @Operation(summary = "Atualizar programa")
    public ProgramaResponse update(@PathVariable String id, @Valid @RequestBody ProgramaUpdateRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.can(authentication, 'SAU_PRG', 'EXC', 'SAUDE_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir programa (bloqueado se referenciado por permissões)")
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
