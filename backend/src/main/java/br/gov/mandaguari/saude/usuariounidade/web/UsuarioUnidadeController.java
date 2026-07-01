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
@PreAuthorize("hasRole('SAUDE_ADMIN')")
public class UsuarioUnidadeController {

    private final UsuarioUnidadeService service;

    public UsuarioUnidadeController(UsuarioUnidadeService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Listar as unidades (e permissões) de um usuário")
    public List<UsuarioUnidadeResponse> list(@PathVariable Integer usuCod) {
        return service.list(usuCod);
    }

    @GetMapping("/{uniCod}")
    @Operation(summary = "Obter a matriz de permissões de um usuário numa unidade")
    public UsuarioUnidadeResponse get(@PathVariable Integer usuCod, @PathVariable Integer uniCod) {
        return service.get(usuCod, uniCod);
    }

    @PostMapping
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
    @Operation(summary = "Atualizar as permissões de um usuário numa unidade")
    public UsuarioUnidadeResponse update(@PathVariable Integer usuCod, @PathVariable Integer uniCod,
                                         @RequestBody UsuarioUnidadeUpsertRequest req) {
        return service.update(usuCod, uniCod, req);
    }

    @DeleteMapping("/{uniCod}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revogar o acesso de um usuário a uma unidade")
    public void delete(@PathVariable Integer usuCod, @PathVariable Integer uniCod) {
        service.delete(usuCod, uniCod);
    }
}
