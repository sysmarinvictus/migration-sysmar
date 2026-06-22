package br.gov.mandaguari.saude.medicamento.dto;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MedicamentoDtos {

    /** Create/update payload for the main SAU_REM record. Sub-levels are managed via sub-resources. */
    public record MedicamentoCreateRequest(
            String nome,
            Integer tipoMedicamentoCodigo,
            @Size(max = 10) String dcbCodigo,
            @Size(max = 20) String renameCodigo,
            @Size(max = 20) String renameAtualCodigo,
            Integer apresentacaoCodigo,
            @Size(max = 30) String obmCodigo,
            Short tipoProduto,
            @Size(max = 150) String concentracao,
            Short farmaciaBasica,
            Short psicotropico,
            Short controleEspecial,
            Short etico,
            BigDecimal valorHospitalar,
            BigDecimal valorUnitario,
            Boolean semRename,
            @Size(max = 20) String portariaPsicotropico,
            Short situacao,
            Boolean omitirSaldo,
            Boolean usarPosologia,
            Boolean medicamentoPotencialmentePerigoso,
            @Size(max = 1000) String mppEfeitos
    ) {}

    /** Update payload — adds the MPP-cancellation motivo (R44; login/data are server-stamped). */
    public record MedicamentoUpdateRequest(
            String nome,
            Integer tipoMedicamentoCodigo,
            @Size(max = 10) String dcbCodigo,
            @Size(max = 20) String renameCodigo,
            @Size(max = 20) String renameAtualCodigo,
            Integer apresentacaoCodigo,
            @Size(max = 30) String obmCodigo,
            Short tipoProduto,
            @Size(max = 150) String concentracao,
            Short farmaciaBasica,
            Short psicotropico,
            Short controleEspecial,
            Short etico,
            BigDecimal valorHospitalar,
            BigDecimal valorUnitario,
            Boolean semRename,
            @Size(max = 20) String portariaPsicotropico,
            Short situacao,
            Boolean omitirSaldo,
            Boolean usarPosologia,
            Boolean medicamentoPotencialmentePerigoso,
            @Size(max = 1000) String mppEfeitos,
            @Size(max = 300) String mppCancelamentoMotivo
    ) {}

    public record MedicamentoResponse(
            Integer id,
            String nome,
            Integer tipoMedicamentoCodigo,
            String dcbCodigo,
            String renameCodigo,
            String renameAtualCodigo,
            Integer apresentacaoCodigo,
            String obmCodigo,
            Short tipoProduto,
            String concentracao,
            Short farmaciaBasica,
            Short psicotropico,
            Short controleEspecial,
            Short etico,
            BigDecimal valorHospitalar,
            BigDecimal valorUnitario,
            Boolean semRename,
            String portariaPsicotropico,
            Short situacao,
            Boolean omitirSaldo,
            Boolean usarPosologia,
            Boolean medicamentoPotencialmentePerigoso,
            String mppEfeitos,
            String mppCancelamentoMotivo,
            LocalDateTime mppCancelamentoData,
            String usuarioLogin,
            // derived (not persisted)
            String renameDescricao,   // R12
            long posologiaCount       // R13
    ) {}

    public record MedicamentoLookupItem(Integer id, String nome) {}
}
