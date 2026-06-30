package br.gov.mandaguari.saude.funcionario.repository;

/**
 * Read projection over the un-migrated SYS_PES supertype — the person fields the SAU_FUN transaction
 * displays/edits (simplified write-back scope: name/CPF/phones, like SAU_PRO; full address panel is
 * deferred to the SYS_PES slice). Spring Data maps native-query column aliases to these accessors.
 * <b>PHI — never logged.</b>
 */
public interface PersonProjection {
    String getNome();
    String getCpfCnpj();
    String getTelefone();
    String getCelular();
}
