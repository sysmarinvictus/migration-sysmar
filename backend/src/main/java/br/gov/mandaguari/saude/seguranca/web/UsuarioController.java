package br.gov.mandaguari.saude.seguranca.web;

import br.gov.mandaguari.saude.seguranca.dto.UsuarioDtos.*;
import br.gov.mandaguari.saude.seguranca.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
 * User administration — replaces hwwsau_usu/hviewsau_usu/sau_usu transaction. All endpoints require
 * {@code SAUDE_ADMIN}. Responses NEVER include secrets (UsuSen/UsuKey are not even on the DTOs).
 */
@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuários (administração)")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("@authz.can(authentication, 'SAU_USU', 'CON', 'SAUDE_ADMIN')")
    @Operation(summary = "Listar/buscar usuários (paginado) — filtros login/nome/bloqueado")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Página de usuários (sem segredos)"))
    public Page<UsuarioResponse> list(@RequestParam(required = false) String login,
                                      @RequestParam(required = false) String nome,
                                      @RequestParam(required = false) Boolean bloqueado,
                                      @PageableDefault(size = 20, sort = "login") Pageable pageable) {
        return service.list(login, nome, bloqueado, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.can(authentication, 'SAU_USU', 'CON', 'SAUDE_ADMIN')")
    @Operation(summary = "Obter usuário por código")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Usuário"),
                   @ApiResponse(responseCode = "404", description = "Não encontrado")})
    public UsuarioResponse get(@PathVariable Integer id) {
        return service.get(id);
    }

    // SECURITY (LGPD): the user picker exposes UsuNom (PII), so it stays SAUDE_ADMIN-only (inherits the
    // class-level guard — do NOT downgrade to isAuthenticated()). A non-admin must not enumerate users/names.
    @GetMapping("/lookup")
    @PreAuthorize("@authz.can(authentication, 'SAU_USU', 'CON', 'SAUDE_ADMIN')")
    @Operation(summary = "Autocomplete de usuários (picker, admin)")
    public List<UsuarioLookupItem> lookup(@RequestParam(required = false, defaultValue = "") String q,
                                          @PageableDefault(size = 10) Pageable pageable) {
        return service.lookup(q, pageable);
    }

    @PostMapping
    @PreAuthorize("@authz.can(authentication, 'SAU_USU', 'INC', 'SAUDE_ADMIN')")
    @Operation(summary = "Criar usuário (login único; senha bcrypt; default desbloqueado)")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Criado"),
                   @ApiResponse(responseCode = "409", description = "Login já em uso")})
    public ResponseEntity<UsuarioResponse> create(@Valid @RequestBody UsuarioCreateRequest req,
                                                  UriComponentsBuilder uri) {
        UsuarioResponse created = service.create(req);
        URI location = uri.path("/api/usuarios/{id}").buildAndExpand(created.usuCod()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.can(authentication, 'SAU_USU', 'ALT', 'SAUDE_ADMIN')")
    @Operation(summary = "Atualizar usuário (bloquear/desbloquear via bloqueado)")
    public UsuarioResponse update(@PathVariable Integer id, @Valid @RequestBody UsuarioUpdateRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.can(authentication, 'SAU_USU', 'EXC', 'SAUDE_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir usuário (bloqueado se referenciado por permissões/unidades)")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Excluído"),
                   @ApiResponse(responseCode = "409", description = "Usuário possui vínculos")})
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
