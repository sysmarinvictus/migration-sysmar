package br.gov.mandaguari.saude.setor.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** Request/response DTOs for Setor da Unidade (SAU_UNISETOR). */
public final class UniSetorDtos {
    private UniSetorDtos() {}

    public record UniSetorCreateRequest(
            @Min(0) @Max(999999) int setorCod,                      // R2: user-entered PK component
            @NotBlank @Size(max = 50) String nome,
            @Min(0) @Max(9) short estocador,                        // R3: 0–9 range
            @Pattern(regexp = "^(ativo|inativo)$") String situacao, // R19: controlled vocab
            LocalDateTime dataInativo,
            LocalDateTime horarioInicio,
            LocalDateTime horarioFim
    ) {}

    public record UniSetorUpdateRequest(
            @NotBlank @Size(max = 50) String nome,
            @Min(0) @Max(9) short estocador,                        // R3
            @Pattern(regexp = "^(ativo|inativo)$") String situacao, // R19
            LocalDateTime dataInativo,
            LocalDateTime horarioInicio,
            LocalDateTime horarioFim
    ) {}

    public record UniSetorResponse(
            int uniCod,
            int setorCod,
            String nome,
            short estocador,
            String situacao,
            LocalDateTime dataInativo,
            LocalDateTime horarioInicio,
            LocalDateTime horarioFim,
            String unidadeNome,       // derived from SAU_UNI.UniNom (R8)
            Integer unidadeCnes,      // derived from SAU_UNI.UniCnes (R8)
            Short unidadeSituacao     // derived from SAU_UNI.UniSit  (R20)
    ) {}

    public record UniSetorLookupItem(int uniCod, int setorCod, String nome) {}
}
