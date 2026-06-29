package br.gov.mandaguari.saude.seguranca.web;

import br.gov.mandaguari.saude.seguranca.dto.UsuarioDtos.ChangePasswordRequest;
import br.gov.mandaguari.saude.seguranca.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-service password change (R10/R11) — replaces hmudasenhalogin. Authenticated principal only.
 * Mounted under {@code /auth} alongside the existing AuthController (login/refresh) without touching it.
 *
 * <p>The {@code UsuarioService} is only present in non-test/local profiles (it depends on the real
 * SAU_USU stack); this controller is therefore effectively a prod/default-profile endpoint and does
 * not affect the dev-stub-based integration tests.
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação")
public class ChangePasswordController {

    private final UsuarioService service;

    public ChangePasswordController(UsuarioService service) { this.service = service; }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Trocar a própria senha (exige senha atual + confirmação; sem regras de complexidade)")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req,
                                               Authentication authentication) {
        service.changePassword(authentication.getName(), req);  // principal = UsuCod (JWT sub)
        return ResponseEntity.noContent().build();
    }
}
