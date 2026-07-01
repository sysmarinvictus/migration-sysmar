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
 * Pessoa — the person SUPERTYPE (GeneXus {@code SYS_PES}, 86 live cols, 83k rows). Originally an additive
 * READ model; the SAU_PESF slice now WRITE-enables it (create/update/delete the person cadastro — the
 * transaction that feeds the SAU_PRO/SAU_FUN/SAU_PAC subtypes). Physical column names pinned; CHAR(n)
 * columns are blank-padded in PostgreSQL, so their getters trim. {@code ddl-auto=validate} only checks
 * mapped columns.
 *
 * <p><b>QUARANTINED — NOT mapped:</b> {@code PesSenha}/{@code PesSenhaKey} (reversible person credential,
 * security sign-off pending) and {@code PesSasFlag}/{@code PesSauFlag} (integration flags). The legacy
 * INSERT writes {@code ''}/{@code 0} to these; the modern write path leaves them NULL (both nullable).
 * The establishment/PJ block is also unmapped (person cadastro only — SAU_PESF OQ / deferred).
 *
 * <p><b>PHI:</b> nearly every mapped field is PHI. {@link #toString()} exposes only the id; PHI is never
 * logged. Social-name display (R41) honours {@code PesUsaNomSoc}.
 */
@Entity
@Table(name = "SYS_PES")
public class Pessoa {

    @Id
    @Column(name = "PesCod", nullable = false)
    private Long id;

    /** R48: 1/2 = pessoa física; defaulted to 2 on insert by the service. */
    @Column(name = "PesTip")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer tipoPessoa;

    // --- names / filiation (PHI) ---
    @Column(name = "PesNom", length = 50)
    private String nome;

    @Column(name = "PesNomSoc", length = 50)
    private String nomeSocial;

    @Column(name = "PesUsaNomSoc")
    private Boolean usaNomeSocial;

    @Column(name = "PesNomPai", length = 50)
    private String nomePai;

    @Column(name = "PesNomMae", length = 50)
    private String nomeMae;

    @Column(name = "PesNomCon", length = 50)
    private String nomeConjuge;

    // --- soundex (derived on write; never user-editable) ---
    @Column(name = "PesNomSoundex", length = 50)
    private String nomeSoundex;

    @Column(name = "PesNomMaeSoundex", length = 50)
    private String nomeMaeSoundex;

    @Column(name = "PesNomSocSoundex", length = 50)
    private String nomeSocialSoundex;

    // --- documents (PHI) ---
    @Column(name = "PesCPFCNPJ", length = 18)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String cpfCnpj;

    @Column(name = "PesNumCns", length = 20)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String cns;

    @Column(name = "PesRGIE", length = 15)
    private String rgIe;

    @Column(name = "PesOrgEmiCod")
    private Integer orgaoEmissorCod;

    @Column(name = "PesIdeEst", length = 2)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String rgUf;

    @Column(name = "PesIdeDat")
    private LocalDate rgDataEmissao;

    // --- demographics (PHI) ---
    @Column(name = "PesNasDat")
    private LocalDate dataNascimento;

    @Column(name = "PesSex", length = 1)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String sexo;

    @Column(name = "PesCor")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer corCod;

    @Column(name = "PesEstCiv")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer estadoCivilCod;

    @Column(name = "PesSitFam")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer situacaoFamiliarCod;

    @Column(name = "PesEtnCod")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer etniaCod;

    @Column(name = "PesTipSan", length = 3)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String tipoSanguineo;

    // --- nationality (PHI) ---
    @Column(name = "PesNacTip")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer nacionalidadeTipo;

    @Column(name = "PesPaisCod")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer paisCod;

    @Column(name = "PesNacMunCod")
    private Integer municipioNascCod;

    @Column(name = "PesNacDatNat")
    private LocalDate dataNaturalizacao;

    @Column(name = "PesNacNumPor", length = 16)
    private String numeroPortaria;

    @Column(name = "PesNacDatEnt")
    private LocalDate dataEntradaPais;

    // --- address (PHI) ---
    @Column(name = "PesCEP", length = 8)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String cep;

    @Column(name = "PesTipLogCod")
    private Integer tipoLogradouroCod;

    @Column(name = "PesEnd", length = 70)
    private String endereco;

    @Column(name = "PesEndNum", length = 10)
    private String enderecoNumero;

    @Column(name = "PesEndCom", length = 40)
    private String enderecoComplemento;

    @Column(name = "PesBaiCod")
    private Integer bairroCod;

    @Column(name = "PesMunCod")
    private Integer municipioCod;

    // --- contact (PHI) ---
    @Column(name = "PesFon", length = 20)
    private String telefone;

    @Column(name = "PesCel", length = 20)
    private String celular;

    @Column(name = "PesFax", length = 20)
    private String fax;

    @Column(name = "PesEmail", length = 70)
    private String email;

    @Column(name = "PesHomePage", length = 70)
    private String homePage;

    // --- certidão civil (PHI) ---
    @Column(name = "PesCerCiv")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer certidaoCivilTipo;

    @Column(name = "PesCerNum", length = 8)
    private String certidaoNumero;

    @Column(name = "PesCerLiv", length = 8)
    private String certidaoLivro;

    @Column(name = "PesCerFol", length = 4)
    private String certidaoFolha;

    @Column(name = "PesCerDat")
    private LocalDate certidaoData;

    @Column(name = "PesCerCar", length = 60)
    private String certidaoCartorio;

    // --- certidão novo modelo ---
    @Column(name = "PesNovCerSer", length = 6)
    private String novaCertServentia;

    @Column(name = "PesNovCerAce", length = 2)
    private String novaCertAcervo;

    @Column(name = "PesNovCerRegCiv", length = 2)
    private String novaCertRegistroCivil;

    @Column(name = "PesNovCerAno", length = 4)
    private String novaCertAno;

    @Column(name = "PesNovCerTipLiv", length = 1)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String novaCertTipoLivro;

    @Column(name = "PesNovCerLiv", length = 5)
    private String novaCertLivro;

    @Column(name = "PesNovCerFol", length = 3)
    private String novaCertFolha;

    @Column(name = "PesNovCerTer", length = 7)
    private String novaCertTermo;

    @Column(name = "PesNovCerDv", length = 2)
    private String novaCertDv;

    // --- CTPS / título eleitor / NIS (PHI) ---
    @Column(name = "PesTraSer", length = 5)
    private String ctpsSerie;

    @Column(name = "PesTraNum", length = 7)
    private String ctpsNumero;

    @Column(name = "PesTraEst", length = 2)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String ctpsUf;

    @Column(name = "PesTraDat")
    private LocalDate ctpsData;

    @Column(name = "PesTraPis", length = 11)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String pisPasep;

    @Column(name = "PesEleNum", length = 13)
    private String tituloEleitorNumero;

    @Column(name = "PesEleZon", length = 4)
    private String tituloEleitorZona;

    @Column(name = "PesEleSec", length = 4)
    private String tituloEleitorSecao;

    @Column(name = "PesNumSoc", length = 11)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String nis;

    // --- education / occupation ---
    @Column(name = "PesFreEsc", length = 1)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String frequentaEscola;

    @Column(name = "PesGraEsc")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer grauEscolaridade;

    @Column(name = "PesEscolaridade")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer escolaridade;

    @Column(name = "PesCborCod", length = 6)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String cboCod;

    // --- misc ---
    @Column(name = "PesCadDat")
    private LocalDate dataCadastro;

    @Column(name = "PesObs", length = 300)
    private String observacao;

    @Column(name = "PesGerBpa")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer gerarBpa;

    public Pessoa() {}

    /** R41: the name to DISPLAY — social name when the person opted in, else the registry name. */
    @JsonIgnore
    public String getNomeExibicao() {
        return Boolean.TRUE.equals(usaNomeSocial) && nomeSocial != null && !nomeSocial.isBlank()
                ? nomeSocial : nome;
    }

    /** Composite — "Social (Registro)" when social name is used, else the registry name alone. */
    @JsonIgnore
    public String getNomeCompleto() {
        return Boolean.TRUE.equals(usaNomeSocial) && nomeSocial != null && !nomeSocial.isBlank()
                ? nomeSocial + " (" + nome + ")" : nome;
    }

    private static String trim(String s) { return s == null ? null : s.trim(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getTipoPessoa() { return tipoPessoa; }
    public void setTipoPessoa(Integer tipoPessoa) { this.tipoPessoa = tipoPessoa; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getNomeSocial() { return nomeSocial; }
    public void setNomeSocial(String nomeSocial) { this.nomeSocial = nomeSocial; }
    public Boolean getUsaNomeSocial() { return usaNomeSocial; }
    public void setUsaNomeSocial(Boolean usaNomeSocial) { this.usaNomeSocial = usaNomeSocial; }
    public String getNomePai() { return nomePai; }
    public void setNomePai(String nomePai) { this.nomePai = nomePai; }
    public String getNomeMae() { return nomeMae; }
    public void setNomeMae(String nomeMae) { this.nomeMae = nomeMae; }
    public String getNomeConjuge() { return nomeConjuge; }
    public void setNomeConjuge(String nomeConjuge) { this.nomeConjuge = nomeConjuge; }
    public String getNomeSoundex() { return nomeSoundex; }
    public void setNomeSoundex(String nomeSoundex) { this.nomeSoundex = nomeSoundex; }
    public String getNomeMaeSoundex() { return nomeMaeSoundex; }
    public void setNomeMaeSoundex(String nomeMaeSoundex) { this.nomeMaeSoundex = nomeMaeSoundex; }
    public String getNomeSocialSoundex() { return nomeSocialSoundex; }
    public void setNomeSocialSoundex(String nomeSocialSoundex) { this.nomeSocialSoundex = nomeSocialSoundex; }
    public String getCpfCnpj() { return trim(cpfCnpj); }
    public void setCpfCnpj(String cpfCnpj) { this.cpfCnpj = cpfCnpj; }
    public String getCns() { return trim(cns); }
    public void setCns(String cns) { this.cns = cns; }
    public String getRgIe() { return rgIe; }
    public void setRgIe(String rgIe) { this.rgIe = rgIe; }
    public Integer getOrgaoEmissorCod() { return orgaoEmissorCod; }
    public void setOrgaoEmissorCod(Integer orgaoEmissorCod) { this.orgaoEmissorCod = orgaoEmissorCod; }
    public String getRgUf() { return trim(rgUf); }
    public void setRgUf(String rgUf) { this.rgUf = rgUf; }
    public LocalDate getRgDataEmissao() { return rgDataEmissao; }
    public void setRgDataEmissao(LocalDate rgDataEmissao) { this.rgDataEmissao = rgDataEmissao; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public String getSexo() { return trim(sexo); }
    public void setSexo(String sexo) { this.sexo = sexo; }
    public Integer getCorCod() { return corCod; }
    public void setCorCod(Integer corCod) { this.corCod = corCod; }
    public Integer getEstadoCivilCod() { return estadoCivilCod; }
    public void setEstadoCivilCod(Integer estadoCivilCod) { this.estadoCivilCod = estadoCivilCod; }
    public Integer getSituacaoFamiliarCod() { return situacaoFamiliarCod; }
    public void setSituacaoFamiliarCod(Integer situacaoFamiliarCod) { this.situacaoFamiliarCod = situacaoFamiliarCod; }
    public Integer getEtniaCod() { return etniaCod; }
    public void setEtniaCod(Integer etniaCod) { this.etniaCod = etniaCod; }
    public String getTipoSanguineo() { return trim(tipoSanguineo); }
    public void setTipoSanguineo(String tipoSanguineo) { this.tipoSanguineo = tipoSanguineo; }
    public Integer getNacionalidadeTipo() { return nacionalidadeTipo; }
    public void setNacionalidadeTipo(Integer nacionalidadeTipo) { this.nacionalidadeTipo = nacionalidadeTipo; }
    public Integer getPaisCod() { return paisCod; }
    public void setPaisCod(Integer paisCod) { this.paisCod = paisCod; }
    public Integer getMunicipioNascCod() { return municipioNascCod; }
    public void setMunicipioNascCod(Integer municipioNascCod) { this.municipioNascCod = municipioNascCod; }
    public LocalDate getDataNaturalizacao() { return dataNaturalizacao; }
    public void setDataNaturalizacao(LocalDate dataNaturalizacao) { this.dataNaturalizacao = dataNaturalizacao; }
    public String getNumeroPortaria() { return numeroPortaria; }
    public void setNumeroPortaria(String numeroPortaria) { this.numeroPortaria = numeroPortaria; }
    public LocalDate getDataEntradaPais() { return dataEntradaPais; }
    public void setDataEntradaPais(LocalDate dataEntradaPais) { this.dataEntradaPais = dataEntradaPais; }
    public String getCep() { return trim(cep); }
    public void setCep(String cep) { this.cep = cep; }
    public Integer getTipoLogradouroCod() { return tipoLogradouroCod; }
    public void setTipoLogradouroCod(Integer tipoLogradouroCod) { this.tipoLogradouroCod = tipoLogradouroCod; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public String getEnderecoNumero() { return enderecoNumero; }
    public void setEnderecoNumero(String enderecoNumero) { this.enderecoNumero = enderecoNumero; }
    public String getEnderecoComplemento() { return enderecoComplemento; }
    public void setEnderecoComplemento(String enderecoComplemento) { this.enderecoComplemento = enderecoComplemento; }
    public Integer getBairroCod() { return bairroCod; }
    public void setBairroCod(Integer bairroCod) { this.bairroCod = bairroCod; }
    public Integer getMunicipioCod() { return municipioCod; }
    public void setMunicipioCod(Integer municipioCod) { this.municipioCod = municipioCod; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getCelular() { return celular; }
    public void setCelular(String celular) { this.celular = celular; }
    public String getFax() { return fax; }
    public void setFax(String fax) { this.fax = fax; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getHomePage() { return homePage; }
    public void setHomePage(String homePage) { this.homePage = homePage; }
    public Integer getCertidaoCivilTipo() { return certidaoCivilTipo; }
    public void setCertidaoCivilTipo(Integer certidaoCivilTipo) { this.certidaoCivilTipo = certidaoCivilTipo; }
    public String getCertidaoNumero() { return certidaoNumero; }
    public void setCertidaoNumero(String certidaoNumero) { this.certidaoNumero = certidaoNumero; }
    public String getCertidaoLivro() { return certidaoLivro; }
    public void setCertidaoLivro(String certidaoLivro) { this.certidaoLivro = certidaoLivro; }
    public String getCertidaoFolha() { return certidaoFolha; }
    public void setCertidaoFolha(String certidaoFolha) { this.certidaoFolha = certidaoFolha; }
    public LocalDate getCertidaoData() { return certidaoData; }
    public void setCertidaoData(LocalDate certidaoData) { this.certidaoData = certidaoData; }
    public String getCertidaoCartorio() { return certidaoCartorio; }
    public void setCertidaoCartorio(String certidaoCartorio) { this.certidaoCartorio = certidaoCartorio; }
    public String getNovaCertServentia() { return novaCertServentia; }
    public void setNovaCertServentia(String novaCertServentia) { this.novaCertServentia = novaCertServentia; }
    public String getNovaCertAcervo() { return novaCertAcervo; }
    public void setNovaCertAcervo(String novaCertAcervo) { this.novaCertAcervo = novaCertAcervo; }
    public String getNovaCertRegistroCivil() { return novaCertRegistroCivil; }
    public void setNovaCertRegistroCivil(String novaCertRegistroCivil) { this.novaCertRegistroCivil = novaCertRegistroCivil; }
    public String getNovaCertAno() { return novaCertAno; }
    public void setNovaCertAno(String novaCertAno) { this.novaCertAno = novaCertAno; }
    public String getNovaCertTipoLivro() { return trim(novaCertTipoLivro); }
    public void setNovaCertTipoLivro(String novaCertTipoLivro) { this.novaCertTipoLivro = novaCertTipoLivro; }
    public String getNovaCertLivro() { return novaCertLivro; }
    public void setNovaCertLivro(String novaCertLivro) { this.novaCertLivro = novaCertLivro; }
    public String getNovaCertFolha() { return novaCertFolha; }
    public void setNovaCertFolha(String novaCertFolha) { this.novaCertFolha = novaCertFolha; }
    public String getNovaCertTermo() { return novaCertTermo; }
    public void setNovaCertTermo(String novaCertTermo) { this.novaCertTermo = novaCertTermo; }
    public String getNovaCertDv() { return novaCertDv; }
    public void setNovaCertDv(String novaCertDv) { this.novaCertDv = novaCertDv; }
    public String getCtpsSerie() { return ctpsSerie; }
    public void setCtpsSerie(String ctpsSerie) { this.ctpsSerie = ctpsSerie; }
    public String getCtpsNumero() { return ctpsNumero; }
    public void setCtpsNumero(String ctpsNumero) { this.ctpsNumero = ctpsNumero; }
    public String getCtpsUf() { return trim(ctpsUf); }
    public void setCtpsUf(String ctpsUf) { this.ctpsUf = ctpsUf; }
    public LocalDate getCtpsData() { return ctpsData; }
    public void setCtpsData(LocalDate ctpsData) { this.ctpsData = ctpsData; }
    public String getPisPasep() { return trim(pisPasep); }
    public void setPisPasep(String pisPasep) { this.pisPasep = pisPasep; }
    public String getTituloEleitorNumero() { return tituloEleitorNumero; }
    public void setTituloEleitorNumero(String tituloEleitorNumero) { this.tituloEleitorNumero = tituloEleitorNumero; }
    public String getTituloEleitorZona() { return tituloEleitorZona; }
    public void setTituloEleitorZona(String tituloEleitorZona) { this.tituloEleitorZona = tituloEleitorZona; }
    public String getTituloEleitorSecao() { return tituloEleitorSecao; }
    public void setTituloEleitorSecao(String tituloEleitorSecao) { this.tituloEleitorSecao = tituloEleitorSecao; }
    public String getNis() { return trim(nis); }
    public void setNis(String nis) { this.nis = nis; }
    public String getFrequentaEscola() { return trim(frequentaEscola); }
    public void setFrequentaEscola(String frequentaEscola) { this.frequentaEscola = frequentaEscola; }
    public Integer getGrauEscolaridade() { return grauEscolaridade; }
    public void setGrauEscolaridade(Integer grauEscolaridade) { this.grauEscolaridade = grauEscolaridade; }
    public Integer getEscolaridade() { return escolaridade; }
    public void setEscolaridade(Integer escolaridade) { this.escolaridade = escolaridade; }
    public String getCboCod() { return trim(cboCod); }
    public void setCboCod(String cboCod) { this.cboCod = cboCod; }
    public LocalDate getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(LocalDate dataCadastro) { this.dataCadastro = dataCadastro; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public Integer getGerarBpa() { return gerarBpa; }
    public void setGerarBpa(Integer gerarBpa) { this.gerarBpa = gerarBpa; }

    /** Only the id — PHI is never logged (R-LGPD). */
    @Override
    public String toString() { return "Pessoa{id=" + id + "}"; }
}
