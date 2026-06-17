package br.gov.mandaguari.saude.distrito;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class DistritoControllerIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void cleanUp() {
        jdbc.update("DELETE FROM SAU_UNI");
        jdbc.update("DELETE FROM SAU_DIS");
    }

    @Test
    void listReturnsPaged() {
        createViaApi("DS Norte");
        createViaApi("DS Sul");

        given().spec(asUser("SAUDE_CADASTRO"))
                .when().get("/api/distritos")
                .then().statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(2));
    }

    @Test
    void searchByNomeFilters() {
        createViaApi("DS Norte");
        createViaApi("DS Sul");

        given().spec(asUser("SAUDE_CADASTRO"))
                .queryParam("nome", "Norte")
                .when().get("/api/distritos")
                .then().statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].nome", equalTo("DS NORTE"));
    }

    @Test
    void getReturns200() {
        short cod = createViaApi("DS Leste");

        given().spec(asUser("SAUDE_CADASTRO"))
                .when().get("/api/distritos/" + cod)
                .then().statusCode(200)
                .body("codigo", equalTo((int) cod))
                .body("nome", equalTo("DS LESTE"));
    }

    @Test
    void getReturns404ForUnknown() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .when().get("/api/distritos/9999")
                .then().statusCode(404);
    }

    @Test
    void lookupReturnsItems() {
        createViaApi("DS Oeste");

        given().spec(asUser("SAUDE_CADASTRO"))
                .queryParam("q", "Oeste")
                .when().get("/api/distritos/lookup")
                .then().statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].nome", equalTo("DS OESTE"));
    }

    @Test
    void createReturns201WithAutoAssignedCodigo() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("""
                        {"nome":"DS Central"}
                        """)
                .when().post("/api/distritos")
                .then().statusCode(201)
                .body("codigo", notNullValue())
                .body("nome", equalTo("DS CENTRAL"));
    }

    @Test
    void createWithAllFields() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "nome":"DS Completo",
                          "endereco":"Rua das Flores",
                          "numero":100,
                          "complemento":"Sala 1",
                          "cep":87900000,
                          "ddd":"44",
                          "telefone":32215000,
                          "fax":32215001
                        }
                        """)
                .when().post("/api/distritos")
                .then().statusCode(201)
                .body("ddd", equalTo("44"))
                .body("cep", equalTo(87900000));
    }

    @Test
    void createRejectsMissingNome() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("{}")
                .when().post("/api/distritos")
                .then().statusCode(400);
    }

    @Test
    void createRejectsAlphaDdd() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("""
                        {"nome":"DS Teste","ddd":"AB"}
                        """)
                .when().post("/api/distritos")
                .then().statusCode(422);
    }

    @Test
    void updateUpdatesDistrito() {
        short cod = createViaApi("DS Antiga");

        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("""
                        {"nome":"DS Atualizada"}
                        """)
                .when().put("/api/distritos/" + cod)
                .then().statusCode(200)
                .body("nome", equalTo("DS ATUALIZADA"));
    }

    @Test
    void deleteDeletesDistrito() {
        short cod = createViaApi("DS Efêmera");

        given().spec(asUser("SAUDE_CADASTRO"))
                .when().delete("/api/distritos/" + cod)
                .then().statusCode(204);

        given().spec(asUser("SAUDE_CADASTRO"))
                .when().get("/api/distritos/" + cod)
                .then().statusCode(404);
    }

    @Test
    void deleteBlockedByUnidade() {
        short cod = createViaApi("DS Ocupada");
        jdbc.update("INSERT INTO SAU_UNI(UniCod, UniDisCod) VALUES(1, ?)", cod);
        try {
            given().spec(asUser("SAUDE_CADASTRO"))
                    .when().delete("/api/distritos/" + cod)
                    .then().statusCode(409);
        } finally {
            jdbc.update("DELETE FROM SAU_UNI WHERE UniCod = 1");
        }
    }

    @Test
    void requiresAuthentication() {
        given().spec(anonymous())
                .when().get("/api/distritos")
                .then().statusCode(401);
    }

    @Test
    void createRejectsUnknownTipLogCod() {
        // Non-existent TipLogCod (non-zero) → 422 (R5)
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("""
                        {"nome":"DS Teste","tipoLogradouroCodigo":9999}
                        """)
                .when().post("/api/distritos")
                .then().statusCode(422);
    }

    @Test
    void createRejectsUnknownBairroCod() {
        // Non-existent BairroCodigo (non-zero) → 422 (R8)
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("""
                        {"nome":"DS Teste","bairroCodigo":9999}
                        """)
                .when().post("/api/distritos")
                .then().statusCode(422);
    }

    @Test
    void createWithTipLogZeroIsAccepted() {
        // FK code = 0 is the null sentinel — must not trigger FK existence check (R5)
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("""
                        {"nome":"DS Sentinela","tipoLogradouroCodigo":0,"bairroCodigo":0}
                        """)
                .when().post("/api/distritos")
                .then().statusCode(201);
    }

    @Test
    void nomeStoredAsUppercase() {
        // R4: uppercase normalisation applied by service before persist
        short cod = createViaApi("centro norte");

        given().spec(asUser("SAUDE_CADASTRO"))
                .when().get("/api/distritos/" + cod)
                .then().statusCode(200)
                .body("nome", equalTo("CENTRO NORTE"));
    }

    // --- helper ---

    private short createViaApi(String nome) {
        Integer codigo = given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"" + nome + "\"}")
                .when().post("/api/distritos")
                .then().statusCode(201)
                .extract().path("codigo");
        return codigo.shortValue();
    }
}
