package br.gov.mandaguari.saude.common.audit;

import br.gov.mandaguari.saude.common.audit.AuditoriaDtos.AuditoriaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Read-side service for the admin audit viewer. Self-audits each query (R10) and maps rows to the
 * PHI-free {@link AuditoriaResponse}.
 */
@Service
public class AuditoriaQueryService {

    private final LogAuditoriaRepository repository;
    private final AuditService audit;

    public AuditoriaQueryService(LogAuditoriaRepository repository, AuditService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    /** Paginated, filtered read over SAU_LOG. The read itself is audited (VIEW on SAU_LOG). */
    @Transactional(readOnly = true)
    public Page<AuditoriaResponse> search(String tabela, Integer usuarioCodigo,
                                          LocalDateTime dataHoraFrom, LocalDateTime dataHoraTo,
                                          String chaveRegistro, Pageable pageable) {
        String tab = (tabela == null || tabela.isBlank()) ? null : tabela.trim().toUpperCase(Locale.ROOT);
        String key = (chaveRegistro == null || chaveRegistro.isBlank()) ? null : chaveRegistro.trim();

        Page<AuditoriaResponse> page = repository
                .findByFilters(tab, usuarioCodigo, dataHoraFrom, dataHoraTo, key, pageable)
                .map(AuditoriaResponse::from);

        // R10: reading the trail (PHI-adjacent, 7.7M rows) is itself an audited event.
        audit.record("VIEW", "SAU_LOG",
                "tab=" + tab + ";usu=" + usuarioCodigo + ";n=" + page.getTotalElements());
        return page;
    }
}
