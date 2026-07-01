package br.gov.mandaguari.saude.receituarioespecial.repository;

import java.time.LocalDate;

/** Grid projection for SAU_RECESP joined to the patient name (SYS_PES via SAU_PAC). */
public interface ReceituarioEspecialListProjection {
    Integer getUnidadeCodigo();
    Long getNumero();
    LocalDate getData();
    Long getPacienteCodigo();
    String getPacienteNome();
    Long getPrescritorCodigo();
}
