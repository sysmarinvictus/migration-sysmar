package br.gov.mandaguari.saude.medicamento.dto;

public class MedicamentoSubDtos {

    // ── SAU_REM1 — Unidade do Medicamento ─────────────────────────────────────
    public record UnidadeCriarRequest(Integer unidadeCodigo, Integer estoqueMinimo, Short situacao) {}
    /** R46: only estoqueMinimo + situacao are mutable. */
    public record UnidadeAtualizarRequest(Integer estoqueMinimo, Short situacao) {}
    public record UnidadeResponse(Integer remCod, Integer unidadeCodigo, Integer estoqueMinimo, Short situacao) {}

    // ── SAU_REM2 — Código de Barras (EAN-13) ──────────────────────────────────
    public record Ean13CriarRequest(Long ean13) {}
    public record Ean13Response(Integer remCod, Long ean13) {}

    // ── SAU_REM_UNISETOR — Medicamento por Unidade+Setor ──────────────────────
    public record UnidadeSetorCriarRequest(
            Integer unidadeCodigo, Integer setorCodigo, Integer estoqueMinimo, Short situacao) {}
    /** R31: PK fields (sequencia, unidadeCodigo) read-only — only estMin + situacao mutable. */
    public record UnidadeSetorAtualizarRequest(Integer estoqueMinimo, Short situacao) {}
    public record UnidadeSetorResponse(
            Integer remCod, Integer sequencia, Integer unidadeCodigo, Integer setorCodigo,
            Integer estoqueMinimo, Short situacao) {}

    // ── SAU_REMPOSO — Posologia vinculada ─────────────────────────────────────
    public record PosologiaCriarRequest(Integer posologiaCodigo) {}
    public record PosologiaResponse(Integer remCod, Integer posologiaCodigo) {}
}
