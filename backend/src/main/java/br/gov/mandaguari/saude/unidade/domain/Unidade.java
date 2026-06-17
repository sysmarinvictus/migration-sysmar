package br.gov.mandaguari.saude.unidade.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "SAU_UNI")
public class Unidade {

    @Id
    @Column(name = "UniCod", nullable = false)
    private Integer codigo;

    @Column(name = "UniForPesCod")
    private Long forPesCod;

    @Column(name = "UniNom", length = 50)
    private String nome;

    @Column(name = "UniRazSoc", length = 50)
    private String razaoSocial;

    @Column(name = "UniCnpj", length = 18)
    private String cnpj;

    @Column(name = "UniCep", length = 8)
    private String cep;

    @Column(name = "UniEnd", length = 70)
    private String endereco;

    @Column(name = "UniEndNum", length = 10)
    private String enderecoNumero;

    @Column(name = "UnEndCom", length = 40)
    private String enderecoComplemento;

    @Column(name = "UniBai", length = 70)
    private String bairro;

    @Column(name = "UniFon", length = 20)
    private String telefone;

    @Column(name = "UniFax", length = 20)
    private String fax;

    @Column(name = "UniLicFun", length = 10)
    private String licencaFuncionamento;

    @Column(name = "UniRes", length = 50)
    private String responsavel;

    @Column(name = "UniEMail", length = 70)
    private String email;

    @Column(name = "UniCnes")
    private Integer cnes;

    @Column(name = "UniBPA")
    private Short bpa;

    @Column(name = "UniSIPNI")
    private Short sipni;

    @Column(name = "UniOrgEmi", length = 10)
    private String orgaoEmissor;

    @Column(name = "UniEsfAdm")
    private Short estrategiaFamiliar;

    @Column(name = "UniPSF")
    private Short psf;

    @Column(name = "UniSisPreNatal")
    private Short sisPreNatal;

    @Column(name = "UniHiperdia")
    private Short hiperdia;

    @Column(name = "UniGes")
    private Short gestao;

    @Column(name = "UniSia", length = 7)
    private String sia;

    @Column(name = "UniSigla", length = 6)
    private String sigla;

    @Column(name = "UniSit")
    private Short situacao;

    @Column(name = "UniSIASUS", length = 7)
    private String siaSus;

    @Column(name = "UniScnesID", length = 20)
    private String scnesId;

    @Column(name = "UniExpEsus")
    private Boolean exportarEsus;

    @Column(name = "UniExpBNAFAR")
    private Boolean exportarBnafar;

    @Column(name = "UniCadCNS")
    private Boolean cadastroCns;

    @Column(name = "UniCadEnd")
    private Boolean cadastroEndereco;

    @Column(name = "UniAteSemCNS")
    private Boolean atendimentoSemCns;

    @Column(name = "UniAteSemEnd")
    private Boolean atendimentoSemEndereco;

    @Column(name = "UniEncFisio")
    private Boolean encaminhamentoFisioterapia;

    @Column(name = "UniExt")
    private Boolean externo;

    @Column(name = "TipUniCod")
    private Integer tipoUnidadeCodigo;

    @Column(name = "UniAtencaoSecundaria")
    private Boolean atencaoSecundaria;

    @Column(name = "UniBloqPacSemCadInd")
    private Boolean bloqueioPacSemCadInd;

    @Column(name = "UniAvisoVacinaAtrasada")
    private Boolean avisoVacinaAtrasada;

    @Column(name = "UniCadCPF")
    private Boolean cadastroCpf;

    @Column(name = "UniPainel")
    private Boolean painel;

    @Column(name = "UniRecInterMedMpp")
    private Boolean recepcaoIntermedMpp;

    @Column(name = "UniRecInterMedMppImp")
    private Boolean recepcaoIntermedMppImp;

    @Column(name = "UniBaiRemSemCns")
    private Boolean baixaRemSemCns;

    @Column(name = "UniBloqLancPcdAut")
    private Boolean bloqueioLancPcdAut;

    @Column(name = "UniBloqDispPacExt")
    private Boolean bloqueioDispPacExt;

    @Column(name = "UniBloqAgSolExaPacExt")
    private Boolean bloqueioAgendSolExaPacExt;

    @Column(name = "UniMunCod")
    private Integer municipioCodigo;

    @Column(name = "UniProPesRespCod")
    private Long respProfissionalCodigo;

