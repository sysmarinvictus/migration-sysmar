package br.gov.mandaguari.saude.medicamento.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;

/**
 * Medicamento — maps the existing GeneXus table {@code SAU_REM} (28 columns confirmed against the
 * live {@code saude-mandaguari} schema). Physical GeneXus names are pinned in {@code @Column};
 * clean names appear only in DTOs/UI. Rules mined from {@code sau_rem_impl.java}.
 */
@Entity
@Table(name = "SAU_REM")
public class Medicamento {

    @Id
    @Column(name = "RemCod", nullable = false)
    private Integer id;

    @Column(name = "RemNom", length = 250)
    private String nome;

    @Column(name = "TipRemCod")
    private Integer tipoMedicamentoCodigo;

    @Column(name = "DcbCod", length = 10)
    @JdbcTypeCode(Types.CHAR)
    private String dcbCodigo;

    @Column(name = "RENAMECod", length = 20)
    private String renameCodigo;

    @Column(name = "RenameAtualCod", length = 20)
    private String renameAtualCodigo;

    @Column(name = "AprRemCod")
    private Integer apresentacaoCodigo;

    @Column(name = "ObmCod", length = 30)
    private String obmCodigo;

    @Column(name = "RemTipoProduto")
    private Short tipoProduto;

    @Column(name = "RemCon", length = 150)
    private String concentracao;

    @Column(name = "RemFarBas")
    private Short farmaciaBasica;

    @Column(name = "RemPsico")
    private Short psicotropico;

    @Column(name = "RemConEsp")
    private Short controleEspecial;

    @Column(name = "RemEti")
    private Short etico;

    @Column(name = "RemVlrHos", precision = 11, scale = 4)
    private BigDecimal valorHospitalar;

    @Column(name = "RemVlrUni", precision = 11, scale = 4)
    private BigDecimal valorUnitario;

    @Column(name = "RemSemRename")
    private Boolean semRename;

    @Column(name = "RemPortariaPsicotropico", length = 20)
    private String portariaPsicotropico;

    @Column(name = "RemSit")
    private Short situacao;

    @Column(name = "RemOmitirSaldo")
    private Boolean omitirSaldo;

    @Column(name = "RemUsarPosologia")
    private Boolean usarPosologia;

    @Column(name = "RemMPP")
    private Boolean medicamentoPotencialmentePerigoso;

    @Column(name = "RemMPPDes", length = 1000)
    private String mppEfeitos;

    @Column(name = "RemMPPCanMotivo", length = 300)
    private String mppCancelamentoMotivo;

    @Column(name = "RemMPPCanData")
    private LocalDateTime mppCancelamentoData;

    @Column(name = "RemMppCanUsuLogin", length = 20) // careful: lowercase 'pp' in the physical name
    @JdbcTypeCode(Types.CHAR)
    private String mppCancelamentoLogin;

    @Column(name = "RemUniSetorSeqUlt")
    private Integer ultimaSequenciaUnidadeSetor;

    @Column(name = "RemUsuLogin", length = 20)
    @JdbcTypeCode(Types.CHAR)
    private String usuarioLogin;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Integer getTipoMedicamentoCodigo() { return tipoMedicamentoCodigo; }
    public void setTipoMedicamentoCodigo(Integer v) { this.tipoMedicamentoCodigo = v; }
    public String getDcbCodigo() { return dcbCodigo; }
    public void setDcbCodigo(String v) { this.dcbCodigo = v; }
    public String getRenameCodigo() { return renameCodigo; }
    public void setRenameCodigo(String v) { this.renameCodigo = v; }
    public String getRenameAtualCodigo() { return renameAtualCodigo; }
    public void setRenameAtualCodigo(String v) { this.renameAtualCodigo = v; }
    public Integer getApresentacaoCodigo() { return apresentacaoCodigo; }
    public void setApresentacaoCodigo(Integer v) { this.apresentacaoCodigo = v; }
    public String getObmCodigo() { return obmCodigo; }
    public void setObmCodigo(String v) { this.obmCodigo = v; }
    public Short getTipoProduto() { return tipoProduto; }
    public void setTipoProduto(Short v) { this.tipoProduto = v; }
    public String getConcentracao() { return concentracao; }
    public void setConcentracao(String v) { this.concentracao = v; }
    public Short getFarmaciaBasica() { return farmaciaBasica; }
    public void setFarmaciaBasica(Short v) { this.farmaciaBasica = v; }
    public Short getPsicotropico() { return psicotropico; }
    public void setPsicotropico(Short v) { this.psicotropico = v; }
    public Short getControleEspecial() { return controleEspecial; }
    public void setControleEspecial(Short v) { this.controleEspecial = v; }
    public Short getEtico() { return etico; }
    public void setEtico(Short v) { this.etico = v; }
    public BigDecimal getValorHospitalar() { return valorHospitalar; }
    public void setValorHospitalar(BigDecimal v) { this.valorHospitalar = v; }
    public BigDecimal getValorUnitario() { return valorUnitario; }
    public void setValorUnitario(BigDecimal v) { this.valorUnitario = v; }
    public Boolean getSemRename() { return semRename; }
    public void setSemRename(Boolean v) { this.semRename = v; }
    public String getPortariaPsicotropico() { return portariaPsicotropico; }
    public void setPortariaPsicotropico(String v) { this.portariaPsicotropico = v; }
    public Short getSituacao() { return situacao; }
    public void setSituacao(Short v) { this.situacao = v; }
    public Boolean getOmitirSaldo() { return omitirSaldo; }
    public void setOmitirSaldo(Boolean v) { this.omitirSaldo = v; }
    public Boolean getUsarPosologia() { return usarPosologia; }
    public void setUsarPosologia(Boolean v) { this.usarPosologia = v; }
    public Boolean getMedicamentoPotencialmentePerigoso() { return medicamentoPotencialmentePerigoso; }
    public void setMedicamentoPotencialmentePerigoso(Boolean v) { this.medicamentoPotencialmentePerigoso = v; }
    public String getMppEfeitos() { return mppEfeitos; }
    public void setMppEfeitos(String v) { this.mppEfeitos = v; }
    public String getMppCancelamentoMotivo() { return mppCancelamentoMotivo; }
    public void setMppCancelamentoMotivo(String v) { this.mppCancelamentoMotivo = v; }
    public LocalDateTime getMppCancelamentoData() { return mppCancelamentoData; }
    public void setMppCancelamentoData(LocalDateTime v) { this.mppCancelamentoData = v; }
    public String getMppCancelamentoLogin() { return mppCancelamentoLogin; }
    public void setMppCancelamentoLogin(String v) { this.mppCancelamentoLogin = v; }
    public Integer getUltimaSequenciaUnidadeSetor() { return ultimaSequenciaUnidadeSetor; }
    public void setUltimaSequenciaUnidadeSetor(Integer v) { this.ultimaSequenciaUnidadeSetor = v; }
    public String getUsuarioLogin() { return usuarioLogin; }
    public void setUsuarioLogin(String v) { this.usuarioLogin = v; }
}
