package br.gov.mandaguari.saude.setor.domain;

import java.io.Serializable;
import java.util.Objects;

/** Composite PK identifier for SAU_UNISETOR (UniCod + SetorCod). */
public class UniSetorId implements Serializable {

    private Integer uniCod;
    private Integer setorCod;

    public UniSetorId() {}

    public UniSetorId(Integer uniCod, Integer setorCod) {
        this.uniCod = uniCod;
        this.setorCod = setorCod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UniSetorId that)) return false;
        return Objects.equals(uniCod, that.uniCod) && Objects.equals(setorCod, that.setorCod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniCod, setorCod);
    }

    public Integer getUniCod() { return uniCod; }
    public void setUniCod(Integer uniCod) { this.uniCod = uniCod; }
    public Integer getSetorCod() { return setorCod; }
    public void setSetorCod(Integer setorCod) { this.setorCod = setorCod; }
}
