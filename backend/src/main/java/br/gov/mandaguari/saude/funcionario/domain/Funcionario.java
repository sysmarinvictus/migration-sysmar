package br.gov.mandaguari.saude.funcionario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Funcionário — employee. Maps GeneXus {@code SAU_FUN} (1148 rows). A SYS_PES person-subtype exactly
 * like SAU_PRO: the PK {@code FunPesCod} <b>is</b> {@code SYS_PES.PesCod}, so it is user-selected (an
 * existing Pessoa), NOT generated. Only 5 columns live on SAU_FUN; name/CPF/phones live in SYS_PES and
 * are read via native projection + written back via native UPDATE (no SYS_PES JPA entity yet).
 *
 * <p><b>PHI:</b> {@code nomeSoundex} is derived from the person name and is excluded from {@link #toString()}
 * (R18 — the legacy app logged it; that defect is NOT ported). Person identity never appears here.
 */
@Entity
@Table(name = "SAU_FUN")
public class Funcionario {

    /** = SYS_PES.PesCod. User-supplied existing person; NOT {@code @GeneratedValue}. */
    @Id
    @Column(name = "FunPesCod", nullable = false)
    private Long id;

    @Column(name = "FunTraFon", length = 20)
    private String telefoneTrabalho;

    /** CHAR(10) — fixed-length, space-padded in PG; trimmed on read. Free text, no validation (R9). */
    @Column(name = "FunTraRam", length = 10)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String ramal;

    /** Phonetic key recomputed server-side from SYS_PES.PesNom on every confirm (R3). Read-only. */
    @Column(name = "FunPesNomSoundex", length = 50)
    private String nomeSoundex;

    /** SMALLINT. 1=Ativo / 2=Inativo. App-default 1 on insert (R5; no DB default). */
    @Column(name = "FunSit")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Short situacao;

    public Funcionario() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTelefoneTrabalho() { return telefoneTrabalho; }
    public void setTelefoneTrabalho(String telefoneTrabalho) { this.telefoneTrabalho = telefoneTrabalho; }
    public String getRamal() { return ramal; }
    public void setRamal(String ramal) { this.ramal = ramal; }
    public String getNomeSoundex() { return nomeSoundex; }
    public void setNomeSoundex(String nomeSoundex) { this.nomeSoundex = nomeSoundex; }
    public Short getSituacao() { return situacao; }
    public void setSituacao(Short situacao) { this.situacao = situacao; }

    /** nomeSoundex (PHI-derived) is intentionally excluded (R18). */
    @Override
    public String toString() {
        return "Funcionario{id=" + id + ", situacao=" + situacao + "}";
    }
}
