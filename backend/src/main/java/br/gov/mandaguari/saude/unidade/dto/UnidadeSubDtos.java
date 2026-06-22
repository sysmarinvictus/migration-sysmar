package br.gov.mandaguari.saude.unidade.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class UnidadeSubDtos {

    // ── SAU_UNI1 — Hiperdia ──────────────────────────────────────────────────

    public record HiperdiaCriarRequest(
            Long profissionalId,
            LocalDate dataInclusao,
            String matricula,
            String cbo,
            Short status,
            LocalDate dataDesativacao
    ) {}

    public record HiperdiaResponse(
            Integer uniCod,
            Long profissionalId,
            LocalDate dataInclusao,
            String matricula,
            String cbo,
            Short status,
            LocalDate dataDesativacao
    ) {}

    // ── SAU_UNI2 — SisPré-Natal ──────────────────────────────────────────────

    public record SisPreNatalCriarRequest(
            Long profissionalId,
            Integer especialidadeId,
            LocalDate dataInclusao,
            Short status,
            LocalDate dataDesativacao
    ) {}

    public record SisPreNatalResponse(
            Integer uniCod,
            Long profissionalId,
            Integer especialidadeId,
            LocalDate dataInclusao,
            Short status,
            LocalDate dataDesativacao
    ) {}

    // ── SAU_UNI3 — Nutricionistas ─────────────────────────────────────────────

    public record NutricionistaCriarRequest(
            Long profissionalId,
            Integer especialidadeId,
            LocalDate dataInclusao,
            Short status,
            LocalDate dataDesativacao
    ) {}

    public record NutricionistaResponse(
            Integer uniCod,
            Long profissionalId,
            Integer especialidadeId,
            LocalDate dataInclusao,
            Short status,
            LocalDate dataDesativacao
    ) {}

    // ── SAU_UNISALA — Salas ────────────────────────────────────────────────────

    public record SalaCriarRequest(Short salaCodigo, String nome, String status) {}

    public record SalaAtualizarRequest(String nome, String status) {}

    public record SalaResponse(
            Integer uniCod,
            Short salaCodigo,
            String nome,
            String status,
            LocalDateTime dataAlteracao,
            String usuarioLogin
    ) {}
}
