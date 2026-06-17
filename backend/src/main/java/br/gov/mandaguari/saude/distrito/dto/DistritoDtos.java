package br.gov.mandaguari.saude.distrito.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request/response DTOs for Distrito Sanitário (SAU_DIS). */
public final class DistritoDtos {
    private DistritoDtos() {}

    public record DistritoCreateRequest(
            @NotBlank @Size(max = 30) String nome,
            @Size(max = 50) String endereco,
            @Min(0) @Max(9999) Short numero,                         // R21
            @Size(max = 15) String complemento,
            Integer cep,
            @Size(max = 3) String ddd,
            Integer telefone,
            Integer fax,
            @Min(0) @Max(999999) Integer tipoLogradouroCodigo,       // R21
            @Min(0) @Max(999999) Integer bairroCodigo                // R21
    ) {}

    public record DistritoUpdateRequest(
            @NotBlank @Size(max = 30) String nome,
            @Size(max = 50) String endereco,
            @Min(0) @Max(9999) Short numero,                         // R21
            @Size(max = 15) String complemento,
            Integer cep,
            @Size(max = 3) String ddd,
            Integer telefone,
            Integer fax,
            @Min(0) @Max(999999) Integer tipoLogradouroCodigo,       // R21
            @Min(0) @Max(999999) Integer bairroCodigo                // R21
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
            Integer bairroCodigo,
            String tiplogSigla,   // derived from SAU_TIPLOG.TipLogSig — R7
            String bairroNome     // derived from SAU_BAI.BaiNom — R10
    ) {}

    public record DistritoLookupItem(Short codigo, String nome) {}
}
