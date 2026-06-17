package br.gov.mandaguari.saude.distrito.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request/response DTOs for Distrito Sanitário (SAU_DIS). */
public final class DistritoDtos {
    private DistritoDtos() {}

    public record DistritoCreateRequest(
            @NotBlank @Size(max = 30) String nome,
            @Size(max = 50) String endereco,
            Short numero,
            @Size(max = 15) String complemento,
            Integer cep,
            @Size(max = 3) String ddd,
            Integer telefone,
            Integer fax,
            Integer tipoLogradouroCodigo,
            Integer bairroCodigo
    ) {}

    public record DistritoUpdateRequest(
            @NotBlank @Size(max = 30) String nome,
            @Size(max = 50) String endereco,
            Short numero,
            @Size(max = 15) String complemento,
            Integer cep,
            @Size(max = 3) String ddd,
            Integer telefone,
            Integer fax,
            Integer tipoLogradouroCodigo,
            Integer bairroCodigo
    ) {}

    public record DistritoResponse(
            Short codigo,
            String nome,
            String endereco,
            Short numero,
            String complemento,
            Integer cep,
            String ddd,
            Integer telefone,
            Integer fax,
            Integer tipoLogradouroCodigo,
            Integer bairroCodigo
    ) {}

    public record DistritoLookupItem(Short codigo, String nome) {}
}
