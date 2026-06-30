package br.gov.mandaguari.saude.perfil.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Perfil — RBAC access-profile tier. Maps the existing GeneXus table {@code SAU_PRF} (2 columns,
 * PK {@code PrfCod}, ZERO physical FKs). Read read-only by SAU_USU ({@code UsuPrfCod}); the
 * per-program permission grid lives in SAU_PRFCON (its own slice).
 *
 * <p><b>PrfCod is server-allocated</b> ({@code MAX(PrfCod)+1}, legacy {@code psau_inc_prf}) — the live DB
 * has no sequence/identity, so this is intentionally NOT {@code @GeneratedValue}; the service assigns it.
 * <b>PrfNom</b> is nullable in the DB but app-required (R2, enforced in the DTO/service) and stored
 * uppercase (R3).
 */
@Entity
@Table(name = "SAU_PRF")
public class Perfil {

    @Id
    @Column(name = "PrfCod", nullable = false)
    private Integer id;

    @Column(name = "PrfNom", length = 50)
    private String nome;

    public Perfil() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    @Override
    public String toString() {
        return "Perfil{id=" + id + ", nome='" + nome + "'}";
    }
}
