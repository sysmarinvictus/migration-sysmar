package br.gov.mandaguari.saude.autorizacao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Per-profile program permission — maps GeneXus {@code SAU_PRFCON} (12k rows). Composite PK
 * ({@code PrfCod}, {@code PrfPrgCod}); one Inc/Alt/Exc/Con flag per (profile, program). This is the
 * primary RBAC tier (a user with a valid profile gets permissions from here — see {@code PermissionResolver}).
 */
@Entity
@Table(name = "SAU_PRFCON")
@IdClass(PerfilPermissaoId.class)
public class PerfilPermissao {

    @Id
    @Column(name = "PrfCod", nullable = false)
    private Integer perfilId;

    @Id
    @Column(name = "PrfPrgCod", length = 30, nullable = false)
    private String programaId;

    @Column(name = "PrfPrgCon")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Short consultar;

    @Column(name = "PrfPrgInc")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Short incluir;

    @Column(name = "PrfPrgAlt")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Short alterar;

    @Column(name = "PrfPrgExc")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Short excluir;

    public PerfilPermissao() {}

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

    public Integer getPerfilId() { return perfilId; }
    public void setPerfilId(Integer perfilId) { this.perfilId = perfilId; }
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
