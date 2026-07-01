package br.gov.mandaguari.saude.usuariounidade.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Usuario x Unidade capability matrix — maps GeneXus SAU_USUUNI. Composite PK (UsuCod, UniUsuCod);
 * one row per (user, unit) carrying 54 native-boolean flags: 49 Bloq* = "block this module in this
 * unit" and 5 Per* = "grant this capability". UsuUniEspCod is an optional FK to SAU_ESP (the auditor's
 * specialty). No PHI — this is an authorization matrix. Flags are nullable in the live DB; a null flag
 * means "not blocked / not granted".
 */
@Entity
@Table(name = "SAU_USUUNI")
@IdClass(UsuarioUnidadeId.class)
public class UsuarioUnidade {

    @Id
    @Column(name = "UsuCod", nullable = false)
    private Integer usuCod;

    @Id
    @Column(name = "UniUsuCod", nullable = false)
    private Integer uniCod;

    /** Optional FK → SAU_ESP.EspCod (auditor specialty; pairs with permiteAgendaAuditor). */
    @Column(name = "UsuUniEspCod")
    private Integer especialidadeCod;

    @Column(name = "UsuUniBloqTab")
    private Boolean bloqueioTabela;
    @Column(name = "UsuUniBloqCad")
    private Boolean bloqueioCadastro;
    @Column(name = "UsuUniBloqAmb")
    private Boolean bloqueioAmbulatorio;
    @Column(name = "UsuUniPerConDir")
    private Boolean permiteConsultaDireta;
    @Column(name = "UsuUniBloqPrtCon")
    private Boolean bloqueioProntuarioConsulta;
    @Column(name = "UsuUniBloqPrtOdo")
    private Boolean bloqueioProntuarioOdonto;
    @Column(name = "UsuUniBloqResExa")
    private Boolean bloqueioResultadoExame;
    @Column(name = "UsuUniBloqEsus")
    private Boolean bloqueioEsus;
    @Column(name = "UsuUniBloqCaps")
    private Boolean bloqueioCaps;
    @Column(name = "UsuUniBloqNut")
    private Boolean bloqueioNutricao;
    @Column(name = "UsuUniBloqFar")
    private Boolean bloqueioFarmacia;
    @Column(name = "UsuUniPerBnafar")
    private Boolean permiteBnafar;
    @Column(name = "UsuUniBloqAlm")
    private Boolean bloqueioAlmoxarifado;
    @Column(name = "UsuUniBloqReq")
    private Boolean bloqueioRequisicao;
    @Column(name = "UsuUniBloqBen")
    private Boolean bloqueioBeneficio;
    @Column(name = "UsuUniBloqTra")
    private Boolean bloqueioTransporte;
    @Column(name = "UsuUniBloqVac")
    private Boolean bloqueioVacina;
    @Column(name = "UsuUniBloqAge")
    private Boolean bloqueioAgenda;
    @Column(name = "UsuUniBloqAgeMan")
    private Boolean bloqueioAgendaManual;
    @Column(name = "UsuUniBloqAgeExt")
    private Boolean bloqueioAgendaExterna;
    @Column(name = "UsuUniBloqAgeEsp")
    private Boolean bloqueioAgendaEspecial;
    @Column(name = "UsuUniPerAgeAud")
    private Boolean permiteAgendaAuditor;
    @Column(name = "UsuUniBloqLab")
    private Boolean bloqueioLaboratorio;
    @Column(name = "UsuUniBloqHos")
    private Boolean bloqueioHospital;
    @Column(name = "UsuUniBloqVig")
    private Boolean bloqueioVigilancia;
    @Column(name = "UsuUniBloqAgr")
    private Boolean bloqueioAgravo;
    @Column(name = "UsuUniBloqCMS")
    private Boolean bloqueioCms;
    @Column(name = "UsuUniBloqOuv")
    private Boolean bloqueioOuvidoria;
    @Column(name = "UsuUniBloqImp")
    private Boolean bloqueioImpressao;
    @Column(name = "UsuUniBloqExp")
    private Boolean bloqueioExportacao;
    @Column(name = "UsuUniBloqPar")
    private Boolean bloqueioParametro;
    @Column(name = "UsuUniBloqRel")
    private Boolean bloqueioRelatorio;
    @Column(name = "UsuUniBloqRelTab")
    private Boolean bloqueioRelatorioTabela;
    @Column(name = "UsuUniBloqRelCad")
    private Boolean bloqueioRelatorioCadastro;
    @Column(name = "UsuUniBloqRelAmb")
    private Boolean bloqueioRelatorioAmbulatorio;
    @Column(name = "UsuUniBloqRelEsus")
    private Boolean bloqueioRelatorioEsus;
    @Column(name = "UsuUniBloqRelCaps")
    private Boolean bloqueioRelatorioCaps;
    @Column(name = "UsuUniBloqRelNut")
    private Boolean bloqueioRelatorioNutricao;
    @Column(name = "UsuUniBloqRelVac")
    private Boolean bloqueioRelatorioVacina;
    @Column(name = "UsuUniBloqRelFar")
    private Boolean bloqueioRelatorioFarmacia;
    @Column(name = "UsuUniBloqRelAlm")
    private Boolean bloqueioRelatorioAlmoxarifado;
    @Column(name = "UsuUniBloqRelReq")
    private Boolean bloqueioRelatorioRequisicao;
    @Column(name = "UsuUniBloqRelBen")
    private Boolean bloqueioRelatorioBeneficio;
    @Column(name = "UsuUniBloqRelTra")
    private Boolean bloqueioRelatorioTransporte;
    @Column(name = "UsuUniBloqRelAge")
    private Boolean bloqueioRelatorioAgenda;
    @Column(name = "UsuUniBloqRelLab")
    private Boolean bloqueioRelatorioLaboratorio;
    @Column(name = "UsuUniBloqRelHos")
    private Boolean bloqueioRelatorioHospital;
    @Column(name = "UsuUniBloqRelVig")
    private Boolean bloqueioRelatorioVigilancia;
    @Column(name = "UsuUniBloqRelAgr")
    private Boolean bloqueioRelatorioAgravo;
    @Column(name = "UsuUniBloqRelOuv")
    private Boolean bloqueioRelatorioOuvidoria;
    @Column(name = "UsuUniBloqRelExp")
    private Boolean bloqueioRelatorioExportacao;
    @Column(name = "UsuUniBloqGra")
    private Boolean bloqueioGrafico;
    @Column(name = "UsuUniPerAgeAudPcd")
    private Boolean permiteAgendaAuditorPcd;
    @Column(name = "UsuUniPerSoaBnafar")
    private Boolean permiteSoaBnafar;

