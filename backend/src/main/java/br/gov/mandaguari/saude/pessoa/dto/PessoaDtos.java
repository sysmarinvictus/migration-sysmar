package br.gov.mandaguari.saude.pessoa.dto;

import java.time.LocalDate;

/** DTOs for the Pessoa read API. No secrets (PesSenha/PesSenhaKey are unmapped/quarantined). */
public final class PessoaDtos {
    private PessoaDtos() {}

    /**
     * Person read projection. {@code nomeExibicao}/{@code nomeCompleto} apply the social-name rule
     * (R2/R3) — consumers (UI/reports) MUST display {@code nomeExibicao}, never the raw registry name,
     * when {@code usaNomeSocial} (LGPD / Decreto 8.727/2016).
     */
    public record PessoaResponse(
            Long id,
            String nome,
            String nomeSocial,
            boolean usaNomeSocial,
            String nomeExibicao,
            String nomeCompleto,
            String cpfCnpj,
            String cns,
            LocalDate dataNascimento,
            String sexo,
            String telefone,
            String celular,
            String email) {}

    /** Slim lookup item — shows the LGPD-correct display name only. */
    public record PessoaLookupItem(Long id, String nomeExibicao) {}
}
