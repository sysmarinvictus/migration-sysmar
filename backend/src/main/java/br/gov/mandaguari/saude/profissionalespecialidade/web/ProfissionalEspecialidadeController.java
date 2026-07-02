package br.gov.mandaguari.saude.profissionalespecialidade.web;

import br.gov.mandaguari.saude.profissionalespecialidade.dto.ProfissionalEspecialidadeDtos.*;
import br.gov.mandaguari.saude.profissionalespecialidade.service.ProfissionalEspecialidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Especialidades de um profissional — SAU_PROESP as a sub-resource of profissional. Replaces the
 * GeneXus sau_proesp grid embedded in the professional transaction. SAUDE_CADASTRO.
 */
@RestController
@RequestMapping("/api/profissionais/{proPesCod}/especialidades")
@Tag(name = "Profissional — Especialidades")
public class ProfissionalEspecialidadeController {

    private final ProfissionalEspecialidadeService service;

    public ProfissionalEspecialidadeController(ProfissionalEspecialidadeService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@authz.can(authentication, 'SAU_PROESP', 'CON', 'SAUDE_CADASTRO')")
    @Operation(summary = "Listar as especialidades de um profissional")
    public List<EspecialidadeDoProfissionalResponse> list(@PathVariable Long proPesCod) {
        return service.list(proPesCod);
    }

    @PostMapping
    @PreAuthorize("@authz.can(authentication, 'SAU_PROESP', 'INC', 'SAUDE_CADASTRO')")
    @Operation(summary = "Adicionar uma especialidade ao profissional")
    public ResponseEntity<EspecialidadeDoProfissionalResponse> add(
            @PathVariable Long proPesCod,
            @Valid @RequestBody EspecialidadeCreateRequest req,
            UriComponentsBuilder uri) {
        EspecialidadeDoProfissionalResponse created = service.add(proPesCod, req);
        URI location = uri.path("/api/profissionais/{proPesCod}/especialidades/{espCod}")
                .buildAndExpand(proPesCod, created.especialidadeId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{espCod}")
    @PreAuthorize("@authz.can(authentication, 'SAU_PROESP', 'ALT', 'SAUDE_CADASTRO')")
    @Operation(summary = "Atualizar flags/agenda de uma especialidade do profissional")
    public EspecialidadeDoProfissionalResponse update(
            @PathVariable Long proPesCod,
            @PathVariable Integer espCod,
            @Valid @RequestBody EspecialidadeUpdateRequest req) {
        return service.update(proPesCod, espCod, req);
    }

    @DeleteMapping("/{espCod}")
    @PreAuthorize("@authz.can(authentication, 'SAU_PROESP', 'EXC', 'SAUDE_CADASTRO')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover uma especialidade do profissional (R5: bloqueado se há Impedimento)")
    public void remove(@PathVariable Long proPesCod, @PathVariable Integer espCod) {
        service.remove(proPesCod, espCod);
    }
}
