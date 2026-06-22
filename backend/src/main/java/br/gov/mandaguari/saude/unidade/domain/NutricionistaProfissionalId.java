package br.gov.mandaguari.saude.unidade.domain;

import java.io.Serializable;
import java.util.Objects;

public class NutricionistaProfissionalId implements Serializable {

    private Integer uniCod;
    private Long uniNutProPesCod;
    private Integer uniNutEspCod;

    public NutricionistaProfissionalId() {}
    public NutricionistaProfissionalId(Integer uniCod, Long uniNutProPesCod, Integer uniNutEspCod) {
        this.uniCod = uniCod; this.uniNutProPesCod = uniNutProPesCod; this.uniNutEspCod = uniNutEspCod;
    }

    @Override public boolean equals(Object o) {
        if (!(o instanceof NutricionistaProfissionalId that)) return false;
        return Objects.equals(uniCod, that.uniCod)
            && Objects.equals(uniNutProPesCod, that.uniNutProPesCod)
            && Objects.equals(uniNutEspCod, that.uniNutEspCod);
    }
    @Override public int hashCode() { return Objects.hash(uniCod, uniNutProPesCod, uniNutEspCod); }

    public Integer getUniCod() { return uniCod; }
    public void setUniCod(Integer v) { this.uniCod = v; }
    public Long getUniNutProPesCod() { return uniNutProPesCod; }
    public void setUniNutProPesCod(Long v) { this.uniNutProPesCod = v; }
    public Integer getUniNutEspCod() { return uniNutEspCod; }
    public void setUniNutEspCod(Integer v) { this.uniNutEspCod = v; }
}
