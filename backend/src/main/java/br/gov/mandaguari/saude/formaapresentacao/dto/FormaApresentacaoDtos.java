package br.gov.mandaguari.saude.formaapresentacao.dto;

import jakarta.validation.constraints.Size;

public class FormaApresentacaoDtos {

    public record FormaApresentacaoCreateRequest(
            @Size(max = 30) String descricao,
            @Size(max = 5) String abreviacao
    ) {}

    public record FormaApresentacaoUpdateRequest(
            @Size(max = 30) String descricao,
            @Size(max = 5) String abreviacao
    ) {}

    public record FormaApresentacaoResponse(Integer id, String descricao, String abreviacao) {}

    public record FormaApresentacaoLookupItem(Integer id, String descricao) {}
}
