package br.gov.mandaguari.saude.medicamento.domain;

import java.io.Serializable;
import java.util.Objects;

/** Composite PK for SAU_REM_UNISETOR (RemCod, RemUniSetorSeq, RemUniSetorUniCod). */
public class MedicamentoUnidadeSetorId implements Serializable {
    private Integer remCod;
    private Integer sequencia;
    private Integer unidadeCodigo;

    public MedicamentoUnidadeSetorId() {}
    public MedicamentoUnidadeSetorId(Integer remCod, Integer sequencia, Integer unidadeCodigo) {
        this.remCod = remCod;
        this.sequencia = sequencia;
        this.unidadeCodigo = unidadeCodigo;
    }

    public Integer getRemCod() { return remCod; }
    public void setRemCod(Integer v) { this.remCod = v; }
    public Integer getSequencia() { return sequencia; }
    public void setSequencia(Integer v) { this.sequencia = v; }
    public Integer getUnidadeCodigo() { return unidadeCodigo; }
    public void setUnidadeCodigo(Integer v) { this.unidadeCodigo = v; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MedicamentoUnidadeSetorId that)) return false;
        return Objects.equals(remCod, that.remCod) && Objects.equals(sequencia, that.sequencia)
                && Objects.equals(unidadeCodigo, that.unidadeCodigo);
    }
    @Override public int hashCode() { return Objects.hash(remCod, sequencia, unidadeCodigo); }
}
