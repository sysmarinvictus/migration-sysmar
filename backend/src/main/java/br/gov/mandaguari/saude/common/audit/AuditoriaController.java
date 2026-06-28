package br.gov.mandaguari.saude.common.audit;

import br.gov.mandaguari.saude.common.audit.AuditoriaDtos.AuditoriaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Admin-only, read-only viewer over the {@code SAU_LOG} audit trail (proposed; no legacy screen —
 * the legacy {@code sau_log_impl} was a full CRUD, R11, which we deliberately do NOT reproduce).
 *
 * <p>Append-only: there is NO POST/PUT/DELETE here — the trail is written only internally via
 * {@link AuditService}. Gated by {@code ADMIN} (the dev stub user has ADMIN; OQ4 proposes a
 * dedicated {@code ADMIN_AUDITORIA} role). Every read is self-audited (R10) and the response
 * carries NO PHI columns (R8).
 */
@RestController
@RequestMapping("/api/admin/auditoria")
@Tag(name = "Auditoria (admin)")
public class AuditoriaController {

    private final AuditoriaQueryService service;

    public AuditoriaController(AuditoriaQueryService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Consultar a trilha de auditoria (SAU_LOG), paginado e filtrável. "
            + "Não retorna colunas PHI (nome/paciente/histórico).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de eventos de auditoria"),
            @ApiResponse(responseCode = "403", description = "Sem autoridade de auditoria")
    })
    public Page<AuditoriaResponse> list(
            @Parameter(description = "Nome da transação/programa (logtab), ex. SAU_ESP")
            @RequestParam(required = false) String tabela,
            @Parameter(description = "Código do usuário que agiu (logusucod)")
            @RequestParam(required = false) Integer usuarioCodigo,
            @Parameter(description = "Data/hora inicial (inclusive)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataHoraFrom,
            @Parameter(description = "Data/hora final (inclusive)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataHoraTo,
            @Parameter(description = "Chave do registro auditado (logkey)")
            @RequestParam(required = false) String chaveRegistro,
            @PageableDefault(size = 20, sort = "logdat") Pageable pageable) {
        return service.search(tabela, usuarioCodigo, dataHoraFrom, dataHoraTo, chaveRegistro, pageable);
    }
}
