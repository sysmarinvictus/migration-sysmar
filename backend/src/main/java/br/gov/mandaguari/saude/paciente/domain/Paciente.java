package br.gov.mandaguari.saude.paciente.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Paciente — maps GeneXus {@code SAU_PAC} (80k rows), a SYS_PES person-SUBTYPE: PK {@code PacPesCod} =
 * {@code SYS_PES.PesCod}. This entity maps ONLY the 26 SAU_PAC-owned columns (patient clinical/social/
 * audit data); the person fields (nome, CPF, CNS, address, mãe, nascimento…) live in SYS_PES and are
 * handled via the {@code Pessoa} entity on the write-back path (SAU_PRO precedent). {@code PacPesCod} is a
 * raw {@code Long} PK (no {@code @MapsId} cascade).
 *
 * <p><b>PHI (most sensitive slice):</b> allergy, disease history, death flag, mental-health/vulnerability
 * flags (psychiatric crisis, homeless, unconscious). {@link #toString()} emits only the id; PHI never logged.
 * Types are LIVE-verified: smallint→Integer+@JdbcTypeCode(SMALLINT), native boolean→Boolean, char→trimmed.
 */
@Entity
@Table(name = "SAU_PAC")
public class Paciente {

    @Id
    @Column(name = "PacPesCod", nullable = false)
    private Long id;                       // = SYS_PES.PesCod

    @Column(name = "PacUniCod")
    private Integer unidadeCod;

    @Column(name = "PacProNum", length = 10)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String prontuario;

    @Column(name = "PacIdNum")
    private Long numeroIdentificacao;

    // ── clinical (PHI) ──
    @Column(name = "PacAler", length = 50)
    private String alergia;

    @Column(name = "PacCHistDoe", length = 200)
    private String historicoDoencas;

    @Column(name = "PacObi")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer obito;

    @Column(name = "PacInconsciente")
    private Boolean inconsciente;

    @Column(name = "PacSituacaoRua")
    private Boolean situacaoRua;

    @Column(name = "PacSurtoPsiquiatrico")
    private Boolean surtoPsiquiatrico;

    // ── social ──
    @Column(name = "PacRendaFamiliar")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer rendaFamiliar;

    @Column(name = "PacMeioTransporte", length = 50)
    private String meioTransporte;

    @Column(name = "PacBeneficioSocial")
    private Boolean beneficioSocial;

    @Column(name = "PacCNH", length = 11)
    private String cnh;

    // ── situação / audit (system-set) ──
    @Column(name = "PacSit")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer situacao;

    @Column(name = "PacExpEsusErro")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer exportEsusErro;

    @Column(name = "PacUsuLogin", length = 20)
    private String usuarioAlteracao;

    @Column(name = "PacUsuLoginIns", length = 20)
    private String usuarioInclusao;

    @Column(name = "PacUsuDatAlt")
    private LocalDateTime dataUltimaAlteracao;

    @Column(name = "PacPesUsuCod")
    private Integer usuarioCod;

    @Column(name = "PacPesUsuLogin", length = 40)
    private String usuarioLogin;

    @Column(name = "PacPesCadInsUniCod")
    private Integer unidadeInclusaoCod;

    @Column(name = "PacPesCadAltUniCod")
    private Integer unidadeAlteracaoCod;

    // ── derived soundex (system-computed) ──
    @Column(name = "PacPesNomSoundex", length = 50)
    private String nomeSoundex;

    @Column(name = "PacPesNomMaeSoundex", length = 50)
    private String nomeMaeSoundex;

    @Column(name = "PacPesNomSocSoundex", length = 50)
    private String nomeSocialSoundex;

    public Paciente() {}

    /** R6/R7 clinical predicates. */
    public boolean isObito() { return obito != null && obito == 1; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getUnidadeCod() { return unidadeCod; }
    public void setUnidadeCod(Integer unidadeCod) { this.unidadeCod = unidadeCod; }
    public String getProntuario() { return prontuario == null ? null : prontuario.trim(); }
    public void setProntuario(String prontuario) { this.prontuario = prontuario; }
    public Long getNumeroIdentificacao() { return numeroIdentificacao; }
    public void setNumeroIdentificacao(Long v) { this.numeroIdentificacao = v; }
    public String getAlergia() { return alergia; }
    public void setAlergia(String alergia) { this.alergia = alergia; }
    public String getHistoricoDoencas() { return historicoDoencas; }
    public void setHistoricoDoencas(String v) { this.historicoDoencas = v; }
    public Integer getObito() { return obito; }
    public void setObito(Integer obito) { this.obito = obito; }
    public Boolean getInconsciente() { return inconsciente; }
    public void setInconsciente(Boolean v) { this.inconsciente = v; }
    public Boolean getSituacaoRua() { return situacaoRua; }
    public void setSituacaoRua(Boolean v) { this.situacaoRua = v; }
    public Boolean getSurtoPsiquiatrico() { return surtoPsiquiatrico; }
    public void setSurtoPsiquiatrico(Boolean v) { this.surtoPsiquiatrico = v; }
    public Integer getRendaFamiliar() { return rendaFamiliar; }
    public void setRendaFamiliar(Integer v) { this.rendaFamiliar = v; }
    public String getMeioTransporte() { return meioTransporte; }
    public void setMeioTransporte(String v) { this.meioTransporte = v; }
    public Boolean getBeneficioSocial() { return beneficioSocial; }
    public void setBeneficioSocial(Boolean v) { this.beneficioSocial = v; }
    public String getCnh() { return cnh; }
    public void setCnh(String cnh) { this.cnh = cnh; }
    public Integer getSituacao() { return situacao; }
    public void setSituacao(Integer situacao) { this.situacao = situacao; }
    public Integer getExportEsusErro() { return exportEsusErro; }
    public void setExportEsusErro(Integer v) { this.exportEsusErro = v; }
    public String getUsuarioAlteracao() { return usuarioAlteracao; }
    public void setUsuarioAlteracao(String v) { this.usuarioAlteracao = v; }
    public String getUsuarioInclusao() { return usuarioInclusao; }
    public void setUsuarioInclusao(String v) { this.usuarioInclusao = v; }
    public LocalDateTime getDataUltimaAlteracao() { return dataUltimaAlteracao; }
    public void setDataUltimaAlteracao(LocalDateTime v) { this.dataUltimaAlteracao = v; }
    public Integer getUsuarioCod() { return usuarioCod; }
    public void setUsuarioCod(Integer v) { this.usuarioCod = v; }
    public String getUsuarioLogin() { return usuarioLogin; }
    public void setUsuarioLogin(String v) { this.usuarioLogin = v; }
    public Integer getUnidadeInclusaoCod() { return unidadeInclusaoCod; }
    public void setUnidadeInclusaoCod(Integer v) { this.unidadeInclusaoCod = v; }
    public Integer getUnidadeAlteracaoCod() { return unidadeAlteracaoCod; }
    public void setUnidadeAlteracaoCod(Integer v) { this.unidadeAlteracaoCod = v; }
    public String getNomeSoundex() { return nomeSoundex; }
    public void setNomeSoundex(String v) { this.nomeSoundex = v; }
    public String getNomeMaeSoundex() { return nomeMaeSoundex; }
    public void setNomeMaeSoundex(String v) { this.nomeMaeSoundex = v; }
    public String getNomeSocialSoundex() { return nomeSocialSoundex; }
    public void setNomeSocialSoundex(String v) { this.nomeSocialSoundex = v; }

    /** Only the id — PHI is never logged. */
    @Override
    public String toString() { return "Paciente{id=" + id + "}"; }
}
