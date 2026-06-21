package br.gov.mandaguari.saude.setor.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Setor da Unidade de Atendimento — maps SAU_UNISETOR (composite PK: UniCod + SetorCod). */
@Entity
@Table(name = "SAU_UNISETOR")
@IdClass(UniSetorId.class)
public class UniSetor {

    @Id
    @Column(name = "UniCod", nullable = false)
    private Integer uniCod;

    @Id
    @Column(name = "SetorCod", nullable = false)
    private Integer setorCod;

    @Column(name = "SetorNom", length = 50, nullable = false)
    private String nome;

    /** Estocador flag: 0=não, 1=sim (GX picture '9', range 0–9). */
    @Column(name = "SetorEstocador", nullable = false)
    private Short estocador;

    /** Situation: 'ativo' or 'inativo' (GX varchar 40). */
    @Column(name = "SetorSituacao", length = 40, nullable = false)
    private String situacao;

    /**
     * Date/time the sector was inactivated. GeneXus uses nullDate (1900-01-01) as
     * the sentinel for "not set" — see UniSetorService.toGxDate / fromGxDate.
     */
    @Column(name = "SetorDataInativo")
    private LocalDateTime dataInativo;

    @Column(name = "SetorHorIni")
    private LocalDateTime horarioInicio;

    @Column(name = "SetorHorFim")
    private LocalDateTime horarioFim;

    public Integer getUniCod() { return uniCod; }
    public void setUniCod(Integer uniCod) { this.uniCod = uniCod; }
    public Integer getSetorCod() { return setorCod; }
    public void setSetorCod(Integer setorCod) { this.setorCod = setorCod; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Short getEstocador() { return estocador; }
    public void setEstocador(Short estocador) { this.estocador = estocador; }
    public String getSituacao() { return situacao; }
    public void setSituacao(String situacao) { this.situacao = situacao; }
    public LocalDateTime getDataInativo() { return dataInativo; }
    public void setDataInativo(LocalDateTime dataInativo) { this.dataInativo = dataInativo; }
    public LocalDateTime getHorarioInicio() { return horarioInicio; }
    public void setHorarioInicio(LocalDateTime horarioInicio) { this.horarioInicio = horarioInicio; }
    public LocalDateTime getHorarioFim() { return horarioFim; }
    public void setHorarioFim(LocalDateTime horarioFim) { this.horarioFim = horarioFim; }
}
