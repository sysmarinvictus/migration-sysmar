package br.gov.mandaguari.saude.parametro.dto;

/**
 * DTOs for the SAU_PAR singleton config. Two focused update views mirror the two legacy transactions:
 * {@code geral} (sau_par_ger) and {@code ambulatorial} (sau_par_amb). The response echoes the full mapped
 * subset. Only the fields present in each update record are writable via that endpoint.
 */
public final class ParametroDtos {
    private ParametroDtos() {}

    /** Full read projection of the mapped config subset. */
    public record ParametroResponse(
            Integer empresaCod,
            // general — prescription validity
            Boolean validadeReceita,
            Integer validadeReceitaSimplesDias,
            Integer validadeReceitaUsoContinuoDias,
            Integer validadeReceitaControleEspecialDias,
            // general — user/security
            Integer inatividadeUsuarioDias,
            Integer senhaUsuarioDias,
            // general — secretariat header
            String secretaria,
            String secretariaEndereco,
            String secretariaCep,
            String secretariaFone1,
            String secretariaFone2,
            String secretariaEmail,
            // general — policy flags
            Boolean cadastroSemCns,
            Boolean reciboComprador,
            // ambulatorial — policy flags
            Boolean exigeCid10Atestado,
            Boolean estornarAtendimento,
            Integer imprimeRiscoMaterno,
            Integer atendimentoHistorico) {}

    /** Update the GENERAL parameters (sau_par_ger). */
    public record ParametroGeralUpdateRequest(
            Boolean validadeReceita,
            Integer validadeReceitaSimplesDias,
            Integer validadeReceitaUsoContinuoDias,
            Integer validadeReceitaControleEspecialDias,
            Integer inatividadeUsuarioDias,
            Integer senhaUsuarioDias,
            String secretaria,
            String secretariaEndereco,
            String secretariaCep,
            String secretariaFone1,
            String secretariaFone2,
            String secretariaEmail,
            Boolean cadastroSemCns,
            Boolean reciboComprador) {}

    /** Update the AMBULATORIAL parameters (sau_par_amb — policy flags subset). */
    public record ParametroAmbulatorialUpdateRequest(
            Boolean exigeCid10Atestado,
            Boolean estornarAtendimento,
            Integer imprimeRiscoMaterno,
            Integer atendimentoHistorico) {}
}
