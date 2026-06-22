package br.gov.mandaguari.saude.medicamento.repository;

/** Projection of SAU_RENAME display columns — used by R12 (derived RENAMEDescr). */
public interface RenameDisplay {
    String getPrincipioAtivo();
    String getConcentracao();
    String getFormaFarmaceutica();
    String getVolume();
    String getUnidade();
}
