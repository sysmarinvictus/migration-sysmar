package br.gov.mandaguari.saude.formaapresentacao.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;

/**
 * Forma de Apresentação do Medicamento — maps the existing GeneXus table {@code SAU_APRREM}
 * (referenced by {@code SAU_REM.AprRemCod}). Rules mined from {@code sau_aprrem_impl.java}.
 */
@Entity
@Table(name = "SAU_APRREM")
public class FormaApresentacao {

    @Id
    @Column(name = "AprRemCod", nullable = false)
    private Integer id;

    @Column(name = "AprRemDes", length = 30)
    private String descricao;

    @Column(name = "AprRemAbr", length = 5)
    @JdbcTypeCode(Types.CHAR)   // physical CHAR(5)
    private String abreviacao;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getAbreviacao() { return abreviacao; }
    public void setAbreviacao(String abreviacao) { this.abreviacao = abreviacao; }
}
