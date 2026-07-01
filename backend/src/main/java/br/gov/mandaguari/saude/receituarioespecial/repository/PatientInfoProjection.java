package br.gov.mandaguari.saude.receituarioespecial.repository;

import java.time.LocalDate;

/** Patient snapshot used by the prescription rules/derivations: status (R12), CNS presence (R13), name+birth (R15/R16). */
public interface PatientInfoProjection {
    Integer getSituacao();          // SAU_PAC.PacSit — 1=ativo, 2=inativo (R12)
    String getCns();                // SYS_PES.PesNumCns (R13)
    String getNome();               // SYS_PES.PesNom
    String getNomeSocial();         // SYS_PES.PesNomSoc (R16)
    Boolean getUsaNomeSocial();     // SYS_PES.PesUsaNomSoc (R16)
    LocalDate getDataNascimento();  // SYS_PES.PesNasDat (R15)
}
