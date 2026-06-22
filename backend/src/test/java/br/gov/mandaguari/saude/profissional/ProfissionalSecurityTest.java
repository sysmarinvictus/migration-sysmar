package br.gov.mandaguari.saude.profissional;

import br.gov.mandaguari.saude.profissional.domain.CertificadoSenhaCryptoConverter;
import br.gov.mandaguari.saude.profissional.domain.Profissional;
import br.gov.mandaguari.saude.profissional.dto.ProfissionalDtos.ProfissionalResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Security-critical tests for the certificate password at rest and for response-body redaction
 * (SLICE-SPEC SAU_PRO §Security, R31). The legacy app stored {@code ProCertificadoSenha} in plaintext
 * and even logged it — that defect is NOT ported; these tests pin the modern guarantees:
 * <ul>
 *   <li>the converter encrypts (AES-GCM, {@code v1:iv:ct}), is fail-closed without a key, passes null
 *       through, tolerates legacy plaintext on read, and round-trips;</li>
 *   <li>no JSON DTO ever carries certificadoSenha / certificado / assinaturaImagem.</li>
 * </ul>
 */
class ProfissionalSecurityTest {

    /** A 32-byte (AES-256) test key, Base64 — test-only, never a production secret. */
    static final String TEST_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    /** JSR-310-aware mapper, matching the Spring Boot auto-configured ObjectMapper. */
    static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private CertificadoSenhaCryptoConverter converter(String key) {
        return new CertificadoSenhaCryptoConverter(key);
    }

    @Test
    void roundTripsEncryptThenDecrypt() {
        var c = converter(TEST_KEY);
        String stored = c.convertToDatabaseColumn("S3nh@-Cert!");
        assertThat(c.convertToEntityAttribute(stored)).isEqualTo("S3nh@-Cert!");
    }

    @Test
    void storedValueIsCiphertextNotCleartextAndCarriesV1Envelope() {
        var c = converter(TEST_KEY);
        String stored = c.convertToDatabaseColumn("S3nh@-Cert!");
        assertThat(stored).startsWith("v1:");
        // format v1:<base64(iv)>:<base64(ct)>
        assertThat(stored.split(":")).hasSize(3);
        assertThat(stored).doesNotContain("S3nh@-Cert!");
        // non-deterministic IV => two encryptions differ
        assertThat(c.convertToDatabaseColumn("S3nh@-Cert!")).isNotEqualTo(stored);
    }

    @Test
    void failClosedWhenKeyBlankAndValueNonNull() {
        var c = converter("   ");
        assertThatThrownBy(() -> c.convertToDatabaseColumn("anything"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cleartext");
    }

    @Test
    void nullPassesThroughEvenWithoutKey() {
        assertThat(converter("").convertToDatabaseColumn(null)).isNull();
        assertThat(converter("").convertToEntityAttribute(null)).isNull();
    }

    @Test
    void tolaratesLegacyPlaintextOnReadWithoutKey() {
        // A row written by the legacy app (no v1: prefix) is returned as-is on read (migration window).
        var c = converter("");
        assertThat(c.convertToEntityAttribute("legacyClearPassword")).isEqualTo("legacyClearPassword");
    }

    @Test
    void responseDtoNeverSerializesSecretOrBlobFields() throws Exception {
        ProfissionalResponse resp = new ProfissionalResponse(
                100L, "700000000000021", "12345", "PR",
                (short) 1, "Conselho Regional de Medicina", "CRM",
                LocalDate.of(2020, 1, 1), null, "CNES1", Boolean.FALSE, (short) 0, (short) 1,
                "Maria Sintetica", "01111111294", "RG1", "F", LocalDate.of(1990, 1, 1),
                "Rua X", "(44) 3232-3232", "(44) 99999-8888");

        String json = MAPPER.writeValueAsString(resp);

        // The response DTO declares none of the sensitive fields at all.
        assertThat(json)
                .doesNotContain("certificadoSenha")
                .doesNotContain("certificado")
                .doesNotContain("assinatura");
    }

    @Test
    void entitySerializationOmitsSecretAndBlobFields() throws Exception {
        // Even if the entity itself is ever serialized, @JsonIgnore must keep the sensitive columns out.
        Profissional p = new Profissional();
        p.setId(100L);
        p.setSituacao((short) 1);
        p.setCertificadoSenha("topsecret");
        p.setCertificado(new byte[]{1, 2, 3});
        p.setAssinaturaImagem(new byte[]{4, 5, 6});

        String json = MAPPER.writeValueAsString(p);

        // @JsonIgnore keeps the secret value and the secret/blob KEYS out. (assinaturaImagemTipo is a
        // non-sensitive content-type field and may legitimately appear — assert on the exact keys.)
        assertThat(json)
                .doesNotContain("topsecret")
                .doesNotContain("\"certificadoSenha\"")
                .doesNotContain("\"certificado\"")
                .doesNotContain("\"assinaturaImagem\"");
    }
}
