package br.gov.mandaguari.saude.posologia.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Request/response DTOs for Posologia (SAU_REMOBS). */
public final class PosologiaDtos {
    private PosologiaDtos() {}

    public record PosologiaResponse(
            Integer codigo,
            String descricao,
            Boolean internamento,
            BigDecimal quantidadeDose,
            Integer medidaDose,
            Short intervaloHoras,
            Short duracaoDias,
            Integer usuarioCodigo) {}

    /**
     * Create request — no {@code codigo}: the backend assigns it (R1 system-assigned MAX+1).
     * Only {@code descricao} is required (R2); all dosage fields are optional.
     */
    public record PosologiaCreateRequest(
            @NotBlank @Size(max = 60) String descricao,
            Boolean internamento,
            BigDecimal quantidadeDose,
            Integer medidaDose,
            @Min(0) @Max(99) Integer intervaloHoras,
            @Min(0) @Max(999) Integer duracaoDias) {}

    /** Update request — {@code codigo} is immutable (taken from path). */
    public record PosologiaUpdateRequest(
            @NotBlank @Size(max = 60) String descricao,
            Boolean internamento,
            BigDecimal quantidadeDose,
            Integer medidaDose,
            @Min(0) @Max(99) Integer intervaloHoras,
            @Min(0) @Max(999) Integer duracaoDias) {}

    public record PosologiaLookupItem(Integer codigo, String descricao) {}
}
