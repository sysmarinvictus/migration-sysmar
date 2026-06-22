package br.gov.mandaguari.saude.unidade.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;
import java.time.LocalDate;

@Entity
@Table(name = "SAU_UNI1")
@IdClass(HiperdiaProfissionalId.class)
public class HiperdiaProfissional {

    @Id @Column(name = "UniCod", nullable = false) private Integer uniCod;
    @Id @Column(name = "UniProPesCod", nullable = false) private Long uniProPesCod;

    @Column(name = "UniProDatInc") private LocalDate datInclusao;
    @Column(name = "UniProMat", length = 20) @JdbcTypeCode(Types.CHAR) private String matricula;
    @Column(name = "UniProCBO", length = 8) private String cbo;
    @Column(name = "UniProSta") private Short status;
    @Column(name = "UniProDatDes") private LocalDate datDesativacao;

    public Integer getUniCod() { return uniCod; }
    public void setUniCod(Integer v) { this.uniCod = v; }
    public Long getUniProPesCod() { return uniProPesCod; }
    public void setUniProPesCod(Long v) { this.uniProPesCod = v; }
    public LocalDate getDatInclusao() { return datInclusao; }
    public void setDatInclusao(LocalDate v) { this.datInclusao = v; }
    public String getMatricula() { return matricula; }
    public void setMatricula(String v) { this.matricula = v; }
    public String getCbo() { return cbo; }
    public void setCbo(String v) { this.cbo = v; }
    public Short getStatus() { return status; }
    public void setStatus(Short v) { this.status = v; }
    public LocalDate getDatDesativacao() { return datDesativacao; }
    public void setDatDesativacao(LocalDate v) { this.datDesativacao = v; }
}
