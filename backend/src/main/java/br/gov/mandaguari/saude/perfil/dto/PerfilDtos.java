package br.gov.mandaguari.saude.perfil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request/response DTOs for Perfil. Clean domain names. No secrets/PHI (RBAC catalog). */
public final class PerfilDtos {
    private PerfilDtos() {}

    public record PerfilResponse(Integer id, String nome) {}

    public record PerfilLookupItem(Integer id, String nome) {}

    /** Create: PrfCod is auto-allocated (R1), so it is NOT accepted from the client; nome required (R2). */
    public record PerfilCreateRequest(
            @NotBlank(message = "Informe a descrição do perfil!") @Size(max = 50) String nome) {}

    public record PerfilUpdateRequest(
            @NotBlank(message = "Informe a descrição do perfil!") @Size(max = 50) String nome) {}
}
