package br.gov.mandaguari.saude.medicamento;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class MedicamentoControllerIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void cleanUp() {
        jdbc.update("DELETE FROM SAU_REMPOSO");
        jdbc.update("DELETE FROM SAU_REM2");
        jdbc.update("DELETE FROM SAU_REM_UNISETOR");
        jdbc.update("DELETE FROM SAU_REM1");
        jdbc.update("DELETE FROM SAU_REMLOT");
        jdbc.update("DELETE FROM InteracaoMedicamentosa");
        jdbc.update("DELETE FROM SAU_REM");
        jdbc.update("DELETE FROM SAU_TIPREM");
        jdbc.update("INSERT INTO SAU_TIPREM(TipRemCod, TipRemDes) VALUES(1, 'COMUM') ON CONFLICT DO NOTHING");
    }

    @Test
    void listReturnsPaged() {
        createViaApi("DIPIRONA");
        createViaApi("PARACETAMOL");
        given().spec(asUser("SAUDE_CADASTRO"))
                .when().get("/api/medicamentos")
                .then().statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(2));
    }

    @Test
    void getReturns404ForUnknown() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .when().get("/api/medicamentos/999999")
                .then().statusCode(404);
    }

    @Test
    void createReturns201WithGeneratedId() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body(minimalJson("AMOXICILINA"))
                .when().post("/api/medicamentos")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("nome", equalTo("AMOXICILINA"));
    }

    @Test
    void createRejectsBlankNome() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("{\"situacao\":1}")
                .when().post("/api/medicamentos")
                .then().statusCode(422);
    }

    @Test
    void createRejectsNonExistentTipRem() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"X\",\"tipoMedicamentoCodigo\":777,\"situacao\":1}")
                .when().post("/api/medicamentos")
                .then().statusCode(422);
    }

    @Test
    void deleteUnusedReturns204() {
        Integer id = createViaApi("EFEMERO");
        given().spec(asUser("SAUDE_CADASTRO"))
                .when().delete("/api/medicamentos/" + id)
                .then().statusCode(204);
    }

    @Test
    void deleteBlockedByRemlot() {
        Integer id = createViaApi("EM USO");
        jdbc.update("INSERT INTO SAU_REMLOT(RemCod, RemLotNum) VALUES(?, 'L1')", id);
        given().spec(asUser("SAUDE_CADASTRO"))
                .when().delete("/api/medicamentos/" + id)
                .then().statusCode(409);
    }

    @Test
    void requiresAuthentication() {
        given().spec(anonymous())
                .when().get("/api/medicamentos")
                .then().statusCode(401);
    }

    // --- helpers ---
    private Integer createViaApi(String nome) {
        return given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body(minimalJson(nome))
                .when().post("/api/medicamentos")
                .then().statusCode(201)
                .extract().path("id");
    }

    private static String minimalJson(String nome) {
        return "{\"nome\":\"%s\",\"situacao\":1}".formatted(nome);
    }
}
