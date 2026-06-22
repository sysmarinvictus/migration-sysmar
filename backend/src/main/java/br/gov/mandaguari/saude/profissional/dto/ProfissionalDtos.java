package br.gov.mandaguari.saude.profissional.dto;

import br.gov.mandaguari.saude.common.validation.CNS;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * DTOs for the Profissional API.
 *
 * <p><b>v1 security scope:</b> NO DTO here exposes {@code certificado}, {@code assinaturaImagem} or
 * {@code certificadoSenha} — the signing certificate, signature image and (cleartext-in-legacy)
 * certificate password are out of the v1 API surface and handled by a future audited sub-feature.
 */
public final class ProfissionalDtos {

    private ProfissionalDtos() {}

    /** Detail/list response. Joins SYS_PES (person) + SAU_CONCLA. Never returns secret/blob fields. */
    public record ProfissionalResponse(
            Long id,
            String numeroCns,
            String numeroCr,
            String ufConselho,
            Short conselhoClasseCod,
            String conselhoClasseNome,
            String conselhoClasseSigla,
            LocalDate dataInicio,
            LocalDate dataFim,
            String cnesId,
            Boolean exportaEsus,
            Short externo,
            Short situacao,
            // projected person (SYS_PES) — PHI
            String nome,
            String cpfCnpj,
            String rgIe,
            String sexo,
            LocalDate dataNascimento,
            String endereco,
            String telefone,
            String celular) {}

    /** Slim FK-autocomplete item (prescriber selector for SAU_RECESP). */
    public record ProfissionalLookupItem(
            Long id,
            String nome,
            String numeroCns,
            String conselhoClasseSigla) {}

    /** Person sub-fields submitted alongside SAU_PRO data (write-back to SYS_PES, R2). */
    public record PessoaSubRequest(
            @Size(max = 50) String nome,
            @Size(max = 18) String cpfCnpj,
            @Size(max = 20) String telefone,
            @Size(max = 20) String celular) {}

    /** POST body INCLUDES id (the person code) — it is user-supplied, not generated (R1). */
    public record ProfissionalCreateRequest(
            @NotNull(message = "Informe o código da Pessoa (Profissional)!") Long id,
            @NotNull(message = "Informe o Número do CNS!")
            @CNS(message = "CNS inválido") String numeroCns,
            @Size(max = 20) String numeroCr,
            @Size(max = 2) String ufConselho,
            Short conselhoClasseCod,
            LocalDate dataInicio,
            LocalDate dataFim,
            @Size(max = 20) String cnesId,
            Boolean exportaEsus,
            Short externo,
            Short situacao,
            PessoaSubRequest pessoa) {}

    /** PUT body — id comes from the path. */
    public record ProfissionalUpdateRequest(
            @NotNull(message = "Informe o Número do CNS!")
            @CNS(message = "CNS inválido") String numeroCns,
            @Size(max = 20) String numeroCr,
            @Size(max = 2) String ufConselho,
            Short conselhoClasseCod,
            LocalDate dataInicio,
            LocalDate dataFim,
            @Size(max = 20) String cnesId,
            Boolean exportaEsus,
            Short externo,
            Short situacao,
            PessoaSubRequest pessoa) {}
}
