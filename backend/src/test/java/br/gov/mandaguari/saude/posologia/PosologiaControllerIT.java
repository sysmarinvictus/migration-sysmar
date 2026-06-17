package br.gov.mandaguari.saude.posologia;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class PosologiaControllerIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @Test
    void createReturns201WithAutoAssignedCodigo() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("""
                        {"descricao":"Tomar 1 comprimido de 8/8h"}
                        """)
                .when().post("/api/posologias")
                .then().statusCode(201)
                .body("codigo", notNullValue())
                .body("descricao", equalTo("Tomar 1 comprimido de 8/8h"));
    }

    @Test
    void createRejectsMissingDescricao() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("{}")
                .when().post("/api/posologias")
                .then().statusCode(400);
    }

    @Test
    void listReturnsPaged() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .when().get("/api/posologias")
                .then().statusCode(200)
                .body("content", notNullValue());
    }

    @Test
    void getByIdReturns200() {
        Integer id = createViaApi("Dose teste GET");

        given().spec(asUser("SAUDE_CADASTRO"))
                .when().get("/api/posologias/{id}", id)
                .then().statusCode(200)
                .body("codigo", equalTo(id));
    }

    @Test
    void getByIdReturns404() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .when().get("/api/posologias/9999999")
                .then().statusCode(404);
    }

    @Test
    void updateDescricao() {
        Integer id = createViaApi("Descrição original");

        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("""
                        {"descricao":"Descrição atualizada"}
                        """)
                .when().put("/api/posologias/{id}", id)
                .then().statusCode(200)
                .body("descricao", equalTo("Descrição atualizada"));
    }

    @Test
    void deleteUnusedReturns204() {
        Integer id = createViaApi("Para excluir");

        given().spec(asUser("SAUDE_CADASTRO"))
                .when().delete("/api/posologias/{id}", id)
                .then().statusCode(204);
    }

    @Test
    void deleteBlockedByRemposo() {
        Integer id = createViaApi("Posologia em uso REMPOSO");
        jdbc.update("insert into SAU_REMPOSO(RemCod,PosoRemObsCod) values(9001,?)", id);
        try {
            given().spec(asUser("SAUDE_CADASTRO"))
                    .when().delete("/api/posologias/{id}", id)
                    .then().statusCode(409);
        } finally {
            jdbc.update("delete from SAU_REMPOSO where RemCod=9001");
        }
    }

    @Test
    void deleteBlockedByRecesp1() {
        Integer id = createViaApi("Posologia em uso RECESP1");
        jdbc.update("insert into SAU_RECESP1(RecEspUniCod,RecEspCod,RecEspSeq,RemObsCod) values(9001,9001,1,?)", id);
        try {
            given().spec(asUser("SAUDE_CADASTRO"))
                    .when().delete("/api/posologias/{id}", id)
                    .then().statusCode(409);
        } finally {
            jdbc.update("delete from SAU_RECESP1 where RecEspUniCod=9001 and RecEspCod=9001");
        }
    }

    @Test
    void lookupReturnsItems() {
        createViaApi("Antibiótico lookup");

        given().spec(asUser("SAUDE_CADASTRO"))
                .queryParam("q", "Antibiótico")
                .when().get("/api/posologias/lookup")
                .then().statusCode(200)
                .body("$", not(empty()));
    }

    @Test
    void requiresAuthenticationForCreate() {
        given().spec(anonymous())
                .contentType(ContentType.JSON)
                .body("""
                        {"descricao":"Sem auth"}
                        """)
                .when().post("/api/posologias")
                .then().statusCode(401);
    }

    @Test
    void requiresCorrectRoleForCreate() {
        given().spec(asUser("OUTRA_ROLE"))
                .contentType(ContentType.JSON)
                .body("""
                        {"descricao":"Role errada"}
                        """)
                .when().post("/api/posologias")
                .then().statusCode(403);
    }

    // --- helpers ---

    private Integer createViaApi(String descricao) {
        return given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("{\"descricao\":\"" + descricao + "\"}")
                .when().post("/api/posologias")
                .then().statusCode(201)
                .extract().path("codigo");
    }
}
