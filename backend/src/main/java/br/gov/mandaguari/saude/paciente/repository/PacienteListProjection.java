package br.gov.mandaguari.saude.paciente.repository;

import java.time.LocalDate;

/** Grid/lookup projection joining SAU_PAC + SYS_PES (name/mother/CPF/CNS + prontuário/situação/óbito). */
public interface PacienteListProjection {
    Long getId();
    String getNome();
    String getNomeMae();
    String getProntuario();
    String getCpfCnpj();
    String getCns();
    LocalDate getDataNascimento();
    Integer getSituacao();
    Integer getObito();
}
