package br.gov.mandaguari.saude.especialidade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Request/response DTOs for Especialidade. Clean domain names (not GeneXus attribute names). */
public final class EspecialidadeDtos {
    private EspecialidadeDtos() {}

    /** Scheduling-queue parameters per urgency tier (estagnado / tempo-máximo / vaga min-max). */
    public record AgendaParametros(
            @PositiveOrZero Integer estagnadoMuitoUrgente,
            @PositiveOrZero Integer estagnadoUrgente,
            @PositiveOrZero Integer estagnadoPrioritario,
            @PositiveOrZero Integer estagnadoNormal,
            @PositiveOrZero Integer tempoMaxMuitoUrgente,
            @PositiveOrZero Integer tempoMaxUrgente,
            @PositiveOrZero Integer tempoMaxPrioritario,
            @PositiveOrZero Integer tempoMaxNormal,
            @PositiveOrZero Integer vagaMuitoUrgenteMin,
            @PositiveOrZero Integer vagaMuitoUrgenteMax,
            @PositiveOrZero Integer vagaUrgenteMin,
            @PositiveOrZero Integer vagaUrgenteMax,
            @PositiveOrZero Integer vagaPrioritarioMin,
            @PositiveOrZero Integer vagaPrioritarioMax,
            @PositiveOrZero Integer vagaNormalMin,
            @PositiveOrZero Integer vagaNormalMax) {}

    public record EspecialidadeResponse(
            Integer codigo,
            String nome,
            String situacao,
            Boolean auxiliar,
            Integer cborCodigo,
            String cborDescricao,      // R3: derived read-only from SAU_CBOR
            AgendaParametros agenda) {}

    public record EspecialidadeCreateRequest(
            @NotNull @Positive Integer codigo,      // R2: client-supplied PK (GeneXus code)
            @NotBlank @Size(max = 50) String nome,  // R1
            @Size(max = 1) String situacao,
            Boolean auxiliar,
            Integer cborCodigo,                      // R3
            AgendaParametros agenda) {}

    public record EspecialidadeUpdateRequest(
            @NotBlank @Size(max = 50) String nome,   // R1 (codigo is immutable — R2)
            @Size(max = 1) String situacao,
            Boolean auxiliar,
            Integer cborCodigo,
            AgendaParametros agenda) {}

    public record EspecialidadeLookupItem(Integer codigo, String nome) {}
}
