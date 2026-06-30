package br.gov.mandaguari.saude.funcionario.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTOs for the Funcionário API. Person fields (name/CPF/phones) are projected from / written back to
 * SYS_PES (R2). No secret fields. CPF is NOT check-digit validated (R12 — legacy SAU_FUN does not).
 */
public final class FuncionarioDtos {
    private FuncionarioDtos() {}

    /** Detail/list response — SAU_FUN own fields + projected SYS_PES person (PHI). */
    public record FuncionarioResponse(
            Long id,
            String telefoneTrabalho,
            String ramal,
            Short situacao,
            String nome,
            String cpfCnpj,
            String telefone,
            String celular) {}

    /** Slim FK-autocomplete item. nome is PII → endpoint is SAUDE_CADASTRO-only. */
    public record FuncionarioLookupItem(Long id, String nome) {}

    /** Person sub-fields written back to SYS_PES (R2). */
    public record PessoaSubRequest(
            @Size(max = 50) String nome,
            @Size(max = 18) String cpfCnpj,
            @Size(max = 20) String telefone,
            @Size(max = 20) String celular) {}

    /** POST body INCLUDES id (the person code) — user-supplied, not generated (R1). */
    public record FuncionarioCreateRequest(
            @NotNull(message = "Informe o código da Pessoa (Funcionário)!") Long id,
            @Size(max = 20) String telefoneTrabalho,
            @Size(max = 10) String ramal,
            Short situacao,
            PessoaSubRequest pessoa) {}

    /** PUT body — id comes from the path. */
    public record FuncionarioUpdateRequest(
            @Size(max = 20) String telefoneTrabalho,
            @Size(max = 10) String ramal,
            Short situacao,
            PessoaSubRequest pessoa) {}
}
