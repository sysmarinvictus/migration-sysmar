package br.gov.mandaguari.saude.common.audit;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTOs for the admin audit viewer.
 *
 * <p><b>LGPD:</b> {@link AuditoriaResponse} deliberately omits every PHI column
 * ({@code nomePaciente}, {@code pacienteCodigo}, {@code historico}, {@code nomeProfissional},
 * {@code situacao}) — they are never exposed over the API.
 */
public final class AuditoriaDtos {

    private AuditoriaDtos() {}

    /** Read-only projection of one {@code SAU_LOG} row. No PHI fields (R8). */
    @Schema(description = "Linha da trilha de auditoria (sem colunas PHI)")
    public record AuditoriaResponse(
            Integer empresaCodigo,
            LocalDateTime dataHora,
            Integer usuarioCodigo,
            String operacao,
            String tabela,
            String chaveRegistro,
            Long profissionalCodigo,
            Integer unidadeCodigo) {

        static AuditoriaResponse from(LogAuditoria l) {
            return new AuditoriaResponse(
                    l.getEmpresaCodigo(),
                    l.getDataHora(),
                    l.getUsuarioCodigo(),
                    l.getOperacao(),
                    l.getTabela(),
                    l.getChaveRegistro() == null ? null : l.getChaveRegistro().trim(),
                    l.getProfissionalCodigo(),
                    l.getUnidadeCodigo());
        }
    }
}
