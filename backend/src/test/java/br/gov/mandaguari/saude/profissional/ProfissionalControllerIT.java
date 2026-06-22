package br.gov.mandaguari.saude.profissional;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Full-stack integration tests for /api/profissionais — HTTP → service → JPA → PostgreSQL
 * (Testcontainers, shared container via AbstractIntegrationTest). One test per endpoint plus the
 * highest-value rule coverage (R1/R3/R4/R5/R10 inserts, R2 write-back, R16 name+date filters,
 * R20/R26 delete-guards, R31 secret redaction).
 *
 * <p>Seed values are synthetic and NON-PHI. CNS/CPF are generated to pass the mod-11 validators
 * (700000000000021 / 01111111294). The signing-certificate password / blobs are never set via the API.
 */
class ProfissionalControllerIT extends AbstractIntegrationTest {

    static final String VALID_CNS = "700000000000021";
    static final String VALID_CNS_2 = "700000000000056";
    static final String VALID_CPF = "01111111294";
    static final long PES = 100L;
    static final long PES_2 = 200L;

    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        cleanAll();
        // conselho de classe (CRM)
        jdbc.update("INSERT INTO SAU_CONCLA (ConClaCod, ConClaSigra, ConClaNom) VALUES (1, 'CRM', 'Conselho Regional de Medicina')");
        // person (SYS_PES) — the SAU_PRO PK IS the person code
        jdbc.update("INSERT INTO SYS_PES (PesCod, PesNom, PesCPFCNPJ, PesFon, PesCel) "
                + "VALUES (100, 'Dr. Silva Sintetico', '01111111294', '(44) 3232-3232', '(44) 99999-8888')");
        // a second person available to become a professional (POST happy-path target)
        jdbc.update("INSERT INTO SYS_PES (PesCod, PesNom) VALUES (200, 'Dra. Souza Sintetica')");
        // professional row (seeded senha is legacy plaintext — converter tolerates on read)
        jdbc.update("INSERT INTO SAU_PRO (ProPesCod, ProPesNumCns, ProSit, ProExt, ConClaCod, ProUfConselho, ProCertificadoSenha) "
                + "VALUES (100, '700000000000021', 1, 0, 1, 'PR', 'legacy-plaintext-senha')");
    }

    void cleanAll() {
        jdbc.update("DELETE FROM SAU_PROESP");
        jdbc.update("DELETE FROM SAU_RECESP");   // child of SAU_UNI; drop before unidade
        jdbc.update("UPDATE SAU_UNI SET UniProPesAutCod = NULL, UniProPesAudCod = NULL, UniProPesDirCod = NULL, UniProPesRespCod = NULL");
        jdbc.update("UPDATE SAU_USU SET UsuProPesCod = NULL");
        jdbc.update("DELETE FROM SAU_PRO");
        jdbc.update("DELETE FROM SYS_PES");
        jdbc.update("DELETE FROM SAU_CONCLA");
    }

    private Map<String, Object> validCreateBody(long id, String cns) {
        Map<String, Object> body = new HashMap<>();
        body.put("id", id);
        body.put("numeroCns", cns);
        body.put("ufConselho", "PR");
        body.put("conselhoClasseCod", 1);
        Map<String, Object> pessoa = new HashMap<>();
        pessoa.put("nome", "Dra. Souza Sintetica");
        body.put("pessoa", pessoa);
        return body;
    }

    // ── Security ──────────────────────────────────────────────────────────────────────────────────

    @Test
    void requiresAuthentication() { // R27
        given().spec(anonymous()).when().get("/api/profissionais").then().statusCode(401);
    }

    @Test
    void forbidsWrongRole() { // R28
        given().spec(asUser("OUTRA_ROLE")).when().get("/api/profissionais").then().statusCode(403);
    }

    // ── List / search ─────────────────────────────────────────────────────────────────────────────

    @Test
    void listsPaginated() {
        given().spec(asUser("SAUDE_CADASTRO"))
            .when().get("/api/profissionais")
            .then().statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].id", equalTo(100))
                .body("content[0].nome", equalTo("Dr. Silva Sintetico"))
                .body("content[0].conselhoClasseSigla", equalTo("CRM"));
    }

    @Test
    void filtersByNameSubstring() { // R16: PesNom LIKE %?%
        given().spec(asUser("SAUDE_CADASTRO"))
            .queryParam("nome", "Silva")
            .when().get("/api/profissionais")
            .then().statusCode(200).body("content", hasSize(1)).body("content[0].id", equalTo(100));
        given().spec(asUser("SAUDE_CADASTRO"))
            .queryParam("nome", "Inexistente")
            .when().get("/api/profissionais")
            .then().statusCode(200).body("content", hasSize(0));
    }

    @Test
    void filtersByCnsAndSituacaoBounds() {
        // Regression for PG 42P18: the native query CASTs every nullable filter param, including the
        // SMALLINT situacao/externo bounds and the VARCHAR cns. Supplying them must not 500.
        given().spec(asUser("SAUDE_CADASTRO"))
            .queryParam("numeroCns", VALID_CNS).queryParam("situacao", 1).queryParam("externo", 0)
            .when().get("/api/profissionais")
            .then().statusCode(200).body("content", hasSize(1)).body("content[0].id", equalTo(100));
        // a non-matching situacao excludes it (proves the predicate is applied, not ignored)
        given().spec(asUser("SAUDE_CADASTRO"))
            .queryParam("situacao", 2)
            .when().get("/api/profissionais")
            .then().statusCode(200).body("content", hasSize(0));
    }

    // ── Get by id ─────────────────────────────────────────────────────────────────────────────────

    @Test
    void getsByIdAndNeverReturnsSecretFields() {
        String json = given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/profissionais/100")
            .then().statusCode(200)
                .body("id", equalTo(100))
                // ProPesNumCns is CHAR(20) → space-padded; legacy-faithful (no app-side trim).
                .body("numeroCns", startsWith(VALID_CNS))
                .body("nome", equalTo("Dr. Silva Sintetico"))
                .body("conselhoClasseSigla", equalTo("CRM"))
                .extract().asString();
        org.assertj.core.api.Assertions.assertThat(json)
                .doesNotContain("certificadoSenha")
                .doesNotContain("certificado")
                .doesNotContain("assinatura")
                .doesNotContain("legacy-plaintext-senha");
    }

    @Test
    void getReturns404WhenAbsent() {
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/profissionais/9999")
            .then().statusCode(404).body("title", notNullValue());
    }

    // ── Create ────────────────────────────────────────────────────────────────────────────────────

    @Test
    void createsValid() {
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(validCreateBody(PES_2, VALID_CNS_2))
            .when().post("/api/profissionais")
            .then().statusCode(201)
                .header("Location", containsString("/api/profissionais/200"))
                .body("id", equalTo(200))
                .body("situacao", equalTo(1))      // R12 default ATIVO
                .body("externo", equalTo(0))       // R13 default 0
                .body("exportaEsus", equalTo(false)); // R13 default false
    }

    @Test
    void rejectsCreateWhenPersonMissing() { // R1
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(validCreateBody(999L, VALID_CNS_2))
            .when().post("/api/profissionais")
            .then().statusCode(422).body("code", equalTo("pro.pessoa.notfound"));
    }

    @Test
    void rejectsMissingCns() { // R3 — @NotNull bean validation → 400
        Map<String, Object> body = validCreateBody(PES_2, null);
        body.remove("numeroCns");
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(body)
            .when().post("/api/profissionais")
            .then().statusCode(400).body("errors.numeroCns", notNullValue());
    }

    @Test
    void rejectsInvalidCns() { // R4 — @CNS bean validation → 400
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(validCreateBody(PES_2, "700000000000001"))
            .when().post("/api/profissionais")
            .then().statusCode(400).body("errors.numeroCns", notNullValue());
    }

    @Test
    void rejectsDuplicateCns() { // R5 — CNS already used by person 100
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(validCreateBody(PES_2, VALID_CNS))
            .when().post("/api/profissionais")
            .then().statusCode(422).body("code", equalTo("pro.cns.duplicate"));
    }

    @Test
    void rejectsUnknownConselho() { // R10 — conselhoClasseCod != 0 must exist
        Map<String, Object> body = validCreateBody(PES_2, VALID_CNS_2);
        body.put("conselhoClasseCod", 99);
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(body)
            .when().post("/api/profissionais")
            .then().statusCode(422).body("code", equalTo("pro.conselho.notfound"));
    }

    // ── Update ────────────────────────────────────────────────────────────────────────────────────

    @Test
    void updatesAndWritesBackPerson() { // R2
        Map<String, Object> body = new HashMap<>();
        body.put("numeroCns", VALID_CNS);
        body.put("ufConselho", "SP");
        body.put("conselhoClasseCod", 1);
        body.put("situacao", 2);
        Map<String, Object> pessoa = new HashMap<>();
        pessoa.put("nome", "Dr. Silva Renomeado");
        pessoa.put("cpfCnpj", VALID_CPF);
        pessoa.put("telefone", "(44) 3000-0000");
        pessoa.put("celular", "(44) 98888-7777");
        body.put("pessoa", pessoa);

        given().spec(asUser("SAUDE_CADASTRO"))
            .body(body)
            .when().put("/api/profissionais/100")
            .then().statusCode(200)
                .body("situacao", equalTo(2))
                .body("nome", equalTo("Dr. Silva Renomeado")); // R2 write-back reflected via SYS_PES join

        // SYS_PES was actually mutated
        String nome = jdbc.queryForObject("SELECT PesNom FROM SYS_PES WHERE PesCod = 100", String.class);
        org.assertj.core.api.Assertions.assertThat(nome).isEqualTo("Dr. Silva Renomeado");
    }

    // ── Delete ────────────────────────────────────────────────────────────────────────────────────

    @Test
    void deleteBlockedWhenReferencedBySpecialty() { // R19/R20
        jdbc.update("INSERT INTO SAU_PROESP (ProPesCod, EspCod) VALUES (100, 1)");
        given().spec(asUser("SAUDE_CADASTRO"))
            .when().delete("/api/profissionais/100")
            .then().statusCode(422).body("code", equalTo("pro.delete.referenced"));
    }

    @Test
    void deleteBlockedWhenHasControlledPrescription() { // R26 — Portaria 344/98
        // SAU_RECESP.RecEspUniCod is FK→SAU_UNI; seed a unidade then a controlled-prescription row.
        jdbc.update("INSERT INTO SAU_UNI (UniCod) VALUES (1) ON CONFLICT DO NOTHING");
        jdbc.update("INSERT INTO SAU_RECESP (RecEspUniCod, RecEspCod, RecEspProPesCod) VALUES (1, 1, 100)");
        given().spec(asUser("SAUDE_CADASTRO"))
            .when().delete("/api/profissionais/100")
            .then().statusCode(422).body("code", equalTo("pro.delete.referenced"));
    }

    @Test
    void deletesWhenUnreferenced() { // R19 — no guard matches
        given().spec(asUser("SAUDE_CADASTRO"))
            .when().delete("/api/profissionais/100")
            .then().statusCode(204);
        given().spec(asUser("SAUDE_CADASTRO"))
            .when().get("/api/profissionais/100")
            .then().statusCode(404);
    }

    @Test
    void deleteRequiresAuth() {
        given().spec(anonymous()).when().delete("/api/profissionais/100").then().statusCode(401);
    }

    // ── Lookup ────────────────────────────────────────────────────────────────────────────────────

    @Test
    void lookupReturnsPrescriberItems() {
        given().spec(asUser("SAUDE_CADASTRO"))
            .queryParam("q", "Silva")
            .when().get("/api/profissionais/lookup")
            .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].id", equalTo(100))
                .body("[0].conselhoClasseSigla", equalTo("CRM"));
    }
}
