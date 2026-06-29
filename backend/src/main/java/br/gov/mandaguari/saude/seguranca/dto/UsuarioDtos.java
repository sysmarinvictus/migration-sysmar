package br.gov.mandaguari.saude.seguranca.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * User-admin DTOs. <b>Secrets ({@code UsuSen}/{@code UsuKey}) are NEVER present in any response DTO.</b>
 * Create/Update accept a plaintext password (admin-set) which the service bcrypt-hashes; no DTO ever
 * returns or echoes a password.
 */
public final class UsuarioDtos {
    private UsuarioDtos() {}

    /** Admin/read projection — no secrets. {@code nome} is PII (audited on read). */
    public record UsuarioResponse(
            Integer usuCod,
            String login,
            String nome,
            Short tipo,
            boolean bloqueado,
            Integer perfilId,
            boolean superusuario,
            Long profissionalId,
            Long funcionarioId,
            LocalDate ultimoAcesso,
            LocalDate dataRedefinicaoSenha) {}

    /** Slim picker item — no secrets, no PII beyond display name. */
    public record UsuarioLookupItem(Integer usuCod, String login, String nome) {}

    public record UsuarioCreateRequest(
            @NotBlank(message = "O nome é obrigatório") @Size(max = 50) String nome,   // R12
            @NotBlank(message = "O login é obrigatório") @Size(max = 20) String login,  // R12
            @NotBlank(message = "A senha é obrigatória") String senha,                  // bcrypt-hashed in service
            Short tipo,
            Integer perfilId,
            Long profissionalId,
            Long funcionarioId,
            Boolean bloqueado,        // R16: default unblocked when null
            Boolean superusuario) {}

    public record UsuarioUpdateRequest(
            @NotBlank(message = "O nome é obrigatório") @Size(max = 50) String nome,   // R12
            Short tipo,
            Integer perfilId,
            Long profissionalId,
            Long funcionarioId,
            Boolean bloqueado,        // R16: block/unblock
            Boolean superusuario) {}

    /** Self-service password change (R10): current + new + confirmation; NO complexity rules (legacy). */
    public record ChangePasswordRequest(
            @NotBlank String senhaAtual,
            @NotBlank String novaSenha,
            @NotBlank String confirmacaoSenha) {}
}
