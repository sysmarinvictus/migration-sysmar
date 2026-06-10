package br.gov.mandaguari.saude.security;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** Auth request/response payloads. */
public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record TokenResponse(String accessToken, String refreshToken, String tokenType,
                                String username, List<String> roles) {
        public static TokenResponse bearer(String access, String refresh, String user, List<String> roles) {
            return new TokenResponse(access, refresh, "Bearer", user, roles);
        }
    }
}
