package br.gov.mandaguari.saude.unidade.domain;

import java.io.Serializable;
import java.util.Objects;

public class SalaId implements Serializable {

    private Integer uniCod;
    private Short salaCod;

    public SalaId() {}
    public SalaId(Integer uniCod, Short salaCod) { this.uniCod = uniCod; this.salaCod = salaCod; }

    @Override public boolean equals(Object o) {
        if (!(o instanceof SalaId that)) return false;
        return Objects.equals(uniCod, that.uniCod) && Objects.equals(salaCod, that.salaCod);
    }
    @Override public int hashCode() { return Objects.hash(uniCod, salaCod); }

    public Integer getUniCod() { return uniCod; }
    public void setUniCod(Integer v) { this.uniCod = v; }
    public Short getSalaCod() { return salaCod; }
    public void setSalaCod(Short v) { this.salaCod = v; }
}
