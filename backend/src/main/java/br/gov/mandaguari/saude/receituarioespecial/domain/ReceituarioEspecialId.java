package br.gov.mandaguari.saude.receituarioespecial.domain;

import java.io.Serializable;
import java.util.Objects;

/** Composite PK for SAU_RECESP: (RecEspUniCod, RecEspCod) — unit + per-unit sequential prescription number. */
public class ReceituarioEspecialId implements Serializable {

    private Integer unidadeId;
    private Long codigo;

    public ReceituarioEspecialId() {}

    public ReceituarioEspecialId(Integer unidadeId, Long codigo) {
        this.unidadeId = unidadeId;
        this.codigo = codigo;
    }

    public Integer getUnidadeId() { return unidadeId; }
    public void setUnidadeId(Integer unidadeId) { this.unidadeId = unidadeId; }
    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReceituarioEspecialId that)) return false;
        return Objects.equals(unidadeId, that.unidadeId) && Objects.equals(codigo, that.codigo);
    }

    @Override
    public int hashCode() { return Objects.hash(unidadeId, codigo); }
}
