package br.gov.mandaguari.saude.autorizacao.domain;

import java.io.Serializable;
import java.util.Objects;

/** Composite PK for SAU_PRFCON: (PrfCod, PrfPrgCod). */
public class PerfilPermissaoId implements Serializable {

    private Integer perfilId;
    private String programaId;

    public PerfilPermissaoId() {}

    public PerfilPermissaoId(Integer perfilId, String programaId) {
        this.perfilId = perfilId;
        this.programaId = programaId;
    }

    public Integer getPerfilId() { return perfilId; }
    public void setPerfilId(Integer perfilId) { this.perfilId = perfilId; }
    public String getProgramaId() { return programaId; }
    public void setProgramaId(String programaId) { this.programaId = programaId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PerfilPermissaoId that)) return false;
        return Objects.equals(perfilId, that.perfilId) && Objects.equals(programaId, that.programaId);
    }

    @Override
    public int hashCode() { return Objects.hash(perfilId, programaId); }
}
