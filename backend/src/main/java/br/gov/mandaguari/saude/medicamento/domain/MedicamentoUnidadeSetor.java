package br.gov.mandaguari.saude.medicamento.domain;

import jakarta.persistence.*;

/**
 * SAU_REM_UNISETOR — Medicamento por Unidade+Setor. PK (RemCod, RemUniSetorSeq, RemUniSetorUniCod).
 * Seq is assigned from SAU_REM.RemUniSetorSeqUlt (R14); seq + unidade are read-only after insert (R31).
 */
@Entity
@Table(name = "SAU_REM_UNISETOR")
@IdClass(MedicamentoUnidadeSetorId.class)
public class MedicamentoUnidadeSetor {

    @Id @Column(name = "RemCod", nullable = false) private Integer remCod;
    @Id @Column(name = "RemUniSetorSeq", nullable = false) private Integer sequencia;
    @Id @Column(name = "RemUniSetorUniCod", nullable = false) private Integer unidadeCodigo;

    @Column(name = "RemUniSetorSetorCod") private Integer setorCodigo;
    @Column(name = "RemUniSetorEstMin") private Integer estoqueMinimo;
    @Column(name = "RemUniSetorSit", nullable = false) private Short situacao;

    public Integer getRemCod() { return remCod; }
    public void setRemCod(Integer v) { this.remCod = v; }
    public Integer getSequencia() { return sequencia; }
    public void setSequencia(Integer v) { this.sequencia = v; }
    public Integer getUnidadeCodigo() { return unidadeCodigo; }
    public void setUnidadeCodigo(Integer v) { this.unidadeCodigo = v; }
    public Integer getSetorCodigo() { return setorCodigo; }
    public void setSetorCodigo(Integer v) { this.setorCodigo = v; }
    public Integer getEstoqueMinimo() { return estoqueMinimo; }
    public void setEstoqueMinimo(Integer v) { this.estoqueMinimo = v; }
    public Short getSituacao() { return situacao; }
    public void setSituacao(Short v) { this.situacao = v; }
}
