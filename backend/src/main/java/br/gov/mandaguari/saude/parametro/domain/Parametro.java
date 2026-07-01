package br.gov.mandaguari.saude.parametro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Parâmetros do sistema — maps GeneXus {@code SAU_PAR}, a per-empresa SINGLETON config row (PK
 * {@code ParEmpCod}). The legacy table has 232 columns spanning many modules; this entity maps only the
 * FOCUSED, high-value subset the {@code sau_par_ger} (general) and {@code sau_par_amb} (ambulatorial)
 * transactions edit that matter to the receituário: prescription validity, user/security day-counts,
 * secretariat report header, and key policy flags. {@code ddl-auto=validate} only checks mapped columns,
 * so the ~215 unmapped config columns are intentionally left out (see SLICE-SPEC deferrals).
 *
 * <p>Types are LIVE-verified: {@code smallint} day-counts/flags → Integer + {@code @JdbcTypeCode(SMALLINT)};
 * native {@code boolean} flags → Boolean; {@code char(n)} → trimmed getters.
 */
@Entity
@Table(name = "SAU_PAR")
public class Parametro {

    @Id
    @Column(name = "ParEmpCod", nullable = false)
    private Integer empresaCod;

    // ── General (sau_par_ger): prescription validity ──
    /** Whether prescriptions carry a validity limit at all. */
    @Column(name = "ParValidadeReceita")
    private Boolean validadeReceita;

    @Column(name = "ParValidadeReceitaSimples")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer validadeReceitaSimplesDias;

    @Column(name = "ParValidadeReceitaUsoCon")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer validadeReceitaUsoContinuoDias;

    @Column(name = "ParValidadeReceitaConEsp")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer validadeReceitaControleEspecialDias;

    // ── General: user / security ──
    @Column(name = "ParInaUsuDias")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer inatividadeUsuarioDias;

    @Column(name = "ParSenUsuDias")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer senhaUsuarioDias;

    // ── General: secretariat report header ──
    @Column(name = "ParSecr", length = 50)
    private String secretaria;

    @Column(name = "ParSecrEnd", length = 50)
    private String secretariaEndereco;

    @Column(name = "ParSecrCep", length = 8)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String secretariaCep;

    @Column(name = "ParSecrFone1", length = 20)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String secretariaFone1;

    @Column(name = "ParSecrFone2", length = 20)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String secretariaFone2;

    @Column(name = "ParSecrEmail", length = 70)
    private String secretariaEmail;

    // ── General: policy flags ──
    @Column(name = "ParCadSemCns")
    private Boolean cadastroSemCns;

    @Column(name = "ParRecComprador")
    private Boolean reciboComprador;

    // ── Ambulatorial (sau_par_amb): policy flags ──
    @Column(name = "ParExigeCid10Atestado")
    private Boolean exigeCid10Atestado;

    @Column(name = "ParEstornarAtendimento")
    private Boolean estornarAtendimento;

    @Column(name = "ParImpRiscoMaterno")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer imprimeRiscoMaterno;

    @Column(name = "ParAteHis")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer atendimentoHistorico;

    public Parametro() {}

    private static String trim(String s) { return s == null ? null : s.trim(); }

    public Integer getEmpresaCod() { return empresaCod; }
    public void setEmpresaCod(Integer empresaCod) { this.empresaCod = empresaCod; }
    public Boolean getValidadeReceita() { return validadeReceita; }
    public void setValidadeReceita(Boolean validadeReceita) { this.validadeReceita = validadeReceita; }
    public Integer getValidadeReceitaSimplesDias() { return validadeReceitaSimplesDias; }
    public void setValidadeReceitaSimplesDias(Integer v) { this.validadeReceitaSimplesDias = v; }
    public Integer getValidadeReceitaUsoContinuoDias() { return validadeReceitaUsoContinuoDias; }
    public void setValidadeReceitaUsoContinuoDias(Integer v) { this.validadeReceitaUsoContinuoDias = v; }
    public Integer getValidadeReceitaControleEspecialDias() { return validadeReceitaControleEspecialDias; }
    public void setValidadeReceitaControleEspecialDias(Integer v) { this.validadeReceitaControleEspecialDias = v; }
    public Integer getInatividadeUsuarioDias() { return inatividadeUsuarioDias; }
    public void setInatividadeUsuarioDias(Integer v) { this.inatividadeUsuarioDias = v; }
    public Integer getSenhaUsuarioDias() { return senhaUsuarioDias; }
    public void setSenhaUsuarioDias(Integer v) { this.senhaUsuarioDias = v; }
    public String getSecretaria() { return secretaria; }
    public void setSecretaria(String secretaria) { this.secretaria = secretaria; }
    public String getSecretariaEndereco() { return secretariaEndereco; }
    public void setSecretariaEndereco(String v) { this.secretariaEndereco = v; }
    public String getSecretariaCep() { return trim(secretariaCep); }
    public void setSecretariaCep(String v) { this.secretariaCep = v; }
    public String getSecretariaFone1() { return trim(secretariaFone1); }
    public void setSecretariaFone1(String v) { this.secretariaFone1 = v; }
    public String getSecretariaFone2() { return trim(secretariaFone2); }
    public void setSecretariaFone2(String v) { this.secretariaFone2 = v; }
    public String getSecretariaEmail() { return secretariaEmail; }
    public void setSecretariaEmail(String v) { this.secretariaEmail = v; }
    public Boolean getCadastroSemCns() { return cadastroSemCns; }
    public void setCadastroSemCns(Boolean v) { this.cadastroSemCns = v; }
    public Boolean getReciboComprador() { return reciboComprador; }
    public void setReciboComprador(Boolean v) { this.reciboComprador = v; }
    public Boolean getExigeCid10Atestado() { return exigeCid10Atestado; }
    public void setExigeCid10Atestado(Boolean v) { this.exigeCid10Atestado = v; }
    public Boolean getEstornarAtendimento() { return estornarAtendimento; }
    public void setEstornarAtendimento(Boolean v) { this.estornarAtendimento = v; }
    public Integer getImprimeRiscoMaterno() { return imprimeRiscoMaterno; }
    public void setImprimeRiscoMaterno(Integer v) { this.imprimeRiscoMaterno = v; }
    public Integer getAtendimentoHistorico() { return atendimentoHistorico; }
    public void setAtendimentoHistorico(Integer v) { this.atendimentoHistorico = v; }

    @Override
    public String toString() { return "Parametro{empresaCod=" + empresaCod + "}"; }
}
