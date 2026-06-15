package br.gov.mandaguari.saude.tipomedicamento;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Full-stack integration tests for /api/tipos-medicamento — HTTP → service → JPA → PostgreSQL
 * (Testcontainers). One test per endpoint, plus security (401/403) and the mined business rules.
 */
class TipoMedicamentoControllerIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM SAU_REM");
        jdbc.update("DELETE FROM SAU_TIPREM");
        jdbc.update("INSERT INTO SAU_TIPREM (TipRemCod, TipRemDes) VALUES (1, 'Controlado')");
        jdbc.update("INSERT INTO SAU_TIPREM (TipRemCod, TipRemDes) VALUES (2, 'Genérico')");
    }

    @Test
    void requiresAuthentication() {
        given().spec(anonymous()).when().get("/api/tipos-medicamento").then().statusCode(401);
    }

    @Test
    void forbidsWrongRole() {
        given().spec(asUser("OUTRA_ROLE")).when().get("/api/tipos-medicamento").then().statusCode(403);
    }

    @Test
    void listsAndSearches() {
        given().spec(asUser("SAUDE_CADASTRO"))
            .when().get("/api/tipos-medicamento?descricao=control")
            .then().statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].descricao", equalTo("Controlado"));
    }

    @Test
    void getsByIdAnd404() {
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/tipos-medicamento/1")
            .then().statusCode(200).body("descricao", equalTo("Controlado"));
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/tipos-medicamento/9999")
            .then().statusCode(404).body("title", notNullValue());
    }

    @Test
    void createsValid() {
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("codigo", 10, "descricao", "Manipulado"))
            .when().post("/api/tipos-medicamento")
            .then().statusCode(201)
                .header("Location", containsString("/api/tipos-medicamento/10"))
                .body("descricao", equalTo("Manipulado"));
    }

    @Test
    void rejectsBlankDescricao() { // R2 (bean validation → 400)
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("codigo", 11, "descricao", ""))
            .when().post("/api/tipos-medicamento")
            .then().statusCode(400).body("errors.descricao", notNullValue());
    }

    @Test
    void rejectsDuplicateCodigo() { // R1
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("codigo", 1, "descricao", "Duplicado"))
            .when().post("/api/tipos-medicamento")
            .then().statusCode(409);
    }

    @Test
    void rejectsCodigoOutOfRange() { // R1 (bean validation @Max(999999) → 400)
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("codigo", 1_000_000, "descricao", "Grande"))
            .when().post("/api/tipos-medicamento")
            .then().statusCode(400).body("errors.codigo", notNullValue());
    }

    @Test
    void updatesDescricao() {
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("descricao", "Genérico EQ"))
            .when().put("/api/tipos-medicamento/2")
            .then().statusCode(200).body("descricao", equalTo("Genérico EQ"));
    }

    @Test
    void deletesUnused() {
        given().spec(asUser("SAUDE_CADASTRO")).when().delete("/api/tipos-medicamento/2")
            .then().statusCode(204);
    }

    @Test
    void blocksDeleteWhenReferencedByMedicamento() { // R3
        jdbc.update("INSERT INTO SAU_REM (RemCod, TipRemCod) VALUES (500, 1)");
        given().spec(asUser("SAUDE_CADASTRO")).when().delete("/api/tipos-medicamento/1")
            .then().statusCode(409);
    }

    @Test
    void lookupReturnsItems() {
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/tipos-medicamento/lookup?q=gen")
            .then().statusCode(200).body("size()", greaterThanOrEqualTo(1))
                .body("[0].descricao", notNullValue());
    }
}
