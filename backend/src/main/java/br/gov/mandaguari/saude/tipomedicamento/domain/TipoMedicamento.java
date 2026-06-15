package br.gov.mandaguari.saude.tipomedicamento.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tipo de Medicamento — maps the existing GeneXus table {@code SAU_TIPREM} (see SLICE-SPEC
 * SAU_TIPREM). Medication-type catalog, referenced by {@code SAU_REM} (medicamento).
 *
 * <p>The code is a <b>user-entered</b> N(6,0) → {@link Integer} (Autonumber=No / Autogenerate=No).
 * {@code descricao} is nullable at the DB level (reorg DDL) but the legacy transaction requires it
 * (R2) — enforced in the DTO/service, not the column.
 */
@Entity
@Table(name = "SAU_TIPREM")
public class TipoMedicamento {

    @Id
    @Column(name = "TipRemCod", nullable = false)
    private Integer codigo;

    @Column(name = "TipRemDes", length = 50)
    private String descricao;

    public TipoMedicamento() {} // JPA + service instantiation (different package)

    public Integer getCodigo() { return codigo; }
    public void setCodigo(Integer codigo) { this.codigo = codigo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