    @Column(name = "UniProPesDirCod")
    private Long diretorCodigo;

    @Column(name = "UniProPesAudCod")
    private Long auditorCodigo;

    @Column(name = "UniProPesAutCod")
    private Long autorizadorCodigo;

    @Column(name = "UniDisCod")
    private Short distritoCodigo;

    public Integer getCodigo() { return codigo; }
    public void setCodigo(Integer codigo) { this.codigo = codigo; }
    public Long getForPesCod() { return forPesCod; }
    public void setForPesCod(Long forPesCod) { this.forPesCod = forPesCod; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public String getEnderecoNumero() { return enderecoNumero; }
    public void setEnderecoNumero(String enderecoNumero) { this.enderecoNumero = enderecoNumero; }
    public String getEnderecoComplemento() { return enderecoComplemento; }
    public void setEnderecoComplemento(String enderecoComplemento) { this.enderecoComplemento = enderecoComplemento; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getFax() { return fax; }
    public void setFax(String fax) { this.fax = fax; }
    public String getLicencaFuncionamento() { return licencaFuncionamento; }
    public void setLicencaFuncionamento(String licencaFuncionamento) { this.licencaFuncionamento = licencaFuncionamento; }
    public String getResponsavel() { return responsavel; }
    public void setResponsavel(String responsavel) { this.responsavel = responsavel; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Integer getCnes() { return cnes; }
    public void setCnes(Integer cnes) { this.cnes = cnes; }
    public Short getBpa() { return bpa; }
    public void setBpa(Short bpa) { this.bpa = bpa; }
    public Short getSipni() { return sipni; }
    public void setSipni(Short sipni) { this.sipni = sipni; }
    public String getOrgaoEmissor() { return orgaoEmissor; }
    public void setOrgaoEmissor(String orgaoEmissor) { this.orgaoEmissor = orgaoEmissor; }
    public Short getEstrategiaFamiliar() { return estrategiaFamiliar; }
    public void setEstrategiaFamiliar(Short estrategiaFamiliar) { this.estrategiaFamiliar = estrategiaFamiliar; }
    public Short getPsf() { return psf; }
    public void setPsf(Short psf) { this.psf = psf; }
    public Short getSisPreNatal() { return sisPreNatal; }
    public void setSisPreNatal(Short sisPreNatal) { this.sisPreNatal = sisPreNatal; }
    public Short getHiperdia() { return hiperdia; }
    public void setHiperdia(Short hiperdia) { this.hiperdia = hiperdia; }
    public Short getGestao() { return gestao; }
    public void setGestao(Short gestao) { this.gestao = gestao; }
    public String getSia() { return sia; }
    public void setSia(String sia) { this.sia = sia; }
    public String getSigla() { return sigla; }
    public void setSigla(String sigla) { this.sigla = sigla; }
    public Short getSituacao() { return situacao; }
    public void setSituacao(Short situacao) { this.situacao = situacao; }
    public String getSiaSus() { return siaSus; }
    public void setSiaSus(String siaSus) { this.siaSus = siaSus; }
    public String getScnesId() { return scnesId; }
    public void setScnesId(String scnesId) { this.scnesId = scnesId; }
    public Boolean getExportarEsus() { return exportarEsus; }
    public void setExportarEsus(Boolean exportarEsus) { this.exportarEsus = exportarEsus; }
    public Boolean getExportarBnafar() { return exportarBnafar; }
    public void setExportarBnafar(Boolean exportarBnafar) { this.exportarBnafar = exportarBnafar; }
    public Boolean getCadastroCns() { return cadastroCns; }
    public void setCadastroCns(Boolean cadastroCns) { this.cadastroCns = cadastroCns; }
    public Boolean getCadastroEndereco() { return cadastroEndereco; }
    public void setCadastroEndereco(Boolean cadastroEndereco) { this.cadastroEndereco = cadastroEndereco; }
    public Boolean getAtendimentoSemCns() { return atendimentoSemCns; }
    public void setAtendimentoSemCns(Boolean atendimentoSemCns) { this.atendimentoSemCns = atendimentoSemCns; }
    public Boolean getAtendimentoSemEndereco() { return atendimentoSemEndereco; }
    public void setAtendimentoSemEndereco(Boolean atendimentoSemEndereco) { this.atendimentoSemEndereco = atendimentoSemEndereco; }
    public Boolean getEncaminhamentoFisioterapia() { return encaminhamentoFisioterapia; }
    public void setEncaminhamentoFisioterapia(Boolean encaminhamentoFisioterapia) { this.encaminhamentoFisioterapia = encaminhamentoFisioterapia; }
    public Boolean getExterno() { return externo; }
    public void setExterno(Boolean externo) { this.externo = externo; }
    public Integer getTipoUnidadeCodigo() { return tipoUnidadeCodigo; }
    public void setTipoUnidadeCodigo(Integer tipoUnidadeCodigo) { this.tipoUnidadeCodigo = tipoUnidadeCodigo; }
    public Boolean getAtencaoSecundaria() { return atencaoSecundaria; }
    public void setAtencaoSecundaria(Boolean atencaoSecundaria) { this.atencaoSecundaria = atencaoSecundaria; }
    public Boolean getBloqueioPacSemCadInd() { return bloqueioPacSemCadInd; }
    public void setBloqueioPacSemCadInd(Boolean bloqueioPacSemCadInd) { this.bloqueioPacSemCadInd = bloqueioPacSemCadInd; }
    public Boolean getAvisoVacinaAtrasada() { return avisoVacinaAtrasada; }
    public void setAvisoVacinaAtrasada(Boolean avisoVacinaAtrasada) { this.avisoVacinaAtrasada = avisoVacinaAtrasada; }
    public Boolean getCadastroCpf() { return cadastroCpf; }
    public void setCadastroCpf(Boolean cadastroCpf) { this.cadastroCpf = cadastroCpf; }
    public Boolean getPainel() { return painel; }
    public void setPainel(Boolean painel) { this.painel = painel; }
    public Boolean getRecepcaoIntermedMpp() { return recepcaoIntermedMpp; }
    public void setRecepcaoIntermedMpp(Boolean recepcaoIntermedMpp) { this.recepcaoIntermedMpp = recepcaoIntermedMpp; }
    public Boolean getRecepcaoIntermedMppImp() { return recepcaoIntermedMppImp; }
    public void setRecepcaoIntermedMppImp(Boolean recepcaoIntermedMppImp) { this.recepcaoIntermedMppImp = recepcaoIntermedMppImp; }
    public Boolean getBaixaRemSemCns() { return baixaRemSemCns; }
    public void setBaixaRemSemCns(Boolean baixaRemSemCns) { this.baixaRemSemCns = baixaRemSemCns; }
    public Boolean getBloqueioLancPcdAut() { return bloqueioLancPcdAut; }
    public void setBloqueioLancPcdAut(Boolean bloqueioLancPcdAut) { this.bloqueioLancPcdAut = bloqueioLancPcdAut; }
    public Boolean getBloqueioDispPacExt() { return bloqueioDispPacExt; }
    public void setBloqueioDispPacExt(Boolean bloqueioDispPacExt) { this.bloqueioDispPacExt = bloqueioDispPacExt; }
    public Boolean getBloqueioAgendSolExaPacExt() { return bloqueioAgendSolExaPacExt; }
    public void setBloqueioAgendSolExaPacExt(Boolean bloqueioAgendSolExaPacExt) { this.bloqueioAgendSolExaPacExt = bloqueioAgendSolExaPacExt; }
    public Integer getMunicipioCodigo() { return municipioCodigo; }
    public void setMunicipioCodigo(Integer municipioCodigo) { this.municipioCodigo = municipioCodigo; }
    public Long getRespProfissionalCodigo() { return respProfissionalCodigo; }
    public void setRespProfissionalCodigo(Long respProfissionalCodigo) { this.respProfissionalCodigo = respProfissionalCodigo; }
    public Long getDiretorCodigo() { return diretorCodigo; }
    public void setDiretorCodigo(Long diretorCodigo) { this.diretorCodigo = diretorCodigo; }
    public Long getAuditorCodigo() { return auditorCodigo; }
    public void setAuditorCodigo(Long auditorCodigo) { this.auditorCodigo = auditorCodigo; }
    public Long getAutorizadorCodigo() { return autorizadorCodigo; }
    public void setAutorizadorCodigo(Long autorizadorCodigo) { this.autorizadorCodigo = autorizadorCodigo; }
    public Short getDistritoCodigo() { return distritoCodigo; }
    public void setDistritoCodigo(Short distritoCodigo) { this.distritoCodigo = distritoCodigo; }
}
