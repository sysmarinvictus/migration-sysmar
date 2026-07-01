package br.gov.mandaguari.saude.profissionalespecialidade.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Especialidade do Profissional — maps GeneXus {@code SAU_PROESP} (1904 rows), the association between a
 * professional (SAU_PRO) and a specialty (SAU_ESP). Composite PK ({@code ProPesCod}, {@code EspCod});
 * carries the 'prioritário' flag, the situação, and the per-shift agenda quotas. Rules mined in
 * {@code sau_proesp_impl.java} (see SLICE-SPEC SAU_PROESP) — cited as {@code // R<n>} where enforced.
 *
 * <p>No {@code @ManyToOne} to SAU_PRO/SAU_ESP: the FKs are validated by existence queries (R1/R2), and
 * SAU_PRO is only tested (not verified) — a raw-id association keeps this slice decoupled.
 */
@Entity
@Table(name = "SAU_PROESP")
@IdClass(ProfissionalEspecialidadeId.class)
public class ProfissionalEspecialidade {

    @Id
    @Column(name = "ProPesCod", nullable = false)
    private Long profissionalId;

    @Id
    @Column(name = "EspCod", nullable = false)
    private Integer especialidadeId;

    /** R4: 0/1 checkbox 'prioritário'; exposed as boolean; default 0. */
    @Column(name = "ProEspPri")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Short prioritario;

    /** R3: 1=Ativo; default 1 on insert. */
    @Column(name = "ProEspSit")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Short situacao;

    @Column(name = "ProEspAgeManQtd")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Short agendaManhaQtd;

    @Column(name = "ProEspAgeTarQtd")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Short agendaTardeQtd;

    @Column(name = "ProEspAgeNoiQtd")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Short agendaNoiteQtd;

    public ProfissionalEspecialidade() {}

    /** R4: 'prioritário' is true iff the flag is 1. */
    public boolean isPrioritario() { return prioritario != null && prioritario == 1; }

    public Long getProfissionalId() { return profissionalId; }
    public void setProfissionalId(Long profissionalId) { this.profissionalId = profissionalId; }
    public Integer getEspecialidadeId() { return especialidadeId; }
    public void setEspecialidadeId(Integer especialidadeId) { this.especialidadeId = especialidadeId; }
    public Short getPrioritario() { return prioritario; }
    public void setPrioritario(Short prioritario) { this.prioritario = prioritario; }
    public Short getSituacao() { return situacao; }
    public void setSituacao(Short situacao) { this.situacao = situacao; }
    public Short getAgendaManhaQtd() { return agendaManhaQtd; }
    public void setAgendaManhaQtd(Short agendaManhaQtd) { this.agendaManhaQtd = agendaManhaQtd; }
    public Short getAgendaTardeQtd() { return agendaTardeQtd; }
    public void setAgendaTardeQtd(Short agendaTardeQtd) { this.agendaTardeQtd = agendaTardeQtd; }
    public Short getAgendaNoiteQtd() { return agendaNoiteQtd; }
    public void setAgendaNoiteQtd(Short agendaNoiteQtd) { this.agendaNoiteQtd = agendaNoiteQtd; }
}
