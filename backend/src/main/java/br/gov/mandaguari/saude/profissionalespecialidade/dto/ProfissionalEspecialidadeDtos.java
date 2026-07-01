package br.gov.mandaguari.saude.profissionalespecialidade.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** DTOs for the SAU_PROESP sub-resource (a professional's specialties). Agenda quotas are Short (≥0). */
public final class ProfissionalEspecialidadeDtos {
    private ProfissionalEspecialidadeDtos() {}

    /** One specialty of a professional. {@code prioritario} is the boolean view of the 0/1 flag (R4). */
    public record EspecialidadeDoProfissionalResponse(
            Long profissionalId,
            Integer especialidadeId,
            boolean prioritario,
            Short situacao,
            Short agendaManhaQtd,
            Short agendaTardeQtd,
            Short agendaNoiteQtd) {}

    /** Add a specialty to a professional. The professional id comes from the path, not the body. */
    public record EspecialidadeCreateRequest(
            @NotNull Integer especialidadeId,
            Boolean prioritario,
            @PositiveOrZero Short agendaManhaQtd,
            @PositiveOrZero Short agendaTardeQtd,
            @PositiveOrZero Short agendaNoiteQtd) {}

    /** Update the flags/agenda of an existing (profissional, especialidade) link. Situação toggles active/inactive. */
    public record EspecialidadeUpdateRequest(
            Boolean prioritario,
            Short situacao,
            @PositiveOrZero Short agendaManhaQtd,
            @PositiveOrZero Short agendaTardeQtd,
            @PositiveOrZero Short agendaNoiteQtd) {}
}
