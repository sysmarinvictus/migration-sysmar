package br.gov.mandaguari.saude.tipologradouro.dto;

/** Request/response DTOs for TipoLogradouro (SAU_TIPLOG). */
public final class TipoLogradouroDtos {
    private TipoLogradouroDtos() {}

    public record TipoLogradouroResponse(Integer codigo, String nome, String sigla) {}

    public record TipoLogradouroLookupItem(Integer codigo, String sigla, String nome) {}
}
