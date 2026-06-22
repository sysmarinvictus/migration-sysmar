package br.gov.mandaguari.saude.formaapresentacao;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class FormaApresentacaoControllerIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void cleanUp() {
        jdbc.update("DELETE FROM SAU_REM");
        jdbc.update("DELETE FROM SAU_APRREM");
    }

    @Test
    void listReturnsPaged() {
        createViaApi("Comprimido", "CP");
        createViaApi("Xarope", "XRP");
        given().spec(asUser("SAUDE_CADASTRO"))
                .when().get("/api/formas-apresentacao")
                .then().statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(2));
    }

    @Test
    void getReturns404ForUnknown() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .when().get("/api/formas-apresentacao/999999")
                .then().statusCode(404);
    }

    @Test
    void createReturns201StoredUpperCase() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("{\"descricao\":\"comprimido\",\"abreviacao\":\"cp\"}")
                .when().post("/api/formas-apresentacao")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("descricao", equalTo("COMPRIMIDO"))
                .body("abreviacao", equalTo("CP"));
    }

    @Test
    void createRejectsBlankDescricao() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("{\"abreviacao\":\"CP\"}")
                .when().post("/api/formas-apresentacao")
                .then().statusCode(422);
    }

    @Test
    void createRejectsBlankAbreviacao() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("{\"descricao\":\"Comprimido\"}")
                .when().post("/api/formas-apresentacao")
                .then().statusCode(422);
    }

    @Test
    void deleteUnusedReturns204() {
        Integer id = createViaApi("Efêmero", "EF");
        given().spec(asUser("SAUDE_CADASTRO"))
                .when().delete("/api/formas-apresentacao/" + id)
                .then().statusCode(204);
    }

    @Test
    void deleteBlockedByMedicamento() {
        Integer id = createViaApi("Em uso", "EU");
        jdbc.update("INSERT INTO SAU_REM(RemCod, AprRemCod) VALUES(900001, ?)", id);
        given().spec(asUser("SAUDE_CADASTRO"))
                .when().delete("/api/formas-apresentacao/" + id)
                .then().statusCode(409);
    }

    @Test
    void requiresAuthentication() {
        given().spec(anonymous())
                .when().get("/api/formas-apresentacao")
                .then().statusCode(401);
    }

    // R10: authenticated but missing SAUDE_CADASTRO → 403
    @Test
    void forbiddenForUserWithoutRole() {
        given().spec(asUser("SAUDE_OUTRO"))
                .when().get("/api/formas-apresentacao")
                .then().statusCode(403);
    }

    private Integer createViaApi(String descricao, String abreviacao) {
        return given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("{\"descricao\":\"%s\",\"abreviacao\":\"%s\"}".formatted(descricao, abreviacao))
                .when().post("/api/formas-apresentacao")
                .then().statusCode(201)
                .extract().path("id");
    }
}
