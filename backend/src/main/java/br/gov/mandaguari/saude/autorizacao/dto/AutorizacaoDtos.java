package br.gov.mandaguari.saude.autorizacao.dto;

/** DTOs for the RBAC permission grids (SAU_PRFCON / SAU_USUCON) and the resolver check. */
public final class AutorizacaoDtos {
    private AutorizacaoDtos() {}

    /** A program permission row (uniform for profile and user grids). Flags are booleans (==1). */
    public record PermissaoResponse(
            String programaId,
            boolean consultar,
            boolean incluir,
            boolean alterar,
            boolean excluir) {}

    /** Upsert the four flags for a (owner, program) permission row. */
    public record PermissaoUpsertRequest(
            boolean consultar,
            boolean incluir,
            boolean alterar,
            boolean excluir) {}

    /** Result of a fine-grained authorization check. */
    public record CheckResponse(Integer usuCod, String programaId, String mode, boolean granted) {}
}
