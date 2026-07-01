package br.gov.mandaguari.saude.profissionalexterno.dto;

import java.time.LocalDate;

/**
 * DTOs for the "Cadastro de Profissional Externo" (SAU_PESF_PROFEXT) — a LEAN composite create of a
 * person (SYS_PES, PesTip=1) plus an external professional (SAU_PRO, ProExt=1). No CPF/address/sexo/etc.
 * (the legacy form doesn't collect them). No certificate (external professionals have none).
 */
public final class ProfissionalExternoDtos {
    private ProfissionalExternoDtos() {}

    /** Register an external professional. Required: nome, cns, município, conselho de classe + nº. */
    public record ProfissionalExternoCreateRequest(
            String nome,
            String cns,
            Integer municipioCod,
            Short conselhoClasseCod,
            String numeroConselho,   // ProNumCR
            LocalDate dataFim) {}

    /** Read-back of an external professional (person nome/município + SAU_PRO fields). */
    public record ProfissionalExternoResponse(
            Long id,
            String nome,
            String cns,
            Integer municipioCod,
            Short conselhoClasseCod,
            String numeroConselho,
            LocalDate dataInicio,
            LocalDate dataFim,
            Short situacao,
            Short externo) {}
}
