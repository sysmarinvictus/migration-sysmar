package br.gov.mandaguari.saude.unidade.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "SAU_UNI3")
@IdClass(NutricionistaProfissionalId.class)
public class NutricionistaProfissional {

    @Id @Column(name = "UniCod", nullable = false) private Integer uniCod;
    @Id @Column(name = "UniNutProPesCod", nullable = false) private Long uniNutProPesCod;
    @Id @Column(name = "UniNutEspCod", nullable = false) private Integer uniNutEspCod;

    @Column(name = "UniNutDatInc") private LocalDate datInclusao;
    @Column(name = "UniNutSta") private Short status;
    @Column(name = "UniNutDatDes") private LocalDate datDesativacao;

    public Integer getUniCod() { return uniCod; }
    public void setUniCod(Integer v) { this.uniCod = v; }
    public Long getUniNutProPesCod() { return uniNutProPesCod; }
    public void setUniNutProPesCod(Long v) { this.uniNutProPesCod = v; }
    public Integer getUniNutEspCod() { return uniNutEspCod; }
    public void setUniNutEspCod(Integer v) { this.uniNutEspCod = v; }
    public LocalDate getDatInclusao() { return datInclusao; }
    public void setDatInclusao(LocalDate v) { this.datInclusao = v; }
    public Short getStatus() { return status; }
    public void setStatus(Short v) { this.status = v; }
    public LocalDate getDatDesativacao() { return datDesativacao; }
    public void setDatDesativacao(LocalDate v) { this.datDesativacao = v; }
}
