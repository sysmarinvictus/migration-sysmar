package br.gov.mandaguari.saude.profissionalexterno.web;

import br.gov.mandaguari.saude.profissionalexterno.dto.ProfissionalExternoDtos.*;
import br.gov.mandaguari.saude.profissionalexterno.service.ProfissionalExternoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * "Cadastro de Profissional Externo" (SAU_PESF_PROFEXT) — registers a person + an external professional
 * (SAU_PRO, ProExt=1) in one atomic call. Edit/delete of the resulting professional go through the
 * profissional (SAU_PRO) flow, mirroring the legacy post-create redirect. SAUDE_CADASTRO.
 */
@RestController
@RequestMapping("/api/profissionais-externos")
@Tag(name = "Profissionais Externos")
public class ProfissionalExternoController {

    private final ProfissionalExternoService service;

    public ProfissionalExternoController(ProfissionalExternoService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("@authz.can(authentication, 'SAU_PESF_PROFEXT', 'INC', 'SAUDE_CADASTRO')")
    @Operation(summary = "Registrar profissional externo (cria a pessoa + o SAU_PRO externo)")
    public ResponseEntity<ProfissionalExternoResponse> create(@RequestBody ProfissionalExternoCreateRequest req,
                                                              UriComponentsBuilder uri) {
        ProfissionalExternoResponse created = service.create(req);
        URI location = uri.path("/api/profissionais-externos/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.can(authentication, 'SAU_PESF_PROFEXT', 'CON', 'SAUDE_CADASTRO')")
    @Operation(summary = "Obter um profissional externo por código")
    public ProfissionalExternoResponse get(@PathVariable Long id) {
        return service.get(id);
    }
}
