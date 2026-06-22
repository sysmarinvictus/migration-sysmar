package br.gov.mandaguari.saude.medicamento.domain;

import java.io.Serializable;
import java.util.Objects;

/** Composite PK for SAU_REM1 (RemCod, RemUniCod). */
public class MedicamentoUnidadeId implements Serializable {
    private Integer remCod;
    private Integer remUniCod;

    public MedicamentoUnidadeId() {}
    public MedicamentoUnidadeId(Integer remCod, Integer remUniCod) {
        this.remCod = remCod;
        this.remUniCod = remUniCod;
    }

    public Integer getRemCod() { return remCod; }
    public void setRemCod(Integer v) { this.remCod = v; }
    public Integer getRemUniCod() { return remUniCod; }
    public void setRemUniCod(Integer v) { this.remUniCod = v; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MedicamentoUnidadeId that)) return false;
        return Objects.equals(remCod, that.remCod) && Objects.equals(remUniCod, that.remUniCod);
    }
    @Override public int hashCode() { return Objects.hash(remCod, remUniCod); }
}
