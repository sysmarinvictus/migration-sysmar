package br.gov.mandaguari.saude.local.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Request/response DTOs for Local. Clean domain names (not GeneXus attribute names). */
public final class LocalDtos {
    private LocalDtos() {}

    public record LocalResponse(
            Integer codigo,
            String nome,
            Integer municipioCodigo,
            String municipioNome,      // R4: derived read-only from SYS_MUN
            String municipioUf,        // R4
            String municipioIbge) {}   // R4

    public record LocalCreateRequest(
            // R1: client-supplied PK, GeneXus N(6,0) → range 0..999999
            @NotNull @PositiveOrZero @Max(999999) Integer codigo,
            // R2: required by the transaction (DB column is nullable)
            @NotBlank @Size(max = 50) String nome,
            // R3: required (service also rejects 0); R4: must exist in SYS_MUN
            @NotNull Integer municipioCodigo) {}

    public record LocalUpdateRequest(
            // R1: codigo is immutable (taken from the path)
            @NotBlank @Size(max = 50) String nome,
            @NotNull Integer municipioCodigo) {}

    public record LocalLookupItem(Integer codigo, String nome) {}
}
