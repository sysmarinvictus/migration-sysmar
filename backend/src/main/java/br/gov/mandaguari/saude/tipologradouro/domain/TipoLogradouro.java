package br.gov.mandaguari.saude.tipologradouro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * TipoLogradouro — maps the GeneXus table {@code SAU_TIPLOG} (see SLICE-SPEC SAU_TIPLOG).
 * Address/street-type catalog (Rua, Avenida, etc.). Pre-populated reference data;
 * no GeneXus transaction form — read-only via REST (no POST/PUT/DELETE endpoints).
 */
@Entity
@Table(name = "SAU_TIPLOG")
public class TipoLogradouro {

    @Id
    @Column(name = "TipLogCod", nullable = false)
    private Integer codigo;

    @Column(name = "TipLogNom", length = 100)
    private String nome;

    @Column(name = "TipLogSig", length = 15)
    private String sigla;

    public TipoLogradouro() {}

    public Integer getCodigo() { return codigo; }
    public void setCodigo(Integer codigo) { this.codigo = codigo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getSigla() { return sigla; }
    public void setSigla(String sigla) { this.sigla = sigla; }
}
