package br.gov.mandaguari.saude.medicamento.domain;

import java.io.Serializable;
import java.util.Objects;

/** Composite PK for SAU_REM2 (RemCod, RemEan13). EAN-13 is N(13,0) → Long. */
public class MedicamentoEan13Id implements Serializable {
    private Integer remCod;
    private Long ean13;

    public MedicamentoEan13Id() {}
    public MedicamentoEan13Id(Integer remCod, Long ean13) {
        this.remCod = remCod;
        this.ean13 = ean13;
    }

    public Integer getRemCod() { return remCod; }
    public void setRemCod(Integer v) { this.remCod = v; }
    public Long getEan13() { return ean13; }
    public void setEan13(Long v) { this.ean13 = v; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MedicamentoEan13Id that)) return false;
        return Objects.equals(remCod, that.remCod) && Objects.equals(ean13, that.ean13);
    }
    @Override public int hashCode() { return Objects.hash(remCod, ean13); }
}
