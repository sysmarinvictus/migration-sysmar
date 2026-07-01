package br.gov.mandaguari.saude.receituarioespecial.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

/**
 * Item do Receituário Controle Especial — maps GeneXus {@code SAU_RECESP1} (2298 live rows), one prescribed
 * line (the controlled drug + quantity + posology) of a {@link ReceituarioEspecial}. Composite PK
 * ({@code RecEspUniCod}, {@code RecEspCod}, {@code RecEspSeq}) — the first two are the parent key, the third
 * is the per-prescription line sequence (R26). Medication/posology are raw ids validated by existence
 * queries (R18/R21), never {@code @ManyToOne}. Rules cited {@code // R<n>} (see SLICE-SPEC SAU_RECESP).
 */
@Entity
@Table(name = "SAU_RECESP1")
@IdClass(ReceituarioEspecialItemId.class)
public class ReceituarioEspecialItem {

    @Id
    @Column(name = "RecEspUniCod", nullable = false)
    private Integer unidadeId;

    @Id
    @Column(name = "RecEspCod", nullable = false)
    private Long codigo;

    @Id
    @Column(name = "RecEspSeq", nullable = false)
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer sequencia;

    @Column(name = "RemCod")
    private Integer medicamentoCodigo;          // → SAU_REM (optional, 0=free-text, R18)

    @Column(name = "RecEspPre", length = 50)
    private String prescricao;                  // required per line (R20); defaults from RemNom (R19)

    @Column(name = "RecEspQtd")
    private BigDecimal quantidade;              // NOT validated (R24)

    @Column(name = "RecEspQtdTip")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer quantidadeTipo;             // enum 1..7 (UNIDADE..ML); not enforced (R24)

    @Column(name = "RemObsCod")
    private Integer posologiaCodigo;            // → SAU_REMOBS (optional, R21); defaults RecEspObs (R22)

    @Column(name = "RecEspObs", length = 60)
    private String observacao;

    @Column(name = "RecEspTip")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer tipoReceita;                // required per line (R23): 1/2/3/4

    @Column(name = "RecEspTipUso")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer tipoUso;                     // 1 interno / 2 externo (not enforced, R24)

    @Column(name = "RecEspUsoCon")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer usoContinuo;                 // continuous-use code (not enforced, R24)

    @Column(name = "RecInd")
    private Boolean indeferido;                  // denied flag, defaults false (R25)

    public ReceituarioEspecialItem() {}

    public Integer getUnidadeId() { return unidadeId; }
    public void setUnidadeId(Integer unidadeId) { this.unidadeId = unidadeId; }
    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }
    public Integer getSequencia() { return sequencia; }
    public void setSequencia(Integer sequencia) { this.sequencia = sequencia; }
    public Integer getMedicamentoCodigo() { return medicamentoCodigo; }
    public void setMedicamentoCodigo(Integer v) { this.medicamentoCodigo = v; }
    public String getPrescricao() { return prescricao; }
    public void setPrescricao(String prescricao) { this.prescricao = prescricao; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public Integer getQuantidadeTipo() { return quantidadeTipo; }
    public void setQuantidadeTipo(Integer v) { this.quantidadeTipo = v; }
    public Integer getPosologiaCodigo() { return posologiaCodigo; }
    public void setPosologiaCodigo(Integer v) { this.posologiaCodigo = v; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public Integer getTipoReceita() { return tipoReceita; }
    public void setTipoReceita(Integer v) { this.tipoReceita = v; }
    public Integer getTipoUso() { return tipoUso; }
    public void setTipoUso(Integer tipoUso) { this.tipoUso = tipoUso; }
    public Integer getUsoContinuo() { return usoContinuo; }
    public void setUsoContinuo(Integer v) { this.usoContinuo = v; }
    public Boolean getIndeferido() { return indeferido; }
    public void setIndeferido(Boolean indeferido) { this.indeferido = indeferido; }

    @Override
    public String toString() {
        return "ReceituarioEspecialItem{uni=" + unidadeId + ", cod=" + codigo + ", seq=" + sequencia + "}";
    }
}