    public UsuarioUnidade() {}

    public Integer getUsuCod() { return usuCod; }
    public void setUsuCod(Integer usuCod) { this.usuCod = usuCod; }
    public Integer getUniCod() { return uniCod; }
    public void setUniCod(Integer uniCod) { this.uniCod = uniCod; }
    public Integer getEspecialidadeCod() { return especialidadeCod; }
    public void setEspecialidadeCod(Integer especialidadeCod) { this.especialidadeCod = especialidadeCod; }
    public Boolean getBloqueioTabela() { return bloqueioTabela; }
    public void setBloqueioTabela(Boolean bloqueioTabela) { this.bloqueioTabela = bloqueioTabela; }
    public Boolean getBloqueioCadastro() { return bloqueioCadastro; }
    public void setBloqueioCadastro(Boolean bloqueioCadastro) { this.bloqueioCadastro = bloqueioCadastro; }
    public Boolean getBloqueioAmbulatorio() { return bloqueioAmbulatorio; }
    public void setBloqueioAmbulatorio(Boolean bloqueioAmbulatorio) { this.bloqueioAmbulatorio = bloqueioAmbulatorio; }
    public Boolean getPermiteConsultaDireta() { return permiteConsultaDireta; }
    public void setPermiteConsultaDireta(Boolean permiteConsultaDireta) { this.permiteConsultaDireta = permiteConsultaDireta; }
    public Boolean getBloqueioProntuarioConsulta() { return bloqueioProntuarioConsulta; }
    public void setBloqueioProntuarioConsulta(Boolean bloqueioProntuarioConsulta) { this.bloqueioProntuarioConsulta = bloqueioProntuarioConsulta; }
    public Boolean getBloqueioProntuarioOdonto() { return bloqueioProntuarioOdonto; }
    public void setBloqueioProntuarioOdonto(Boolean bloqueioProntuarioOdonto) { this.bloqueioProntuarioOdonto = bloqueioProntuarioOdonto; }
    public Boolean getBloqueioResultadoExame() { return bloqueioResultadoExame; }
    public void setBloqueioResultadoExame(Boolean bloqueioResultadoExame) { this.bloqueioResultadoExame = bloqueioResultadoExame; }
    public Boolean getBloqueioEsus() { return bloqueioEsus; }
    public void setBloqueioEsus(Boolean bloqueioEsus) { this.bloqueioEsus = bloqueioEsus; }
    public Boolean getBloqueioCaps() { return bloqueioCaps; }
    public void setBloqueioCaps(Boolean bloqueioCaps) { this.bloqueioCaps = bloqueioCaps; }
    public Boolean getBloqueioNutricao() { return bloqueioNutricao; }
    public void setBloqueioNutricao(Boolean bloqueioNutricao) { this.bloqueioNutricao = bloqueioNutricao; }
    public Boolean getBloqueioFarmacia() { return bloqueioFarmacia; }
    public void setBloqueioFarmacia(Boolean bloqueioFarmacia) { this.bloqueioFarmacia = bloqueioFarmacia; }
    public Boolean getPermiteBnafar() { return permiteBnafar; }
    public void setPermiteBnafar(Boolean permiteBnafar) { this.permiteBnafar = permiteBnafar; }
    public Boolean getBloqueioAlmoxarifado() { return bloqueioAlmoxarifado; }
    public void setBloqueioAlmoxarifado(Boolean bloqueioAlmoxarifado) { this.bloqueioAlmoxarifado = bloqueioAlmoxarifado; }
    public Boolean getBloqueioRequisicao() { return bloqueioRequisicao; }
    public void setBloqueioRequisicao(Boolean bloqueioRequisicao) { this.bloqueioRequisicao = bloqueioRequisicao; }
    public Boolean getBloqueioBeneficio() { return bloqueioBeneficio; }
    public void setBloqueioBeneficio(Boolean bloqueioBeneficio) { this.bloqueioBeneficio = bloqueioBeneficio; }
    public Boolean getBloqueioTransporte() { return bloqueioTransporte; }
    public void setBloqueioTransporte(Boolean bloqueioTransporte) { this.bloqueioTransporte = bloqueioTransporte; }
    public Boolean getBloqueioVacina() { return bloqueioVacina; }
    public void setBloqueioVacina(Boolean bloqueioVacina) { this.bloqueioVacina = bloqueioVacina; }
    public Boolean getBloqueioAgenda() { return bloqueioAgenda; }
    public void setBloqueioAgenda(Boolean bloqueioAgenda) { this.bloqueioAgenda = bloqueioAgenda; }
    public Boolean getBloqueioAgendaManual() { return bloqueioAgendaManual; }
    public void setBloqueioAgendaManual(Boolean bloqueioAgendaManual) { this.bloqueioAgendaManual = bloqueioAgendaManual; }
    public Boolean getBloqueioAgendaExterna() { return bloqueioAgendaExterna; }
    public void setBloqueioAgendaExterna(Boolean bloqueioAgendaExterna) { this.bloqueioAgendaExterna = bloqueioAgendaExterna; }
    public Boolean getBloqueioAgendaEspecial() { return bloqueioAgendaEspecial; }
    public void setBloqueioAgendaEspecial(Boolean bloqueioAgendaEspecial) { this.bloqueioAgendaEspecial = bloqueioAgendaEspecial; }
    public Boolean getPermiteAgendaAuditor() { return permiteAgendaAuditor; }
    public void setPermiteAgendaAuditor(Boolean permiteAgendaAuditor) { this.permiteAgendaAuditor = permiteAgendaAuditor; }
    public Boolean getBloqueioLaboratorio() { return bloqueioLaboratorio; }
    public void setBloqueioLaboratorio(Boolean bloqueioLaboratorio) { this.bloqueioLaboratorio = bloqueioLaboratorio; }
    public Boolean getBloqueioHospital() { return bloqueioHospital; }
    public void setBloqueioHospital(Boolean bloqueioHospital) { this.bloqueioHospital = bloqueioHospital; }
    public Boolean getBloqueioVigilancia() { return bloqueioVigilancia; }
    public void setBloqueioVigilancia(Boolean bloqueioVigilancia) { this.bloqueioVigilancia = bloqueioVigilancia; }
    public Boolean getBloqueioAgravo() { return bloqueioAgravo; }
    public void setBloqueioAgravo(Boolean bloqueioAgravo) { this.bloqueioAgravo = bloqueioAgravo; }
    public Boolean getBloqueioCms() { return bloqueioCms; }
    public void setBloqueioCms(Boolean bloqueioCms) { this.bloqueioCms = bloqueioCms; }
    public Boolean getBloqueioOuvidoria() { return bloqueioOuvidoria; }
    public void setBloqueioOuvidoria(Boolean bloqueioOuvidoria) { this.bloqueioOuvidoria = bloqueioOuvidoria; }
    public Boolean getBloqueioImpressao() { return bloqueioImpressao; }
    public void setBloqueioImpressao(Boolean bloqueioImpressao) { this.bloqueioImpressao = bloqueioImpressao; }
    public Boolean getBloqueioExportacao() { return bloqueioExportacao; }
    public void setBloqueioExportacao(Boolean bloqueioExportacao) { this.bloqueioExportacao = bloqueioExportacao; }
    public Boolean getBloqueioParametro() { return bloqueioParametro; }
    public void setBloqueioParametro(Boolean bloqueioParametro) { this.bloqueioParametro = bloqueioParametro; }
    public Boolean getBloqueioRelatorio() { return bloqueioRelatorio; }
    public void setBloqueioRelatorio(Boolean bloqueioRelatorio) { this.bloqueioRelatorio = bloqueioRelatorio; }
    public Boolean getBloqueioRelatorioTabela() { return bloqueioRelatorioTabela; }
    public void setBloqueioRelatorioTabela(Boolean bloqueioRelatorioTabela) { this.bloqueioRelatorioTabela = bloqueioRelatorioTabela; }
    public Boolean getBloqueioRelatorioCadastro() { return bloqueioRelatorioCadastro; }
    public void setBloqueioRelatorioCadastro(Boolean bloqueioRelatorioCadastro) { this.bloqueioRelatorioCadastro = bloqueioRelatorioCadastro; }
    public Boolean getBloqueioRelatorioAmbulatorio() { return bloqueioRelatorioAmbulatorio; }
    public void setBloqueioRelatorioAmbulatorio(Boolean bloqueioRelatorioAmbulatorio) { this.bloqueioRelatorioAmbulatorio = bloqueioRelatorioAmbulatorio; }
    public Boolean getBloqueioRelatorioEsus() { return bloqueioRelatorioEsus; }
    public void setBloqueioRelatorioEsus(Boolean bloqueioRelatorioEsus) { this.bloqueioRelatorioEsus = bloqueioRelatorioEsus; }
    public Boolean getBloqueioRelatorioCaps() { return bloqueioRelatorioCaps; }
    public void setBloqueioRelatorioCaps(Boolean bloqueioRelatorioCaps) { this.bloqueioRelatorioCaps = bloqueioRelatorioCaps; }
    public Boolean getBloqueioRelatorioNutricao() { return bloqueioRelatorioNutricao; }
    public void setBloqueioRelatorioNutricao(Boolean bloqueioRelatorioNutricao) { this.bloqueioRelatorioNutricao = bloqueioRelatorioNutricao; }
    public Boolean getBloqueioRelatorioVacina() { return bloqueioRelatorioVacina; }
    public void setBloqueioRelatorioVacina(Boolean bloqueioRelatorioVacina) { this.bloqueioRelatorioVacina = bloqueioRelatorioVacina; }
    public Boolean getBloqueioRelatorioFarmacia() { return bloqueioRelatorioFarmacia; }
    public void setBloqueioRelatorioFarmacia(Boolean bloqueioRelatorioFarmacia) { this.bloqueioRelatorioFarmacia = bloqueioRelatorioFarmacia; }
    public Boolean getBloqueioRelatorioAlmoxarifado() { return bloqueioRelatorioAlmoxarifado; }
    public void setBloqueioRelatorioAlmoxarifado(Boolean bloqueioRelatorioAlmoxarifado) { this.bloqueioRelatorioAlmoxarifado = bloqueioRelatorioAlmoxarifado; }
    public Boolean getBloqueioRelatorioRequisicao() { return bloqueioRelatorioRequisicao; }
    public void setBloqueioRelatorioRequisicao(Boolean bloqueioRelatorioRequisicao) { this.bloqueioRelatorioRequisicao = bloqueioRelatorioRequisicao; }
    public Boolean getBloqueioRelatorioBeneficio() { return bloqueioRelatorioBeneficio; }
    public void setBloqueioRelatorioBeneficio(Boolean bloqueioRelatorioBeneficio) { this.bloqueioRelatorioBeneficio = bloqueioRelatorioBeneficio; }
    public Boolean getBloqueioRelatorioTransporte() { return bloqueioRelatorioTransporte; }
    public void setBloqueioRelatorioTransporte(Boolean bloqueioRelatorioTransporte) { this.bloqueioRelatorioTransporte = bloqueioRelatorioTransporte; }
    public Boolean getBloqueioRelatorioAgenda() { return bloqueioRelatorioAgenda; }
    public void setBloqueioRelatorioAgenda(Boolean bloqueioRelatorioAgenda) { this.bloqueioRelatorioAgenda = bloqueioRelatorioAgenda; }
    public Boolean getBloqueioRelatorioLaboratorio() { return bloqueioRelatorioLaboratorio; }
    public void setBloqueioRelatorioLaboratorio(Boolean bloqueioRelatorioLaboratorio) { this.bloqueioRelatorioLaboratorio = bloqueioRelatorioLaboratorio; }
    public Boolean getBloqueioRelatorioHospital() { return bloqueioRelatorioHospital; }
    public void setBloqueioRelatorioHospital(Boolean bloqueioRelatorioHospital) { this.bloqueioRelatorioHospital = bloqueioRelatorioHospital; }
    public Boolean getBloqueioRelatorioVigilancia() { return bloqueioRelatorioVigilancia; }
    public void setBloqueioRelatorioVigilancia(Boolean bloqueioRelatorioVigilancia) { this.bloqueioRelatorioVigilancia = bloqueioRelatorioVigilancia; }
    public Boolean getBloqueioRelatorioAgravo() { return bloqueioRelatorioAgravo; }
    public void setBloqueioRelatorioAgravo(Boolean bloqueioRelatorioAgravo) { this.bloqueioRelatorioAgravo = bloqueioRelatorioAgravo; }
    public Boolean getBloqueioRelatorioOuvidoria() { return bloqueioRelatorioOuvidoria; }
    public void setBloqueioRelatorioOuvidoria(Boolean bloqueioRelatorioOuvidoria) { this.bloqueioRelatorioOuvidoria = bloqueioRelatorioOuvidoria; }
    public Boolean getBloqueioRelatorioExportacao() { return bloqueioRelatorioExportacao; }
    public void setBloqueioRelatorioExportacao(Boolean bloqueioRelatorioExportacao) { this.bloqueioRelatorioExportacao = bloqueioRelatorioExportacao; }
    public Boolean getBloqueioGrafico() { return bloqueioGrafico; }
    public void setBloqueioGrafico(Boolean bloqueioGrafico) { this.bloqueioGrafico = bloqueioGrafico; }
    public Boolean getPermiteAgendaAuditorPcd() { return permiteAgendaAuditorPcd; }
    public void setPermiteAgendaAuditorPcd(Boolean permiteAgendaAuditorPcd) { this.permiteAgendaAuditorPcd = permiteAgendaAuditorPcd; }
    public Boolean getPermiteSoaBnafar() { return permiteSoaBnafar; }
    public void setPermiteSoaBnafar(Boolean permiteSoaBnafar) { this.permiteSoaBnafar = permiteSoaBnafar; }
}
