package br.gov.mandaguari.saude.conselhoclasse;

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
 * Full-stack integration tests for /api/conselhos-classe — HTTP → service → JPA → PostgreSQL
 * (Testcontainers). One test per endpoint, plus security (401/403) and the mined business rules.
 */
class ConselhoClasseControllerIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        // synthetic reference data only — no PHI in this slice
        jdbc.update("DELETE FROM SAU_PRO");
        jdbc.update("DELETE FROM SAU_CONCLA");
        jdbc.update("INSERT INTO SAU_CONCLA (ConClaCod, ConClaSigra, ConClaNom) VALUES (1, 'CRM', 'Conselho Regional de Medicina')");
        jdbc.update("INSERT INTO SAU_CONCLA (ConClaCod, ConClaSigra, ConClaNom) VALUES (2, 'COREN', 'Conselho Regional de Enfermagem')");
        jdbc.update("INSERT INTO SAU_CONCLA (ConClaCod) VALUES (3)"); // R5: sigla/nome null allowed
    }

    @Test
    void requiresAuthentication() {
        given().spec(anonymous()).when().get("/api/conselhos-classe").then().statusCode(401);
    }

    @Test
    void forbidsWrongRole() {
        given().spec(asUser("OUTRA_ROLE")).when().get("/api/conselhos-classe").then().statusCode(403);
    }

    @Test
    void listsAndSearches() {
        given().spec(asUser("SAUDE_CADASTRO"))
            .when().get("/api/conselhos-classe?q=CRM")
            .then().statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].sigla", equalTo("CRM"));
    }

    @Test
    void getsByIdAnd404() {
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/conselhos-classe/1")
            .then().statusCode(200).body("sigla", equalTo("CRM"));
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/conselhos-classe/9999")
            .then().statusCode(404).body("title", notNullValue());
    }

    @Test
    void createsValid() {
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("codigo", 10, "sigla", "CRF", "nome", "Conselho Regional de Farmácia"))
            .when().post("/api/conselhos-classe")
            .then().statusCode(201)
                .header("Location", containsString("/api/conselhos-classe/10"))
                .body("sigla", equalTo("CRF"));
    }

    @Test
    void createsWithNullSiglaAndNome() { // R5
        var body = new HashMap<String, Object>();
        body.put("codigo", 11);
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(body)
            .when().post("/api/conselhos-classe")
            .then().statusCode(201).body("codigo", equalTo(11));
    }

    @Test
    void rejectsDuplicateCodigo() { // R2
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("codigo", 1, "sigla", "DUP"))
            .when().post("/api/conselhos-classe")
            .then().statusCode(409);
    }

    @Test
    void rejectsCodigoOutOfRange() { // R1 (bean validation @Max(999) → 400)
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("codigo", 1000, "sigla", "X"))
            .when().post("/api/conselhos-classe")
            .then().statusCode(400).body("errors.codigo", notNullValue());
    }

    @Test
    void updatesSiglaAndNome() {
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("sigla", "COREN-PR", "nome", "Conselho Regional de Enfermagem do Paraná"))
            .when().put("/api/conselhos-classe/2")
            .then().statusCode(200).body("sigla", equalTo("COREN-PR"));
    }

    @Test
    void deletesUnused() {
        given().spec(asUser("SAUDE_CADASTRO")).when().delete("/api/conselhos-classe/3")
            .then().statusCode(204);
    }

    @Test
    void blocksDeleteWhenReferencedByProfissional() { // R3
        jdbc.update("INSERT INTO SAU_PRO (ProCod, ConClaCod) VALUES (500, 1)");
        given().spec(asUser("SAUDE_CADASTRO")).when().delete("/api/conselhos-classe/1")
            .then().statusCode(409);
    }

    @Test
    void lookupReturnsItems() {
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/conselhos-classe/lookup?q=cr")
            .then().statusCode(200).body("size()", greaterThanOrEqualTo(1))
                .body("[0].sigla", notNullValue());
    }
}
