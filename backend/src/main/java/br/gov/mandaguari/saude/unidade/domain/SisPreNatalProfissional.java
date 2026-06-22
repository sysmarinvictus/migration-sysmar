package br.gov.mandaguari.saude.unidade.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "SAU_UNI2")
@IdClass(SisPreNatalProfissionalId.class)
public class SisPreNatalProfissional {

    @Id @Column(name = "UniCod", nullable = false) private Integer uniCod;
    @Id @Column(name = "UniGesProPesCod", nullable = false) private Long uniGesProPesCod;
    @Id @Column(name = "UniGesEspCod", nullable = false) private Integer uniGesEspCod;

    @Column(name = "UniGesDatInc") private LocalDate datInclusao;
    @Column(name = "UniGesSta") private Short status;
    @Column(name = "UniGesDatDes") private LocalDate datDesativacao;

    public Integer getUniCod() { return uniCod; }
    public void setUniCod(Integer v) { this.uniCod = v; }
    public Long getUniGesProPesCod() { return uniGesProPesCod; }
    public void setUniGesProPesCod(Long v) { this.uniGesProPesCod = v; }
    public Integer getUniGesEspCod() { return uniGesEspCod; }
    public void setUniGesEspCod(Integer v) { this.uniGesEspCod = v; }
    public LocalDate getDatInclusao() { return datInclusao; }
    public void setDatInclusao(LocalDate v) { this.datInclusao = v; }
    public Short getStatus() { return status; }
    public void setStatus(Short v) { this.status = v; }
    public LocalDate getDatDesativacao() { return datDesativacao; }
    public void setDatDesativacao(LocalDate v) { this.datDesativacao = v; }
}
