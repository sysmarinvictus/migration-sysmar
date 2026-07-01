package br.gov.mandaguari.saude.profissionalexterno;

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
 * Full-stack tests for SAU_PESF_PROFEXT (POST /api/profissionais-externos) — HTTP → service → JPA →
 * PostgreSQL (Testcontainers). Confirms the composite create writes BOTH a SYS_PES person (PesTip=1)
 * and a SAU_PRO external professional (ProExt=1). Synthetic data only (person supertype is PHI).
 */
class ProfissionalExternoControllerIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    static final String CNS_OK = "700000000000005";
    static final String CNS_OK2 = "700000000000013";

    @BeforeEach
    void seed() {
        clean();
        jdbc.update("INSERT INTO SYS_MUN (MunCod, MunNom) VALUES (411420, 'Mandaguari') ON CONFLICT (MunCod) DO NOTHING");
        jdbc.update("INSERT INTO SAU_CONCLA (ConClaCod, ConClaNom) VALUES (1, 'CRM') ON CONFLICT (ConClaCod) DO NOTHING");
    }

    void clean() {
        jdbc.update("DELETE FROM SAU_PRO");
        jdbc.update("DELETE FROM SYS_PES");
    }

    private Map<String, Object> validBody() {
        Map<String, Object> b = new HashMap<>();
        b.put("nome", "Maria Silva");
        b.put("cns", CNS_OK);
        b.put("municipioCod", 411420);
        b.put("conselhoClasseCod", 1);
        b.put("numeroConselho", "12345");
        return b;
    }

    @Test
    void requiresAuthentication() {
        given().spec(anonymous()).body(validBody()).when().post("/api/profissionais-externos").then().statusCode(401);
    }

    @Test
    void forbidsWrongRole() {
        given().spec(asUser("OUTRA_ROLE")).body(validBody()).when().post("/api/profissionais-externos").then().statusCode(403);
    }

    @Test
    void createsPersonAndExternalProfessional() {
        int id = given().spec(asUser("SAUDE_CADASTRO")).body(validBody())
            .when().post("/api/profissionais-externos")
            .then().statusCode(201)
                .header("Location", containsString("/api/profissionais-externos/1"))
                .body("id", equalTo(1))
                .body("externo", equalTo(1))
                .body("situacao", equalTo(1))
                .body("numeroConselho", equalTo("12345"))
            .extract().path("id");

        // SYS_PES person created with PesTip=1 and uppercased name
        Map<String, Object> pes = jdbc.queryForMap("SELECT PesTip, PesNom, PesMunCod FROM SYS_PES WHERE PesCod = ?", (long) id);
        org.assertj.core.api.Assertions.assertThat(pes.get("pestip")).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(pes.get("pesnom")).isEqualTo("MARIA SILVA");
        // SAU_PRO external professional created (ProExt=1, no certificate)
        Map<String, Object> pro = jdbc.queryForMap("SELECT ProExt, ProSit, ConClaCod, ProNumCr FROM SAU_PRO WHERE ProPesCod = ?", (long) id);
        org.assertj.core.api.Assertions.assertThat(pro.get("proext")).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(pro.get("prosit")).isEqualTo(1);
    }

    @Test
    void rejectsInvalidCns() { // R16
        Map<String, Object> b = validBody();
        b.put("cns", "123456789012345");
        given().spec(asUser("SAUDE_CADASTRO")).body(b)
            .when().post("/api/profissionais-externos")
            .then().statusCode(422).body("code", equalTo("profext.cns.invalid"));
    }

    @Test
    void rejectsMissingConselho() { // R21
        Map<String, Object> b = validBody();
        b.remove("conselhoClasseCod");
        given().spec(asUser("SAUDE_CADASTRO")).body(b)
            .when().post("/api/profissionais-externos")
            .then().statusCode(422).body("code", equalTo("profext.conselho.required"));
    }

    @Test
    void rejectsUnknownMunicipio() { // R19
        Map<String, Object> b = validBody();
        b.put("municipioCod", 999999);
        given().spec(asUser("SAUDE_CADASTRO")).body(b)
            .when().post("/api/profissionais-externos")
            .then().statusCode(422).body("code", equalTo("profext.municipio.notfound"));
    }

    @Test
    void rejectsDuplicateCns() { // R17 → 409
        given().spec(asUser("SAUDE_CADASTRO")).body(validBody())
            .when().post("/api/profissionais-externos").then().statusCode(201);

        Map<String, Object> b2 = validBody();
        b2.put("cns", CNS_OK); // same CNS
        given().spec(asUser("SAUDE_CADASTRO")).body(b2)
            .when().post("/api/profissionais-externos")
            .then().statusCode(409);
    }

    @Test
    void getsByIdAnd404() {
        int id = given().spec(asUser("SAUDE_CADASTRO")).body(validBody())
            .when().post("/api/profissionais-externos").then().statusCode(201).extract().path("id");

        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/profissionais-externos/" + id)
            .then().statusCode(200).body("externo", equalTo(1)).body("nome", equalTo("MARIA SILVA"));

        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/profissionais-externos/99999")
            .then().statusCode(404);
    }
}
