package br.gov.mandaguari.saude.especialidade;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Full-stack integration tests for /api/especialidades — HTTP → service → JPA → PostgreSQL
 * (Testcontainers). One test per endpoint, plus security (401/403) and the key business rules.
 */
class EspecialidadeControllerIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        // synthetic data only — no production PHI (specialty catalog has none anyway)
        jdbc.update("DELETE FROM SAU_PROESP");
        jdbc.update("DELETE FROM SAU_ESP");
        jdbc.update("DELETE FROM SAU_CBOR");
        jdbc.update("INSERT INTO SAU_CBOR (CborCod, CborDes) VALUES (225125, 'Médico cardiologista')");
        jdbc.update("INSERT INTO SAU_ESP (EspCod, EspNom, EspSit, EspCborCod) VALUES (1, 'Cardiologia', 'A', 225125)");
        jdbc.update("INSERT INTO SAU_ESP (EspCod, EspNom) VALUES (2, 'Pediatria')");
    }

    @Test
    void requiresAuthentication() {
        given().spec(anonymous()).when().get("/api/especialidades").then().statusCode(401);
    }

    @Test
    void forbidsWrongRole() {
        given().spec(asUser("OUTRA_ROLE")).when().get("/api/especialidades").then().statusCode(403);
    }

    @Test
    void listsAndSearches() {
        given().spec(asUser("SAUDE_CADASTRO"))
            .when().get("/api/especialidades?nome=cardio")
            .then().statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].nome", equalTo("Cardiologia"))
                .body("content[0].cborDescricao", equalTo("Médico cardiologista")); // R3
    }

    @Test
    void getsByIdAnd404() {
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/especialidades/1")
            .then().statusCode(200).body("nome", equalTo("Cardiologia"));
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/especialidades/9999")
            .then().statusCode(404).body("title", notNullValue());
    }

    @Test
    void createsValid() {
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("codigo", 10, "nome", "Dermatologia", "situacao", "A"))
            .when().post("/api/especialidades")
            .then().statusCode(201)
                .header("Location", containsString("/api/especialidades/10"))
                .body("nome", equalTo("Dermatologia"));
    }

    @Test
    void rejectsDuplicateCodigo() { // R2
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("codigo", 1, "nome", "Duplicada"))
            .when().post("/api/especialidades")
            .then().statusCode(409);
    }

    @Test
    void rejectsBlankNome() { // R1 (bean validation → 400)
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("codigo", 11, "nome", ""))
            .when().post("/api/especialidades")
            .then().statusCode(400).body("errors.nome", notNullValue());
    }

    @Test
    void rejectsUnknownCbor() { // R3
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("codigo", 12, "nome", "Neuro", "cborCodigo", 999999))
            .when().post("/api/especialidades")
            .then().statusCode(422).body("code", equalTo("esp.cbor.unknown"));
    }

    @Test
    void updatesName() {
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("nome", "Pediatria Geral"))
            .when().put("/api/especialidades/2")
            .then().statusCode(200).body("nome", equalTo("Pediatria Geral"));
    }

    @Test
    void deletesUnused() {
        given().spec(asUser("SAUDE_CADASTRO")).when().delete("/api/especialidades/2")
            .then().statusCode(204);
    }

    @Test
    void blocksDeleteWhenReferenced() { // R4
        jdbc.update("INSERT INTO SAU_PROESP (ProCod, EspCod) VALUES (500, 1)");
        given().spec(asUser("SAUDE_CADASTRO")).when().delete("/api/especialidades/1")
            .then().statusCode(409);
    }

    @Test
    void lookupReturnsItems() {
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/especialidades/lookup?q=ped")
            .then().statusCode(200).body("size()", greaterThanOrEqualTo(1))
                .body("[0].nome", notNullValue());
    }
}
