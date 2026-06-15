package br.gov.mandaguari.saude.local.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Local (locality) — maps the existing GeneXus table {@code SAU_LOC} (see SLICE-SPEC SAU_LOC).
 *
 * <p>A Local belongs to a Município ({@code SYS_MUN}); the FK is kept as a raw id
 * ({@code municipioCodigo}) because SYS_MUN is an un-migrated system table — its name/UF/IBGE are
 * derived via a lookup query (see {@code LocalRepository}). The code is a <b>user-entered</b> N(6,0)
 * → {@link Integer} (Autonumber=No / Autogenerate=No).
 *
 * <p>{@code nome}/{@code municipioCodigo} are nullable at the DB level (reorg DDL), but the legacy
 * transaction requires both (R2/R3) — enforced in the DTO/service, not the column.
 */
@Entity
@Table(name = "SAU_LOC")
public class Local {

    @Id
    @Column(name = "LocCod", nullable = false)
    private Integer codigo;

    @Column(name = "LocNom", length = 50)
    private String nome;

    @Column(name = "LocMunCod")
    private Integer municipioCodigo;

    public Local() {} // JPA + service instantiation (different package)

    public Integer getCodigo() { return codigo; }
    public void setCodigo(Integer codigo) { this.codigo = codigo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Integer getMunicipioCodigo() { return municipioCodigo; }
    public void setMunicipioCodigo(Integer municipioCodigo) { this.municipioCodigo = municipioCodigo; }
}
