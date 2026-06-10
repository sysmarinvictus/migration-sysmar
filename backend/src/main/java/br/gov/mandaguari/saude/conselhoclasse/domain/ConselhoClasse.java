package br.gov.mandaguari.saude.conselhoclasse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Conselho de Classe — professional licensing board (e.g. CRM, COREN, CRF). Maps the existing
 * GeneXus table {@code SAU_CONCLA} (see SLICE-SPEC SAU_CONCLA).
 *
 * <p>Physical column names are pinned to the GeneXus names. The code is a <b>user-entered</b>
 * {@code smallint} (GeneXus N(3,0); Autonumber=No / Autogenerate=No) — hence {@link Short}, not a
 * generated id. {@code sigla} and {@code nome} are nullable in the legacy schema (AllowNulls=Yes).
 *
 * <p>Types come from the GeneXus reorg DDL ({@code SAU_CONCLAConversion.xml}), not live-DB
 * introspection — {@code ddl-auto=validate} will flag any mismatch against production.
 */
@Entity
@Table(name = "SAU_CONCLA")
public class ConselhoClasse {

    @Id
    @Column(name = "ConClaCod", nullable = false)
    private Short codigo;

    @Column(name = "ConClaSigra", length = 10)
    private String sigla;

    @Column(name = "ConClaNom", length = 100)
    private String nome;

    protected ConselhoClasse() {} // JPA

    public Short getCodigo() { return codigo; }
    public void setCodigo(Short codigo) { this.codigo = codigo; }
    public String getSigla() { return sigla; }
    public void setSigla(String sigla) { this.sigla = sigla; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
}
