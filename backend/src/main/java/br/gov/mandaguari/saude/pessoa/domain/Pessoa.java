package br.gov.mandaguari.saude.pessoa.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

/**
 * Pessoa — the person SUPERTYPE (GeneXus {@code SYS_PES}, 89 cols, 83k rows). <b>Additive read model:</b>
 * this maps a focused read/search subset; persons are still created/edited THROUGH their subtypes
 * (SAU_PRO/SAU_FUN native access, unchanged). {@code ddl-auto=validate} only checks mapped columns, so the
 * other ~70 columns are intentionally unmapped.
 *
 * <p><b>QUARANTINED — NOT mapped:</b> {@code PesSenha}/{@code PesSenhaKey} (a reversible {@code encrypt64}
 * password, like SAU_USU — security sign-off pending; never exposed). The entity has no such field.
 *
 * <p><b>PHI:</b> every mapped field except the id is PHI. {@link #toString()} exposes only the id; PHI is
 * never logged. Social-name display (R2/R3) is applied in the service/DTO, honoring {@code PesUsaNomSoc}.
 */
@Entity
@Table(name = "SYS_PES")
public class Pessoa {

    @Id
    @Column(name = "PesCod", nullable = false)
    private Long id;

    @Column(name = "PesNom", length = 50)
    private String nome;

    /** Social name (Decreto 8.727/2016) — used for display when {@code usaNomeSocial} (R2/R3). */
    @Column(name = "PesNomSoc", length = 50)
    private String nomeSocial;

    @Column(name = "PesUsaNomSoc")
    private Boolean usaNomeSocial;

    @Column(name = "PesNomSoundex", length = 50)
    private String nomeSoundex;

    @Column(name = "PesCPFCNPJ", length = 18)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String cpfCnpj;

    @Column(name = "PesNumCns", length = 20)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String cns;

    @Column(name = "PesNasDat")
    private LocalDate dataNascimento;

    @Column(name = "PesSex", length = 1)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String sexo;

    @Column(name = "PesFon", length = 20)
    private String telefone;

    @Column(name = "PesCel", length = 20)
    private String celular;

    @Column(name = "PesEmail", length = 70)
    private String email;

    public Pessoa() {}

    /** R2: the name to DISPLAY — social name when the person opted in, else the registry name. */
    @JsonIgnore
    public String getNomeExibicao() {
        return Boolean.TRUE.equals(usaNomeSocial) && nomeSocial != null && !nomeSocial.isBlank()
                ? nomeSocial : nome;
    }

    /** R3: composite — "Social (Registro)" when social name is used, else the registry name alone. */
    @JsonIgnore
    public String getNomeCompleto() {
        return Boolean.TRUE.equals(usaNomeSocial) && nomeSocial != null && !nomeSocial.isBlank()
                ? nomeSocial + " (" + nome + ")" : nome;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getNomeSocial() { return nomeSocial; }
    public void setNomeSocial(String nomeSocial) { this.nomeSocial = nomeSocial; }
    public Boolean getUsaNomeSocial() { return usaNomeSocial; }
    public void setUsaNomeSocial(Boolean usaNomeSocial) { this.usaNomeSocial = usaNomeSocial; }
    public String getNomeSoundex() { return nomeSoundex; }
    public void setNomeSoundex(String nomeSoundex) { this.nomeSoundex = nomeSoundex; }
    public String getCpfCnpj() { return cpfCnpj == null ? null : cpfCnpj.trim(); }
    public void setCpfCnpj(String cpfCnpj) { this.cpfCnpj = cpfCnpj; }
    public String getCns() { return cns == null ? null : cns.trim(); }
    public void setCns(String cns) { this.cns = cns; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public String getSexo() { return sexo == null ? null : sexo.trim(); }
    public void setSexo(String sexo) { this.sexo = sexo; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getCelular() { return celular; }
    public void setCelular(String celular) { this.celular = celular; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    /** Only the id — PHI is never logged (R-LGPD). */
    @Override
    public String toString() { return "Pessoa{id=" + id + "}"; }
}
