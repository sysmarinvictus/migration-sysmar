package br.gov.mandaguari.saude.especialidade.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;

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

    @Column(name = "EspSit")                         // live: SMALLINT (1/2)
    @Convert(converter = SituacaoToShortConverter.class)
    private String situacao;

    @Column(name = "EspAux")                         // live: INTEGER (0/1)
    @Convert(converter = BooleanToIntegerConverter.class)
    private Boolean auxiliar;

    /** CBO occupation code — CHAR(6) in live DB (confirmed 2026-06-21). */
    @Column(name = "EspCborCod", length = 6)
    @JdbcTypeCode(Types.CHAR)
    private String cborCodigo;

    // --- scheduling-queue parameters (estagnado / tempo-máximo / vagas) per urgency tier ---
    // live columns are SMALLINT → @JdbcTypeCode(Types.SMALLINT) so ddl-auto=validate matches.
    @Column(name = "EspLstAgendEstagnadoMuitoUrg") @JdbcTypeCode(Types.SMALLINT) private Integer agendaEstagnadoMuitoUrgente;
    @Column(name = "EspLstAgendEstagnadoNormal")   @JdbcTypeCode(Types.SMALLINT) private Integer agendaEstagnadoNormal;
    @Column(name = "EspLstAgendEstagnadoPri")      @JdbcTypeCode(Types.SMALLINT) private Integer agendaEstagnadoPrioritario;
    @Column(name = "EspLstAgendEstagnadoUrg")      @JdbcTypeCode(Types.SMALLINT) private Integer agendaEstagnadoUrgente;
    @Column(name = "EspLstAgendTempoMaxMuitoUrg")  @JdbcTypeCode(Types.SMALLINT) private Integer agendaTempoMaxMuitoUrgente;
    @Column(name = "EspLstAgendTempoMaxNormal")    @JdbcTypeCode(Types.SMALLINT) private Integer agendaTempoMaxNormal;
    @Column(name = "EspLstAgendTempoMaxPri")       @JdbcTypeCode(Types.SMALLINT) private Integer agendaTempoMaxPrioritario;
    @Column(name = "EspLstAgendTempoMaxUrg")       @JdbcTypeCode(Types.SMALLINT) private Integer agendaTempoMaxUrgente;
    @Column(name = "EspLstAgendVagaMuitoUrgMax")   @JdbcTypeCode(Types.SMALLINT) private Integer agendaVagaMuitoUrgenteMax;
    @Column(name = "EspLstAgendVagaMuitoUrgMin")   @JdbcTypeCode(Types.SMALLINT) private Integer agendaVagaMuitoUrgenteMin;
    @Column(name = "EspLstAgendVagaNorMax")        @JdbcTypeCode(Types.SMALLINT) private Integer agendaVagaNormalMax;
    @Column(name = "EspLstAgendVagaNorMin")        @JdbcTypeCode(Types.SMALLINT) private Integer agendaVagaNormalMin;
    @Column(name = "EspLstAgendVagaPriMax")        @JdbcTypeCode(Types.SMALLINT) private Integer agendaVagaPrioritarioMax;
    @Column(name = "EspLstAgendVagaPriMin")        @JdbcTypeCode(Types.SMALLINT) private Integer agendaVagaPrioritarioMin;
    @Column(name = "EspLstAgendVagaUrgMax")        @JdbcTypeCode(Types.SMALLINT) private Integer agendaVagaUrgenteMax;
    @Column(name = "EspLstAgendVagaUrgMin")        @JdbcTypeCode(Types.SMALLINT) private Integer agendaVagaUrgenteMin;

    public Especialidade() {} // JPA + service instantiation (different package)

    // --- getters / setters ---
    public Integer getCodigo() { return codigo; }
    public void setCodigo(Integer codigo) { this.codigo = codigo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getSituacao() { return situacao; }
    public void setSituacao(String situacao) { this.situacao = situacao; }
    public Boolean getAuxiliar() { return auxiliar; }
    public void setAuxiliar(Boolean auxiliar) { this.auxiliar = auxiliar; }
    public String getCborCodigo() { return cborCodigo; }
    public void setCborCodigo(String cborCodigo) { this.cborCodigo = cborCodigo; }

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
