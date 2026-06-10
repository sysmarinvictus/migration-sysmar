package br.gov.mandaguari.saude.conselhoclasse.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Request/response DTOs for Conselho de Classe. Clean domain names (not GeneXus attribute names). */
public final class ConselhoClasseDtos {
    private ConselhoClasseDtos() {}

    public record ConselhoClasseResponse(
            Short codigo,
            String sigla,
            String nome) {}

    public record ConselhoClasseCreateRequest(
            // R1: client-supplied PK, GeneXus N(3,0) → range 0..999
            @NotNull @PositiveOrZero @Max(999) Short codigo,
            // R5: sigla/nome are optional in the legacy schema (AllowNulls=Yes)
            @Size(max = 10) String sigla,
            @Size(max = 100) String nome) {}

    public record ConselhoClasseUpdateRequest(
            // R2: codigo is immutable (taken from the path); only sigla/nome change
            @Size(max = 10) String sigla,
            @Size(max = 100) String nome) {}

    public record ConselhoClasseLookupItem(Short codigo, String sigla, String nome) {}
}
