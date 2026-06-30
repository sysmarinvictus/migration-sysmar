package br.gov.mandaguari.saude.autorizacao.domain;

import java.io.Serializable;
import java.util.Objects;

/** Composite PK for SAU_USUCON: (UsuCod, PrgCod). */
public class UsuarioPermissaoId implements Serializable {

    private Integer usuarioId;
    private String programaId;

    public UsuarioPermissaoId() {}

    public UsuarioPermissaoId(Integer usuarioId, String programaId) {
        this.usuarioId = usuarioId;
        this.programaId = programaId;
    }

    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }
    public String getProgramaId() { return programaId; }
    public void setProgramaId(String programaId) { this.programaId = programaId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsuarioPermissaoId that)) return false;
        return Objects.equals(usuarioId, that.usuarioId) && Objects.equals(programaId, that.programaId);
    }

    @Override
    public int hashCode() { return Objects.hash(usuarioId, programaId); }
}
