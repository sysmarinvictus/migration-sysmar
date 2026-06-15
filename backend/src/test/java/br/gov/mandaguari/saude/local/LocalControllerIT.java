package br.gov.mandaguari.saude.local;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Full-stack integration tests for /api/locais — HTTP → service → JPA → PostgreSQL (Testcontainers).
 * One test per endpoint, plus security (401/403) and the mined business rules.
 */
class LocalControllerIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM SAU_LOC");
        jdbc.update("DELETE FROM SYS_MUN");
        jdbc.update("INSERT INTO SYS_MUN (MunCod, MunNom, MunUF, MunIBGE) VALUES (4114402, 'Mandaguari', 'PR', '4114402')");
        jdbc.update("INSERT INTO SAU_LOC (LocCod, LocNom, LocMunCod) VALUES (1, 'Centro', 4114402)");
        jdbc.update("INSERT INTO SAU_LOC (LocCod, LocNom, LocMunCod) VALUES (2, 'Vila Nova', 4114402)");
    }

    @Test
    void requiresAuthentication() {
        given().spec(anonymous()).when().get("/api/locais").then().statusCode(401);
    }

    @Test
    void forbidsWrongRole() {
        given().spec(asUser("OUTRA_ROLE")).when().get("/api/locais").then().statusCode(403);
    }

    @Test
    void listsAndSearches() {
        given().spec(asUser("SAUDE_CADASTRO"))
            .when().get("/api/locais?nome=centro")
            .then().statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].nome", equalTo("Centro"))
                .body("content[0].municipioNome", equalTo("Mandaguari")); // R4
    }

    @Test
    void getsByIdAnd404() {
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/locais/1")
            .then().statusCode(200)
                .body("nome", equalTo("Centro"))
                .body("municipioUf", equalTo("PR"));   // R4 derived
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/locais/9999")
            .then().statusCode(404).body("title", notNullValue());
    }

    @Test
    void createsValid() {
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("codigo", 10, "nome", "Jardim", "municipioCodigo", 4114402))
            .when().post("/api/locais")
            .then().statusCode(201)
                .header("Location", containsString("/api/locais/10"))
                .body("municipioNome", equalTo("Mandaguari")); // R4
    }

    @Test
    void rejectsBlankNome() { // R2 (bean validation → 400)
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("codigo", 11, "nome", "", "municipioCodigo", 4114402))
            .when().post("/api/locais")
            .then().statusCode(400).body("errors.nome", notNullValue());
    }

    @Test
    void rejectsMissingMunicipio() { // R3 (municipioCodigo = 0 → 422)
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("codigo", 12, "nome", "SemMunicipio", "municipioCodigo", 0))
            .when().post("/api/locais")
            .then().statusCode(422).body("code", equalTo("loc.municipio.required"));
    }

    @Test
    void rejectsUnknownMunicipio() { // R4
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("codigo", 13, "nome", "Fantasma", "municipioCodigo", 999999))
            .when().post("/api/locais")
            .then().statusCode(422).body("code", equalTo("loc.municipio.unknown"));
    }

    @Test
    void rejectsDuplicateCodigo() { // R1
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("codigo", 1, "nome", "Duplicado", "municipioCodigo", 4114402))
            .when().post("/api/locais")
            .then().statusCode(409);
    }

    @Test
    void rejectsCodigoOutOfRange() { // R1 (bean validation @Max(999999) → 400)
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("codigo", 1_000_000, "nome", "Grande", "municipioCodigo", 4114402))
            .when().post("/api/locais")
            .then().statusCode(400).body("errors.codigo", notNullValue());
    }

    @Test
    void updatesNomeAndMunicipio() {
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("nome", "Vila Velha", "municipioCodigo", 4114402))
            .when().put("/api/locais/2")
            .then().statusCode(200).body("nome", equalTo("Vila Velha"));
    }

    @Test
    void deletesFreely() { // R5 — no delete guard
        given().spec(asUser("SAUDE_CADASTRO")).when().delete("/api/locais/2")
            .then().statusCode(204);
    }

    @Test
    void lookupReturnsItems() {
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/locais/lookup?q=vila")
            .then().statusCode(200).body("size()", greaterThanOrEqualTo(1))
                .body("[0].nome", notNullValue());
    }
}
