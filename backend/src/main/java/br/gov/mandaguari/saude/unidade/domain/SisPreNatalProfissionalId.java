package br.gov.mandaguari.saude.unidade.domain;

import java.io.Serializable;
import java.util.Objects;

public class SisPreNatalProfissionalId implements Serializable {

    private Integer uniCod;
    private Long uniGesProPesCod;
    private Integer uniGesEspCod;

    public SisPreNatalProfissionalId() {}
    public SisPreNatalProfissionalId(Integer uniCod, Long uniGesProPesCod, Integer uniGesEspCod) {
        this.uniCod = uniCod; this.uniGesProPesCod = uniGesProPesCod; this.uniGesEspCod = uniGesEspCod;
    }

    @Override public boolean equals(Object o) {
        if (!(o instanceof SisPreNatalProfissionalId that)) return false;
        return Objects.equals(uniCod, that.uniCod)
            && Objects.equals(uniGesProPesCod, that.uniGesProPesCod)
            && Objects.equals(uniGesEspCod, that.uniGesEspCod);
    }
    @Override public int hashCode() { return Objects.hash(uniCod, uniGesProPesCod, uniGesEspCod); }

    public Integer getUniCod() { return uniCod; }
    public void setUniCod(Integer v) { this.uniCod = v; }
    public Long getUniGesProPesCod() { return uniGesProPesCod; }
    public void setUniGesProPesCod(Long v) { this.uniGesProPesCod = v; }
    public Integer getUniGesEspCod() { return uniGesEspCod; }
    public void setUniGesEspCod(Integer v) { this.uniGesEspCod = v; }
}
