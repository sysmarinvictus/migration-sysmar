package br.gov.mandaguari.saude.autorizacao.web;

import br.gov.mandaguari.saude.autorizacao.domain.Mode;
import br.gov.mandaguari.saude.autorizacao.dto.AutorizacaoDtos.*;
import br.gov.mandaguari.saude.autorizacao.service.AutorizacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Maintains and inspects the RBAC permission grids (per-profile SAU_PRFCON, per-user SAU_USUCON) and
 * exposes the fine-grained {@code PermissionResolver} check. Administrative — SAUDE_ADMIN.
 */
@RestController
@RequestMapping("/api/autorizacao")
@Tag(name = "Autorização")
@PreAuthorize("hasRole('SAUDE_ADMIN')")
public class AutorizacaoController {

    private final AutorizacaoService service;

    public AutorizacaoController(AutorizacaoService service) { this.service = service; }

    @GetMapping("/perfis/{perfilId}/permissoes")
    @Operation(summary = "Permissões por-programa de um perfil (SAU_PRFCON)")
    public List<PermissaoResponse> perfilPermissoes(@PathVariable Integer perfilId) {
        return service.getPerfilPermissoes(perfilId);
    }

    @PutMapping("/perfis/{perfilId}/permissoes/{programaId}")
    @Operation(summary = "Definir as permissões Inc/Alt/Exc/Con de um perfil para um programa")
    public PermissaoResponse setPerfilPermissao(@PathVariable Integer perfilId,
                                                @PathVariable String programaId,
                                                @Valid @RequestBody PermissaoUpsertRequest req) {
        return service.setPerfilPermissao(perfilId, programaId, req);
    }

    @GetMapping("/usuarios/{usuCod}/permissoes")
    @Operation(summary = "Permissões por-programa de um usuário (SAU_USUCON)")
    public List<PermissaoResponse> usuarioPermissoes(@PathVariable Integer usuCod) {
        return service.getUsuarioPermissoes(usuCod);
    }

    @PutMapping("/usuarios/{usuCod}/permissoes/{programaId}")
    @Operation(summary = "Definir as permissões Inc/Alt/Exc/Con de um usuário para um programa")
    public PermissaoResponse setUsuarioPermissao(@PathVariable Integer usuCod,
                                                 @PathVariable String programaId,
                                                 @Valid @RequestBody PermissaoUpsertRequest req) {
        return service.setUsuarioPermissao(usuCod, programaId, req);
    }

    @GetMapping("/check")
    @Operation(summary = "Resolver a permissão efetiva (R8: SYSMAR → perfil → usuário) de um usuário num programa")
    public CheckResponse check(@RequestParam Integer usuCod,
                               @RequestParam String programaId,
                               @RequestParam Mode mode) {
        return service.check(usuCod, programaId, mode);
    }
}
