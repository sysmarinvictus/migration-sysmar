package br.gov.mandaguari.saude.impedimento.domain;

import br.gov.mandaguari.saude.especialidade.domain.Especialidade;
import jakarta.persistence.*;
import java.time.LocalDate;

/** Impedimento do profissional — maps SAU_IMP (GeneXus physical names pinned via @Column). */
@Entity
@Table(name = "SAU_IMP")
public class Impedimento {

    @Id
    @Column(name = "ImpCod", nullable = false)
    private Integer codigo;

    @Column(name = "ImpDat")
    private LocalDate dataCadastro;

    @Column(name = "ImpDatIni")
    private LocalDate dataInicio;

    @Column(name = "ImpDatFim")
    private LocalDate dataFim;

    /** Raw FK to SAU_PRO (propescod BIGINT). No @ManyToOne — SAU_PRO not yet migrated. */
    @Column(name = "ProPesCod")
    private Long profissionalCodigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EspCod")
    private Especialidade especialidade;

    public Impedimento() {}

    public Integer getCodigo() { return codigo; }
    public void setCodigo(Integer codigo) { this.codigo = codigo; }
    public LocalDate getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(LocalDate dataCadastro) { this.dataCadastro = dataCadastro; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }
    public Long getProfissionalCodigo() { return profissionalCodigo; }
    public void setProfissionalCodigo(Long profissionalCodigo) { this.profissionalCodigo = profissionalCodigo; }
    public Especialidade getEspecialidade() { return especialidade; }
    public void setEspecialidade(Especialidade especialidade) { this.especialidade = especialidade; }
}
