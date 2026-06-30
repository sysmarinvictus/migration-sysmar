package br.gov.mandaguari.saude.programa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTOs for the program catalog (SAU_PRGGRP + SAU_PRG). RBAC catalog — no PHI. */
public final class ProgramaDtos {
    private ProgramaDtos() {}

    public record GrupoProgramaResponse(Integer id, String nome) {}

    public record ProgramaResponse(
            String id,
            String nome,
            Integer grupoId,
            boolean admin,
            boolean medico,
            boolean acessoPublico) {}

    public record ProgramaLookupItem(String id, String nome) {}

    public record ProgramaCreateRequest(
            @NotBlank @Size(max = 30) String id,        // client-supplied program key (PrgCod)
            @Size(max = 100) String nome,
            Integer grupoId,
            Boolean admin,
            Boolean medico,
            Boolean acessoPublico) {}

    public record ProgramaUpdateRequest(
            @Size(max = 100) String nome,
            Integer grupoId,
            Boolean admin,
            Boolean medico,
            Boolean acessoPublico) {}
}
