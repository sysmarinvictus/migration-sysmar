package br.gov.mandaguari.saude.distrito.domain;

import jakarta.persistence.*;

/** Represents a row in SAU_DIS (Distrito Sanitário). */
@Entity
@Table(name = "SAU_DIS")
public class Distrito {

    @Id
    @Column(name = "DisCod", nullable = false)
    private Short codigo;

    @Column(name = "DisNom", length = 30)
    private String nome;

    @Column(name = "DisEnd", length = 50)
    private String endereco;

    @Column(name = "DisNum")
    private Short numero;

    @Column(name = "DisCom", length = 15)
    private String complemento;

    @Column(name = "DisCEP")
    private Integer cep;

    @Column(name = "DisDDD", length = 3)
    private String ddd;

    @Column(name = "DisFon")
    private Integer telefone;

    @Column(name = "DisFax")
    private Integer fax;

    @Column(name = "DisTipLogCod")
    private Integer tipoLogradouroCodigo;

    @Column(name = "DisBaiCod")
    private Integer bairroCodigo;

    public Short getCodigo() { return codigo; }
    public void setCodigo(Short codigo) { this.codigo = codigo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public Short getNumero() { return numero; }
    public void setNumero(Short numero) { this.numero = numero; }
    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }
    public Integer getCep() { return cep; }
    public void setCep(Integer cep) { this.cep = cep; }
    public String getDdd() { return ddd; }
    public void setDdd(String ddd) { this.ddd = ddd; }
    public Integer getTelefone() { return telefone; }
    public void setTelefone(Integer telefone) { this.telefone = telefone; }
    public Integer getFax() { return fax; }
    public void setFax(Integer fax) { this.fax = fax; }
    public Integer getTipoLogradouroCodigo() { return tipoLogradouroCodigo; }
    public void setTipoLogradouroCodigo(Integer tipoLogradouroCodigo) { this.tipoLogradouroCodigo = tipoLogradouroCodigo; }
    public Integer getBairroCodigo() { return bairroCodigo; }
    public void setBairroCodigo(Integer bairroCodigo) { this.bairroCodigo = bairroCodigo; }
}
