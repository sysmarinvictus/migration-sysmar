package br.gov.mandaguari.saude.receituarioespecial.domain;

import java.io.Serializable;
import java.util.Objects;

/** Composite PK for SAU_RECESP1 (line item): (RecEspUniCod, RecEspCod, RecEspSeq). */
public class ReceituarioEspecialItemId implements Serializable {

    private Integer unidadeId;
    private Long codigo;
    private Integer sequencia;

    public ReceituarioEspecialItemId() {}

    public ReceituarioEspecialItemId(Integer unidadeId, Long codigo, Integer sequencia) {
        this.unidadeId = unidadeId;
        this.codigo = codigo;
        this.sequencia = sequencia;
    }

    public Integer getUnidadeId() { return unidadeId; }
    public void setUnidadeId(Integer unidadeId) { this.unidadeId = unidadeId; }
    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }
    public Integer getSequencia() { return sequencia; }
    public void setSequencia(Integer sequencia) { this.sequencia = sequencia; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReceituarioEspecialItemId that)) return false;
        return Objects.equals(unidadeId, that.unidadeId)
                && Objects.equals(codigo, that.codigo)
                && Objects.equals(sequencia, that.sequencia);
    }

    @Override
    public int hashCode() { return Objects.hash(unidadeId, codigo, sequencia); }
}
