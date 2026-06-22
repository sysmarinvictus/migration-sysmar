package br.gov.mandaguari.saude.profissional.repository;

import java.time.LocalDate;

/**
 * Read projection over the un-migrated SYS_PES supertype (person fields the SAU_PRO transaction
 * displays). Spring Data maps native-query columns to these accessors by alias. PHI — never logged.
 */
public interface PersonProjection {
    String getNome();
    String getCpfCnpj();
    String getRgIe();
    String getSexo();
    LocalDate getDataNascimento();
    String getEndereco();
    String getEnderecoNumero();
    String getEnderecoComplemento();
    String getCep();
    Integer getBairroCod();
    Integer getMunicipioCod();
    String getTelefone();
    String getCelular();
}
