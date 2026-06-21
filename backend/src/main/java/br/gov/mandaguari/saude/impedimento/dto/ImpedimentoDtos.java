package br.gov.mandaguari.saude.impedimento.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public final class ImpedimentoDtos {

    private ImpedimentoDtos() {}

    public record ImpedimentoResponse(
            Integer codigo,
            LocalDate dataCadastro,
            LocalDate dataInicio,
            LocalDate dataFim,
            Long profissionalCodigo,
            String profissionalNome,
            Integer profissionalSituacao,
            Integer especialidadeCodigo,
            String especialidadeNome,
            String cboCode,
            String cboDescricao) {}

    public record ImpedimentoCreateRequest(
            LocalDate dataCadastro,
            @NotNull(message = "Informe a data inicial do período de Impedimento!") LocalDate dataInicio,
            @NotNull(message = "Informe a data final do período de Impedimento!") LocalDate dataFim,
            @NotNull(message = "Informe o código do Profissional!") Long profissionalCodigo,
            @NotNull(message = "Informe o código da Especialidade!") Integer especialidadeCodigo) {}

    public record ImpedimentoUpdateRequest(
            @NotNull(message = "Informe a data de cadastro do Impedimento!") LocalDate dataCadastro,
            @NotNull(message = "Informe a data inicial do período de Impedimento!") LocalDate dataInicio,
            @NotNull(message = "Informe a data final do período de Impedimento!") LocalDate dataFim,
            @NotNull(message = "Informe o código do Profissional!") Long profissionalCodigo,
            @NotNull(message = "Informe o código da Especialidade!") Integer especialidadeCodigo) {}
}
