package br.gov.mandaguari.saude.medicamento.domain;

import java.io.Serializable;
import java.util.Objects;

/** Composite PK for SAU_REMPOSO (RemCod, PosoRemObsCod). */
public class MedicamentoPosologiaId implements Serializable {
    private Integer remCod;
    private Integer posologiaCodigo;

    public MedicamentoPosologiaId() {}
    public MedicamentoPosologiaId(Integer remCod, Integer posologiaCodigo) {
        this.remCod = remCod;
        this.posologiaCodigo = posologiaCodigo;
    }

    public Integer getRemCod() { return remCod; }
    public void setRemCod(Integer v) { this.remCod = v; }
    public Integer getPosologiaCodigo() { return posologiaCodigo; }
    public void setPosologiaCodigo(Integer v) { this.posologiaCodigo = v; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MedicamentoPosologiaId that)) return false;
        return Objects.equals(remCod, that.remCod) && Objects.equals(posologiaCodigo, that.posologiaCodigo);
    }
    @Override public int hashCode() { return Objects.hash(remCod, posologiaCodigo); }
}
