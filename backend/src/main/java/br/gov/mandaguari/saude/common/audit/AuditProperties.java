package br.gov.mandaguari.saude.common.audit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the audit trail ({@code audit.*}).
 *
 * <ul>
 *   <li>{@code audit.persist.enabled} — when false, {@link AuditService} is log-only (the original
 *       behavior). Default true.</li>
 *   <li>{@code audit.empresa-codigo} — tenant written to {@code LogEmpCod} (OQ3). Default 411420,
 *       the real MunCod in the single-tenant parity environment.</li>
 *   <li>{@code audit.default-usuario-codigo} — actor written to {@code LogUsuCod} when the principal
 *       is not a numeric user id (e.g. system/anonymous). Default 0 = system.</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "audit")
public class AuditProperties {

    private final Persist persist = new Persist();
    private int empresaCodigo = 411420;
    private int defaultUsuarioCodigo = 0;

    public Persist getPersist() { return persist; }
    public int getEmpresaCodigo() { return empresaCodigo; }
    public void setEmpresaCodigo(int empresaCodigo) { this.empresaCodigo = empresaCodigo; }
    public int getDefaultUsuarioCodigo() { return defaultUsuarioCodigo; }
    public void setDefaultUsuarioCodigo(int defaultUsuarioCodigo) { this.defaultUsuarioCodigo = defaultUsuarioCodigo; }

    public static class Persist {
        private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
