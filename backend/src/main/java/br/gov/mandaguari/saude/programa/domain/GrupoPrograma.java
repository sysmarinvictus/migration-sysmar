package br.gov.mandaguari.saude.programa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Grupo de Programas — maps GeneXus {@code SAU_PRGGRP} (28 rows, leaf parent of SAU_PRG). Groups the
 * program catalog into functional areas (TABELAS, CADASTRO, ATENDIMENTO, …).
 */
@Entity
@Table(name = "SAU_PRGGRP")
public class GrupoPrograma {

    @Id
    @Column(name = "GrpCod", nullable = false)
    private Integer id;

    @Column(name = "GrpNom", length = 50)
    private String nome;

    public GrupoPrograma() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
}
