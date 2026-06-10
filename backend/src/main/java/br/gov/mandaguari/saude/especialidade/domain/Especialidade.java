package br.gov.mandaguari.saude.especialidade.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Especialidade médica — maps the existing GeneXus table {@code SAU_ESP}.
 *
 * <p>Physical column names are pinned to the GeneXus names (see SLICE-SPEC SAU_ESP). The CBO FK is
 * kept as a raw id ({@code cborCodigo}) because the occupation slice (SAU_CBOR) is not yet migrated
 * — the description is derived via a lookup query (see {@code EspecialidadeRepository}).
 *
 * ⚠ Column types/nullability are unverified against the live DB (no gxmetadata for SAU_ESP) — the
 * schema-mapper must confirm; {@code ddl-auto=validate} will flag any mismatch against production.
 */
@Entity
@Table(name = "SAU_ESP")
public class Especialidade {

    @Id
    @Column(name = "EspCod", nullable = false)
    private Integer codigo;

    @Column(name = "EspNom", length = 50, nullable = false)
    private String nome;

    @Column(name = "EspSit", length = 1)
    private String situacao;

    @Column(name = "EspAux")
    private Boolean auxiliar;

    @Column(name = "EspCborCod")
    private Integer cborCodigo;

    // --- scheduling-queue parameters (estagnado / tempo-máximo / vagas) per urgency tier ---
    @Column(name = "EspLstAgendEstagnadoMuitoUrg") private Integer agendaEstagnadoMuitoUrgente;
    @Column(name = "EspLstAgendEstagnadoNormal")   private Integer agendaEstagnadoNormal;
    @Column(name = "EspLstAgendEstagnadoPri")      private Integer agendaEstagnadoPrioritario;
    @Column(name = "EspLstAgendEstagnadoUrg")      private Integer agendaEstagnadoUrgente;
    @Column(name = "EspLstAgendTempoMaxMuitoUrg")  private Integer agendaTempoMaxMuitoUrgente;
    @Column(name = "EspLstAgendTempoMaxNormal")    private Integer agendaTempoMaxNormal;
    @Column(name = "EspLstAgendTempoMaxPri")       private Integer agendaTempoMaxPrioritario;
    @Column(name = "EspLstAgendTempoMaxUrg")       private Integer agendaTempoMaxUrgente;
    @Column(name = "EspLstAgendVagaMuitoUrgMax")   private Integer agendaVagaMuitoUrgenteMax;
    @Column(name = "EspLstAgendVagaMuitoUrgMin")   private Integer agendaVagaMuitoUrgenteMin;
    @Column(name = "EspLstAgendVagaNorMax")        private Integer agendaVagaNormalMax;
    @Column(name = "EspLstAgendVagaNorMin")        private Integer agendaVagaNormalMin;
    @Column(name = "EspLstAgendVagaPriMax")        private Integer agendaVagaPrioritarioMax;
    @Column(name = "EspLstAgendVagaPriMin")        private Integer agendaVagaPrioritarioMin;
    @Column(name = "EspLstAgendVagaUrgMax")        private Integer agendaVagaUrgenteMax;
    @Column(name = "EspLstAgendVagaUrgMin")        private Integer agendaVagaUrgenteMin;

    protected Especialidade() {} // JPA

    // --- getters / setters ---
    public Integer getCodigo() { return codigo; }
    public void setCodigo(Integer codigo) { this.codigo = codigo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getSituacao() { return situacao; }
    public void setSituacao(String situacao) { this.situacao = situacao; }
    public Boolean getAuxiliar() { return auxiliar; }
    public void setAuxiliar(Boolean auxiliar) { this.auxiliar = auxiliar; }
    public Integer getCborCodigo() { return cborCodigo; }
    public void setCborCodigo(Integer cborCodigo) { this.cborCodigo = cborCodigo; }

    public Integer getAgendaEstagnadoMuitoUrgente() { return agendaEstagnadoMuitoUrgente; }
    public void setAgendaEstagnadoMuitoUrgente(Integer v) { this.agendaEstagnadoMuitoUrgente = v; }
    public Integer getAgendaEstagnadoNormal() { return agendaEstagnadoNormal; }
    public void setAgendaEstagnadoNormal(Integer v) { this.agendaEstagnadoNormal = v; }
    public Integer getAgendaEstagnadoPrioritario() { return agendaEstagnadoPrioritario; }
    public void setAgendaEstagnadoPrioritario(Integer v) { this.agendaEstagnadoPrioritario = v; }
    public Integer getAgendaEstagnadoUrgente() { return agendaEstagnadoUrgente; }
    public void setAgendaEstagnadoUrgente(Integer v) { this.agendaEstagnadoUrgente = v; }
    public Integer getAgendaTempoMaxMuitoUrgente() { return agendaTempoMaxMuitoUrgente; }
    public void setAgendaTempoMaxMuitoUrgente(Integer v) { this.agendaTempoMaxMuitoUrgente = v; }
    public Integer getAgendaTempoMaxNormal() { return agendaTempoMaxNormal; }
    public void setAgendaTempoMaxNormal(Integer v) { this.agendaTempoMaxNormal = v; }
    public Integer getAgendaTempoMaxPrioritario() { return agendaTempoMaxPrioritario; }
    public void setAgendaTempoMaxPrioritario(Integer v) { this.agendaTempoMaxPrioritario = v; }
    public Integer getAgendaTempoMaxUrgente() { return agendaTempoMaxUrgente; }
    public void setAgendaTempoMaxUrgente(Integer v) { this.agendaTempoMaxUrgente = v; }
    public Integer getAgendaVagaMuitoUrgenteMax() { return agendaVagaMuitoUrgenteMax; }
    public void setAgendaVagaMuitoUrgenteMax(Integer v) { this.agendaVagaMuitoUrgenteMax = v; }
    public Integer getAgendaVagaMuitoUrgenteMin() { return agendaVagaMuitoUrgenteMin; }
    public void setAgendaVagaMuitoUrgenteMin(Integer v) { this.agendaVagaMuitoUrgenteMin = v; }
    public Integer getAgendaVagaNormalMax() { return agendaVagaNormalMax; }
    public void setAgendaVagaNormalMax(Integer v) { this.agendaVagaNormalMax = v; }
    public Integer getAgendaVagaNormalMin() { return agendaVagaNormalMin; }
    public void setAgendaVagaNormalMin(Integer v) { this.agendaVagaNormalMin = v; }
    public Integer getAgendaVagaPrioritarioMax() { return agendaVagaPrioritarioMax; }
    public void setAgendaVagaPrioritarioMax(Integer v) { this.agendaVagaPrioritarioMax = v; }
    public Integer getAgendaVagaPrioritarioMin() { return agendaVagaPrioritarioMin; }
    public void setAgendaVagaPrioritarioMin(Integer v) { this.agendaVagaPrioritarioMin = v; }
    public Integer getAgendaVagaUrgenteMax() { return agendaVagaUrgenteMax; }
    public void setAgendaVagaUrgenteMax(Integer v) { this.agendaVagaUrgenteMax = v; }
    public Integer getAgendaVagaUrgenteMin() { return agendaVagaUrgenteMin; }
    public void setAgendaVagaUrgenteMin(Integer v) { this.agendaVagaUrgenteMin = v; }
}
