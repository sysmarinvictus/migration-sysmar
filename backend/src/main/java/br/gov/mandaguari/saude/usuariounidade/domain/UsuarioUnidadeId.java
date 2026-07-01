package br.gov.mandaguari.saude.usuariounidade.domain;

import java.io.Serializable;
import java.util.Objects;

/** Composite PK for SAU_USUUNI: (UsuCod, UniUsuCod). */
public class UsuarioUnidadeId implements Serializable {

    private Integer usuCod;
    private Integer uniCod;

    public UsuarioUnidadeId() {}

    public UsuarioUnidadeId(Integer usuCod, Integer uniCod) {
        this.usuCod = usuCod;
        this.uniCod = uniCod;
    }

    public Integer getUsuCod() { return usuCod; }
    public void setUsuCod(Integer usuCod) { this.usuCod = usuCod; }
    public Integer getUniCod() { return uniCod; }
    public void setUniCod(Integer uniCod) { this.uniCod = uniCod; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsuarioUnidadeId that)) return false;
        return Objects.equals(usuCod, that.usuCod) && Objects.equals(uniCod, that.uniCod);
    }

    @Override
    public int hashCode() { return Objects.hash(usuCod, uniCod); }
}
