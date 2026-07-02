package br.gov.mandaguari.saude.usuariounidade.web;

import br.gov.mandaguari.saude.usuariounidade.dto.UsuarioUnidadeDtos.*;
import br.gov.mandaguari.saude.usuariounidade.service.UsuarioUnidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Usuário × Unidade capability matrix (SAU_USUUNI) — a sub-resource of usuário. Administers which
 * modules a user may use in each unit (block flags) and which extra capabilities they are granted
 * (permit flags). High-privilege: SAUDE_ADMIN (this table is itself an authorization source).
 */
@RestController
@RequestMapping("/api/usuarios/{usuCod}/unidades")
@Tag(name = "Usuário — Unidades (permissões)")
public class UsuarioUnidadeController {

    private final UsuarioUnidadeService service;

    public UsuarioUnidadeController(UsuarioUnidadeService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("@authz.can(authentication, 'SAU_USUUNI', 'CON', 'SAUDE_ADMIN')")
    @Operation(summary = "Listar as unidades (e permissões) de um usuário")
    public List<UsuarioUnidadeResponse> list(@PathVariable Integer usuCod) {
        return service.list(usuCod);
    }

    @GetMapping("/{uniCod}")
    @PreAuthorize("@authz.can(authentication, 'SAU_USUUNI', 'CON', 'SAUDE_ADMIN')")
    @Operation(summary = "Obter a matriz de permissões de um usuário numa unidade")
    public UsuarioUnidadeResponse get(@PathVariable Integer usuCod, @PathVariable Integer uniCod) {
        return service.get(usuCod, uniCod);
    }

    @PostMapping
    @PreAuthorize("@authz.can(authentication, 'SAU_USUUNI', 'INC', 'SAUDE_ADMIN')")
    @Operation(summary = "Conceder acesso de um usuário a uma unidade")
    public ResponseEntity<UsuarioUnidadeResponse> create(@PathVariable Integer usuCod,
                                                         @RequestParam Integer uniCod,
                                                         @RequestBody UsuarioUnidadeUpsertRequest req,
                                                         UriComponentsBuilder uri) {
        UsuarioUnidadeResponse created = service.create(usuCod, uniCod, req);
        URI location = uri.path("/api/usuarios/{usuCod}/unidades/{uniCod}").buildAndExpand(usuCod, uniCod).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{uniCod}")
    @PreAuthorize("@authz.can(authentication, 'SAU_USUUNI', 'ALT', 'SAUDE_ADMIN')")
    @Operation(summary = "Atualizar as permissões de um usuário numa unidade")
    public UsuarioUnidadeResponse update(@PathVariable Integer usuCod, @PathVariable Integer uniCod,
                                         @RequestBody UsuarioUnidadeUpsertRequest req) {
        return service.update(usuCod, uniCod, req);
    }

    @DeleteMapping("/{uniCod}")
    @PreAuthorize("@authz.can(authentication, 'SAU_USUUNI', 'EXC', 'SAUDE_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revogar o acesso de um usuário a uma unidade")
    public void delete(@PathVariable Integer usuCod, @PathVariable Integer uniCod) {
        service.delete(usuCod, uniCod);
    }
}
