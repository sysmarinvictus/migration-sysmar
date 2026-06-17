package br.gov.mandaguari.saude.bairro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Bairro — maps the GeneXus table {@code SAU_BAI} (see SLICE-SPEC SAU_BAI).
 * Neighborhood catalog referenced by SYS_PES (person address) and SAU_DIS (district).
 *
 * <p>The PK {@code BaiCod} is <b>system-assigned</b> (MAX+1 via service, not user-entered).
 * {@code BaiNom} is nullable at the DB level but required and unique by the transaction (R2/R3).
 */
@Entity
@Table(name = "SAU_BAI")
public class Bairro {

    @Id
    @Column(name = "BaiCod", nullable = false)
    private Integer codigo;

    @Column(name = "BaiNom", length = 50)
    private String nome;

    public Bairro() {}

    public Integer getCodigo() { return codigo; }
    public void setCodigo(Integer codigo) { this.codigo = codigo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
}
