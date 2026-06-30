package br.gov.mandaguari.saude.seguranca;

import br.gov.mandaguari.saude.seguranca.domain.Usuario;
import br.gov.mandaguari.saude.seguranca.dto.UsuarioDtos.UsuarioResponse;
import br.gov.mandaguari.saude.seguranca.mapper.UsuarioMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Security-critical tests for the SAU_USU password secrets at rest in transit (SLICE-SPEC SAU_USU
 * §auth). The legacy app stored {@code UsuSen} as REVERSIBLE ciphertext with a per-user {@code UsuKey};
 * the modern app must never serialize either. These pin the guarantees:
 * <ul>
 *   <li>{@code @JsonIgnore} keeps {@code senha}/{@code chaveSenha} out of any entity serialization;</li>
 *   <li>no read/lookup DTO declares a password field, so the mapper output cannot leak one;</li>
 *   <li>{@code toString()} omits secrets and PII (UsuNom).</li>
 * </ul>
 */
class UsuarioSecurityTest {

    static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    final UsuarioMapper mapper = Mappers.getMapper(UsuarioMapper.class);

    private Usuario withSecrets() {
        Usuario u = new Usuario();
        u.setUsuCod(100);
        u.setLogin("joao");
        u.setNome("Maria Sintetica");
        u.setSenha("$2a$10$THIS_SHOULD_NEVER_APPEAR_IN_JSON");
        u.setChaveSenha("perUserSymmetricKey1234567890ab");
        u.setBloqueado((short) 0);
        return u;
    }

    @Test
    void entitySerializationOmitsPasswordAndKey() throws Exception {
        String json = MAPPER.writeValueAsString(withSecrets());
        assertThat(json)
                .doesNotContain("$2a$10$THIS_SHOULD_NEVER_APPEAR_IN_JSON")
                .doesNotContain("perUserSymmetricKey1234567890ab")
                .doesNotContain("\"senha\"")
                .doesNotContain("\"chaveSenha\"");
    }

    @Test
    void responseDtoNeverCarriesSecrets() throws Exception {
        UsuarioResponse resp = mapper.toResponse(withSecrets());
        String json = MAPPER.writeValueAsString(resp);
        assertThat(json)
                .doesNotContain("$2a$10$THIS_SHOULD_NEVER_APPEAR_IN_JSON")
                .doesNotContain("perUserSymmetricKey1234567890ab")
                .doesNotContain("\"senha\"")
                .doesNotContain("\"chaveSenha\"");
        // sanity: it still projects the non-secret fields
        assertThat(json).contains("\"login\"").contains("\"usuCod\"");
    }

    @Test
    void toStringOmitsSecretsAndPii() {
        String s = withSecrets().toString();
        assertThat(s)
                .doesNotContain("$2a$10$THIS_SHOULD_NEVER_APPEAR_IN_JSON")
                .doesNotContain("perUserSymmetricKey1234567890ab")
                .doesNotContain("Maria Sintetica");   // PII (UsuNom) excluded
        assertThat(s).contains("usuCod=100");
    }
}
