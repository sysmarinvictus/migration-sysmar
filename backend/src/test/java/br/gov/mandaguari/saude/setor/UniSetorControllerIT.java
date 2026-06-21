package br.gov.mandaguari.saude.setor;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class UniSetorControllerIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    static final int UNI_COD = 901;

    @BeforeEach
    void cleanUp() {
        jdbc.update("DELETE FROM SAU_UNISETOR WHERE UniCod = ?", UNI_COD);
        jdbc.update("DELETE FROM SAU_UNI WHERE UniCod = ?", UNI_COD);
        jdbc.update("INSERT INTO SAU_UNI(UniCod, UniNom, UniCnes, UniSit) VALUES(?, ?, ?, ?)",
                UNI_COD, "UBS TESTE", 9876543, 1);
    }

    @Test
    void listReturnsPaged() {
        createViaApi(10, "TRIAGEM");
        createViaApi(20, "FARMACIA");

        given().spec(asUser("SAUDE_CADASTRO"))
                .when().get("/api/unidades/" + UNI_COD + "/setores")
                .then().statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(2));
    }

    @Test
    void searchByNomeFilters() {
        createViaApi(10, "TRIAGEM");
        createViaApi(20, "FARMACIA");

        given().spec(asUser("SAUDE_CADASTRO"))
                .queryParam("nome", "FARM")
                .when().get("/api/unidades/" + UNI_COD + "/setores")
                .then().statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].nome", equalTo("FARMACIA"));
    }

    @Test
    void getReturns200WithDerivedFields() {
        createViaApi(10, "TRIAGEM");

        given().spec(asUser("SAUDE_CADASTRO"))
                .when().get("/api/unidades/" + UNI_COD + "/setores/10")
                .then().statusCode(200)
                .body("setorCod", equalTo(10))
                .body("nome", equalTo("TRIAGEM"))
                .body("unidadeNome", equalTo("UBS TESTE"));
    }

    @Test
    void getReturns404ForUnknown() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .when().get("/api/unidades/" + UNI_COD + "/setores/9999")
                .then().statusCode(404);
    }

    @Test
    void lookupReturnsItems() {
        createViaApi(10, "TRIAGEM");

        given().spec(asUser("SAUDE_CADASTRO"))
                .queryParam("q", "TRIA")
                .when().get("/api/unidades/" + UNI_COD + "/setores/lookup")
                .then().statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].nome", equalTo("TRIAGEM"));
    }

    @Test
    void createReturns201() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("""
                        {"setorCod":10,"nome":"TRIAGEM","estocador":0,"situacao":"ativo"}
                        """)
                .when().post("/api/unidades/" + UNI_COD + "/setores")
                .then().statusCode(201)
                .body("setorCod", equalTo(10))
                .body("nome", equalTo("TRIAGEM"))
                .body("situacao", equalTo("ativo"));
    }

    @Test
    void nomeStoredAsUppercase() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("""
                        {"setorCod":10,"nome":"triagem central","estocador":0,"situacao":"ativo"}
                        """)
                .when().post("/api/unidades/" + UNI_COD + "/setores")
                .then().statusCode(201)
                .body("nome", equalTo("TRIAGEM CENTRAL"));
    }

    @Test
    void createRejectsMissingNome() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("""
                        {"setorCod":10,"estocador":0,"situacao":"ativo"}
                        """)
                .when().post("/api/unidades/" + UNI_COD + "/setores")
                .then().statusCode(400);
    }

    @Test
    void createRejectsInvalidSituacao() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("""
                        {"setorCod":10,"nome":"TRIAGEM","estocador":0,"situacao":"invalido"}
                        """)
                .when().post("/api/unidades/" + UNI_COD + "/setores")
                .then().statusCode(400);
    }

    @Test
    void createRejectsDuplicateCompositeKey() {
        createViaApi(10, "TRIAGEM");

        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("""
                        {"setorCod":10,"nome":"DUPLICADO","estocador":0,"situacao":"ativo"}
                        """)
                .when().post("/api/unidades/" + UNI_COD + "/setores")
                .then().statusCode(409);
    }

    @Test
    void createRejectsNonExistentUnidade() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("""
                        {"setorCod":10,"nome":"TRIAGEM","estocador":0,"situacao":"ativo"}
                        """)
                .when().post("/api/unidades/99999/setores")
                .then().statusCode(422);
    }

    @Test
    void updateUpdatesSetor() {
        createViaApi(10, "TRIAGEM");

        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("""
                        {"nome":"ATUALIZADO","estocador":1,"situacao":"inativo"}
                        """)
                .when().put("/api/unidades/" + UNI_COD + "/setores/10")
                .then().statusCode(200)
                .body("nome", equalTo("ATUALIZADO"))
                .body("estocador", equalTo(1))
                .body("situacao", equalTo("inativo"));
    }

    @Test
    void deleteDeletesSetor() {
        createViaApi(10, "TRIAGEM");

        given().spec(asUser("SAUDE_CADASTRO"))
                .when().delete("/api/unidades/" + UNI_COD + "/setores/10")
                .then().statusCode(204);

        given().spec(asUser("SAUDE_CADASTRO"))
                .when().get("/api/unidades/" + UNI_COD + "/setores/10")
                .then().statusCode(404);
    }

    @Test
    void deleteBlockedBySauPar5() {
        createViaApi(10, "TRIAGEM");
        jdbc.update("INSERT INTO SAU_PAR5(ParEmpCod, ParSalUniCod, ParSalSetorCod) VALUES(1, ?, ?)",
                UNI_COD, 10);
        try {
            given().spec(asUser("SAUDE_CADASTRO"))
                    .when().delete("/api/unidades/" + UNI_COD + "/setores/10")
                    .then().statusCode(409);
        } finally {
            jdbc.update("DELETE FROM SAU_PAR5 WHERE ParEmpCod = 1");
        }
    }

    @Test
    void deleteBlockedBySauUsuUni1() {
        createViaApi(10, "TRIAGEM");
        jdbc.update("INSERT INTO SAU_USUUNI1(UsuCod, UniUsuCod, UsuSetorCod) VALUES(1, ?, ?)",
                UNI_COD, 10);
        try {
            given().spec(asUser("SAUDE_CADASTRO"))
                    .when().delete("/api/unidades/" + UNI_COD + "/setores/10")
                    .then().statusCode(409);
        } finally {
            jdbc.update("DELETE FROM SAU_USUUNI1 WHERE UsuCod = 1");
        }
    }

    @Test
    void deleteBlockedBySauRemLot() {
        createViaApi(10, "TRIAGEM");
        jdbc.update("INSERT INTO SAU_REMLOT(RemCod, RemUniCod, RemSetorCod) VALUES(1, ?, ?)",
                UNI_COD, 10);
        try {
            given().spec(asUser("SAUDE_CADASTRO"))
                    .when().delete("/api/unidades/" + UNI_COD + "/setores/10")
                    .then().statusCode(409);
        } finally {
            jdbc.update("DELETE FROM SAU_REMLOT WHERE RemCod = 1");
        }
    }

    @Test
    void requiresAuthentication() {
        given().spec(anonymous())
                .when().get("/api/unidades/" + UNI_COD + "/setores")
                .then().statusCode(401);
    }

    // --- helper ---

    private void createViaApi(int setorCod, String nome) {
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("{\"setorCod\":" + setorCod + ",\"nome\":\"" + nome + "\",\"estocador\":0,\"situacao\":\"ativo\"}")
                .when().post("/api/unidades/" + UNI_COD + "/setores")
                .then().statusCode(201);
    }
}
