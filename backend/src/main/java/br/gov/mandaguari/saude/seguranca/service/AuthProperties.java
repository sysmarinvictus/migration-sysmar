package br.gov.mandaguari.saude.seguranca.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Auth tuning knobs (safe defaults). Bound via {@link SauUsuUserDetailsService.AuthConfig}. */
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    /**
     * Case-insensitive substring that marks a SAU_PRF profile name as an "admin" profile → grants
     * ROLE_SAUDE_ADMIN + ROLE_ADMIN (OQ8). Default "ADMIN".
     */
    private String adminProfileMatch = "ADMIN";

    public String getAdminProfileMatch() { return adminProfileMatch; }
    public void setAdminProfileMatch(String adminProfileMatch) { this.adminProfileMatch = adminProfileMatch; }
}
