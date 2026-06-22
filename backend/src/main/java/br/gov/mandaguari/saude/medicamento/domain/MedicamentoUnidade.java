package br.gov.mandaguari.saude.medicamento.domain;

import jakarta.persistence.*;

/** SAU_REM1 — Unidade do Medicamento (estoque mínimo + situação por unidade). PK (RemCod, RemUniCod). */
@Entity
@Table(name = "SAU_REM1")
@IdClass(MedicamentoUnidadeId.class)
public class MedicamentoUnidade {

    @Id @Column(name = "RemCod", nullable = false) private Integer remCod;
    /** R46: read-only after insert. FK → SAU_UNI(UniCod). */
    @Id @Column(name = "RemUniCod", nullable = false) private Integer remUniCod;

    @Column(name = "RemEstMin") private Integer estoqueMinimo;
    @Column(name = "RemUniSit") private Short situacao;

    public Integer getRemCod() { return remCod; }
    public void setRemCod(Integer v) { this.remCod = v; }
    public Integer getRemUniCod() { return remUniCod; }
    public void setRemUniCod(Integer v) { this.remUniCod = v; }
    public Integer getEstoqueMinimo() { return estoqueMinimo; }
    public void setEstoqueMinimo(Integer v) { this.estoqueMinimo = v; }
    public Short getSituacao() { return situacao; }
    public void setSituacao(Short v) { this.situacao = v; }
}
