package br.gov.mandaguari.saude.autorizacao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Per-user program permission — maps GeneXus {@code SAU_USUCON} (~1.5M rows). Composite PK
 * ({@code UsuCod}, {@code PrgCod}); one Inc/Alt/Exc/Con flag per (user, program). This is the
 * FALLBACK RBAC tier — consulted only when the user has no valid profile (see {@code PermissionResolver}).
 */
@Entity
@Table(name = "SAU_USUCON")
@IdClass(UsuarioPermissaoId.class)
public class UsuarioPermissao {

    @Id
    @Column(name = "UsuCod", nullable = false)
    private Integer usuarioId;

    @Id
    @Column(name = "PrgCod", length = 30, nullable = false)
    private String programaId;

    @Column(name = "UsuCon")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Short consultar;

    @Column(name = "UsuInc")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Short incluir;

    @Column(name = "UsuAlt")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Short alterar;

    @Column(name = "UsuExc")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Short excluir;

    public UsuarioPermissao() {}

    /** Whether this row grants the given mode (flag == 1). */
    public boolean granted(Mode mode) {
        Short f = switch (mode) {
            case CON -> consultar;
            case INC -> incluir;
            case ALT -> alterar;
            case EXC -> excluir;
        };
        return f != null && f == 1;
    }

    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }
    public String getProgramaId() { return programaId; }
    public void setProgramaId(String programaId) { this.programaId = programaId; }
    public Short getConsultar() { return consultar; }
    public void setConsultar(Short consultar) { this.consultar = consultar; }
    public Short getIncluir() { return incluir; }
    public void setIncluir(Short incluir) { this.incluir = incluir; }
    public Short getAlterar() { return alterar; }
    public void setAlterar(Short alterar) { this.alterar = alterar; }
    public Short getExcluir() { return excluir; }
    public void setExcluir(Short excluir) { this.excluir = excluir; }
}
