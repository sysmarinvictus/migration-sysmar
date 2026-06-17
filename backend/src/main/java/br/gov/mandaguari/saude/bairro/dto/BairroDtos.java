package br.gov.mandaguari.saude.bairro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request/response DTOs for Bairro (SAU_BAI). */
public final class BairroDtos {
    private BairroDtos() {}

    public record BairroResponse(Integer codigo, String nome) {}

    /**
     * Create request — no {@code codigo}: the backend assigns it (R1 system-assigned MAX+1).
     * Only {@code nome} is accepted; it is required (R2) and unique (R3).
     */
    public record BairroCreateRequest(@NotBlank @Size(max = 50) String nome) {}

    /** Update request — {@code codigo} is immutable (taken from path). */
    public record BairroUpdateRequest(@NotBlank @Size(max = 50) String nome) {}

    public record BairroLookupItem(Integer codigo, String nome) {}
}
