package br.gov.mandaguari.saude.paciente.dto;

import java.time.LocalDate;

/**
 * DTOs for Paciente (SAU_PAC person-subtype). Requests/responses carry the person fields (SYS_PES) plus
 * the patient-specific fields (SAU_PAC). Derived/audit columns (soundex, usuLogin, datAlt, cadUniCod) are
 * system-set and NOT accepted from requests. This is PHI-dense — responses are SAUDE_CADASTRO-only + audited.
 */
public final class PacienteDtos {
    private PacienteDtos() {}

    /** Grid/lookup row. */
    public record PacienteListItem(
            Long id, String nome, String nomeMae, String prontuario,
            String cpfCnpj, String cns, LocalDate dataNascimento, Integer situacao, Integer obito) {}

    /** Full patient detail: person (SYS_PES) + patient (SAU_PAC). */
    public record PacienteResponse(
            Long id,
            // person
            String nome, String nomeSocial, boolean usaNomeSocial, String nomeMae, String nomePai,
            String cpfCnpj, String cns, String rg, LocalDate dataNascimento, String sexo,
            String cep, String endereco, String numero, String complemento,
            Integer bairroCod, Integer municipioCod, String telefone, String celular, String email,
            // patient (SAU_PAC)
            Integer unidadeCod, String prontuario, Long numeroIdentificacao,
            String alergia, String historicoDoencas, Integer obito, Boolean inconsciente,
            Boolean situacaoRua, Boolean surtoPsiquiatrico, Integer rendaFamiliar, String meioTransporte,
            Boolean beneficioSocial, String cnh, Integer situacao) {}

    /** Create/update payload (person + patient). id is generated (create) / path (update). */
    public record PacienteWriteRequest(
            // person
            String nome, String nomeSocial, Boolean usaNomeSocial, String nomeMae, String nomePai,
            String cpfCnpj, String cns, String rg, LocalDate dataNascimento, String sexo,
            String cep, String endereco, String numero, String complemento,
            Integer bairroCod, Integer municipioCod, String telefone, String celular, String email,
            Integer etniaCod, Integer paisCod, String cboCod,
            // patient (SAU_PAC)
            Integer unidadeCod, String prontuario, Long numeroIdentificacao,
            String alergia, String historicoDoencas, Integer obito, Boolean inconsciente,
            Boolean situacaoRua, Boolean surtoPsiquiatrico, Integer rendaFamiliar, String meioTransporte,
            Boolean beneficioSocial, String cnh, Integer situacao) {}
}
