package br.gov.mandaguari.saude.programa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Programa do Sistema — maps GeneXus {@code SAU_PRG} (1240 rows). The PK {@code PrgCod} is a
 * VARCHAR(30) program/screen name (e.g. {@code AGENDA}, {@code ATEMED}), NOT a number — it is the key
 * referenced by the permission matrices SAU_PRFCON ({@code PrfPrgCod}) and SAU_USUCON ({@code PrgCod}).
 *
 * <p>{@code GrpCod} → SAU_PRGGRP (raw id; group not coupled via @ManyToOne). Flags {@code PrgAdm}/
 * {@code PrgMed} are smallint markers; {@code PrgAcessoPub} a boolean "public access" flag.
 */
@Entity
@Table(name = "SAU_PRG")
public class Programa {

    @Id
    @Column(name = "PrgCod", length = 30, nullable = false)
    private String id;

    @Column(name = "PrgNom", length = 100)
    private String nome;

    @Column(name = "GrpCod")
    private Integer grupoId;

    @Column(name = "PrgAdm")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Short admin;

    @Column(name = "PrgMed")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Short medico;

    @Column(name = "PrgAcessoPub")
    private Boolean acessoPublico;

    public Programa() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Integer getGrupoId() { return grupoId; }
    public void setGrupoId(Integer grupoId) { this.grupoId = grupoId; }
    public Short getAdmin() { return admin; }
    public void setAdmin(Short admin) { this.admin = admin; }
    public Short getMedico() { return medico; }
    public void setMedico(Short medico) { this.medico = medico; }
    public Boolean getAcessoPublico() { return acessoPublico; }
    public void setAcessoPublico(Boolean acessoPublico) { this.acessoPublico = acessoPublico; }
}
