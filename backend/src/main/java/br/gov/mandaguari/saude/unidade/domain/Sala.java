package br.gov.mandaguari.saude.unidade.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;
import java.time.LocalDateTime;

@Entity
@Table(name = "SAU_UNISALA")
@IdClass(SalaId.class)
public class Sala {

    @Id @Column(name = "UniCod", nullable = false) private Integer uniCod;
    /** R79: read-only after insert. */
    @Id @Column(name = "SalaCod", nullable = false) private Short salaCod;

    @Column(name = "SalaNom", length = 100) private String nome;
    /** OQ9: values unknown — single char accepted. */
    @Column(name = "SalaSta", length = 1) @JdbcTypeCode(Types.CHAR) private String status;
    /** R82: auto-stamped by service. */
    @Column(name = "SalaDatAlt") private LocalDateTime datAlteracao;
    /** R82: auto-stamped by service from security context. */
    @Column(name = "SalaUsuLogin", length = 40) private String usuLogin;

    public Integer getUniCod() { return uniCod; }
    public void setUniCod(Integer v) { this.uniCod = v; }
    public Short getSalaCod() { return salaCod; }
    public void setSalaCod(Short v) { this.salaCod = v; }
    public String getNome() { return nome; }
    public void setNome(String v) { this.nome = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public LocalDateTime getDatAlteracao() { return datAlteracao; }
    public void setDatAlteracao(LocalDateTime v) { this.datAlteracao = v; }
    public String getUsuLogin() { return usuLogin; }
    public void setUsuLogin(String v) { this.usuLogin = v; }
}
