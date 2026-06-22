package br.gov.mandaguari.saude.unidade.domain;

import java.io.Serializable;
import java.util.Objects;

public class HiperdiaProfissionalId implements Serializable {

    private Integer uniCod;
    private Long uniProPesCod;

    public HiperdiaProfissionalId() {}
    public HiperdiaProfissionalId(Integer uniCod, Long uniProPesCod) {
        this.uniCod = uniCod; this.uniProPesCod = uniProPesCod;
    }

    @Override public boolean equals(Object o) {
        if (!(o instanceof HiperdiaProfissionalId that)) return false;
        return Objects.equals(uniCod, that.uniCod) && Objects.equals(uniProPesCod, that.uniProPesCod);
    }
    @Override public int hashCode() { return Objects.hash(uniCod, uniProPesCod); }

    public Integer getUniCod() { return uniCod; }
    public void setUniCod(Integer v) { this.uniCod = v; }
    public Long getUniProPesCod() { return uniProPesCod; }
    public void setUniProPesCod(Long v) { this.uniProPesCod = v; }
}
