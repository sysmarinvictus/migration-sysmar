package br.gov.mandaguari.saude.posologia.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Posologia — maps the GeneXus table {@code SAU_REMOBS} (see SLICE-SPEC SAU_REMOBS).
 * Dosage-instructions catalog referenced by SAU_REMPOSO (medication-posology link) and
 * SAU_RECESP1 (controlled-substance prescription items).
 *
 * <p>The PK {@code RemObsCod} is <b>system-assigned</b> (MAX+1 via service, not user-entered).
 * {@code RemObsDes} is nullable at the DB level but required by the transaction (R2).
 * All other columns are optional.
 */
@Entity
@Table(name = "SAU_REMOBS")
public class Posologia {

    @Id
    @Column(name = "RemObsCod", nullable = false)
    private Integer codigo;

    @Column(name = "RemObsDes", length = 60)
    private String descricao;

    @Column(name = "RemObsInternamento")
    private Boolean internamento;

    @Column(name = "RemObsQuantidadeDose", precision = 8, scale = 2)
    private BigDecimal quantidadeDose;

    @Column(name = "RemObsMedidaDose")
    private Integer medidaDose;

    /** N(2,0) — range 0..99. DB SMALLINT → Short (Hibernate maps Short to int2). */
    @Column(name = "RemObsIntervaloHoras")
    private Short intervaloHoras;

    /** N(3,0) — range 0..999. DB SMALLINT → Short (Hibernate maps Short to int2). */
    @Column(name = "RemObsDuracaoDias")
    private Short duracaoDias;

    /** Soft-FK to SAU_USU (no DDL constraint). System-set on INSERT; null in Wave 1. */
    @Column(name = "RemObsUsuCod")
    private Integer usuarioCodigo;

    public Posologia() {}

    public Integer getCodigo() { return codigo; }
    public void setCodigo(Integer codigo) { this.codigo = codigo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Boolean getInternamento() { return internamento; }
    public void setInternamento(Boolean internamento) { this.internamento = internamento; }
    public BigDecimal getQuantidadeDose() { return quantidadeDose; }
    public void setQuantidadeDose(BigDecimal quantidadeDose) { this.quantidadeDose = quantidadeDose; }
    public Integer getMedidaDose() { return medidaDose; }
    public void setMedidaDose(Integer medidaDose) { this.medidaDose = medidaDose; }
    public Short getIntervaloHoras() { return intervaloHoras; }
    public void setIntervaloHoras(Short intervaloHoras) { this.intervaloHoras = intervaloHoras; }
    public Short getDuracaoDias() { return duracaoDias; }
    public void setDuracaoDias(Short duracaoDias) { this.duracaoDias = duracaoDias; }
    public Integer getUsuarioCodigo() { return usuarioCodigo; }
    public void setUsuarioCodigo(Integer usuarioCodigo) { this.usuarioCodigo = usuarioCodigo; }
}
