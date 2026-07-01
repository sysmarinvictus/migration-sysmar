package br.gov.mandaguari.saude.receituarioespecial.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTOs for Receituário Controle Especial (SAU_RECESP master + SAU_RECESP1 items). Derived/audit fields
 * ({@code RecEspCod} allocation, {@code RecEspUsuLogin}, {@code RecEspSeq}, {@code RecEspSeqUlt}) are
 * system-set and NOT accepted from requests. PHI-dense (patient + prescribed controlled drug) — responses
 * are SAUDE_CADASTRO-only + audited.
 */
public final class ReceituarioEspecialDtos {
    private ReceituarioEspecialDtos() {}

    /** Grid row. */
    public record ReceituarioEspecialListItem(
            Integer unidadeCodigo, Long numero, LocalDate data,
            Long pacienteCodigo, String pacienteNome, Long prescritorCodigo) {}

    /** One prescribed line (SAU_RECESP1). */
    public record ItemResponse(
            Integer sequencia, Integer medicamentoCodigo, String prescricao, BigDecimal quantidade,
            Integer quantidadeTipo, Integer posologiaCodigo, String observacao, Integer tipoReceita,
            Integer tipoUso, Integer usoContinuo, boolean indeferido) {}

    /** Full prescription: master + items + load-time derivations + non-blocking warnings (R13/R27). */
    public record ReceituarioEspecialResponse(
            Integer unidadeCodigo, Long numero, LocalDate data,
            Long pacienteCodigo, String pacienteNome, String pacienteNomeExibicao, Integer pacienteIdade,
            Long prescritorCodigo, Long funcionarioCodigo,
            Integer situacao, Integer tipoReceituario, String observacao,
            List<ItemResponse> itens, List<String> avisos) {}

    /** Create/update payload for a prescribed line. sequencia is assigned by the server (R26). */
    public record ItemRequest(
            Integer medicamentoCodigo, String prescricao, BigDecimal quantidade, Integer quantidadeTipo,
            Integer posologiaCodigo, String observacao, Integer tipoReceita, Integer tipoUso,
            Integer usoContinuo, Boolean indeferido) {}

    /**
     * Create/update payload. On create, {@code unidadeCodigo} is required and {@code numero} is allocated
     * server-side (R1); on update the unit+numero come from the path. {@code itens} is the full line set.
     */
    public record ReceituarioEspecialWriteRequest(
            Integer unidadeCodigo, LocalDate data,
            Long pacienteCodigo, Long prescritorCodigo, Long funcionarioCodigo,
            Integer situacao, Integer tipoReceituario, String observacao,
            List<ItemRequest> itens) {}
}
