package br.gov.mandaguari.saude.unidade;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class UnidadeControllerIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    // Valid CNPJ: 11.222.333/0001-81
    private static final String VALID_CNPJ = "11.222.333/0001-81";

    @BeforeEach
    void cleanUp() {
        jdbc.update("DELETE FROM SAU_UNISETOR");
        jdbc.update("DELETE FROM SAU_RECESP");
        jdbc.update("DELETE FROM SAU_PROESP1");
        jdbc.update("DELETE FROM SAU_PAR5");
        jdbc.update("DELETE FROM SAU_USUUNI");
        jdbc.update("DELETE FROM SAU_USU");
        jdbc.update("DELETE FROM SAU_REM1");
        jdbc.update("DELETE FROM SAU_REM_UNISETOR");
        jdbc.update("DELETE FROM SAU_PAC");
        jdbc.update("DELETE FROM SAU_PAR2");
        jdbc.update("DELETE FROM SAU_UNI");
        jdbc.update("DELETE FROM SAU_LOC");
        jdbc.update("DELETE FROM SYS_MUN");
        jdbc.update("INSERT INTO SYS_MUN(MunCod, MunNom) VALUES(1, 'Mandaguari')");
    }

    @Test
    void listReturnsPaged() {
        createViaApi("UBS Central");
        createViaApi("UBS Norte");

        given().spec(asUser("SAUDE_CADASTRO"))
                .when().get("/api/unidades")
                .then().statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(2));
    }

    @Test
    void searchByNomeFilters() {
        createViaApi("UBS Central");
        createViaApi("UBS Norte");

        given().spec(asUser("SAUDE_CADASTRO"))
                .queryParam("nome", "Central")
                .when().get("/api/unidades")
                .then().statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].nome", equalTo("UBS CENTRAL"));
    }

    @Test
    void getReturns200() {
        Integer cod = createViaApi("UBS Leste");

        given().spec(asUser("SAUDE_CADASTRO"))
                .when().get("/api/unidades/" + cod)
                .then().statusCode(200)
                .body("codigo", equalTo(cod))
                .body("nome", equalTo("UBS LESTE"));
    }

    @Test
    void getReturns404ForUnknown() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .when().get("/api/unidades/9999")
                .then().statusCode(404);
    }

    @Test
    void lookupReturnsItems() {
        createViaApi("UBS Oeste");

        given().spec(asUser("SAUDE_CADASTRO"))
                .queryParam("q", "Oeste")
                .when().get("/api/unidades/lookup")
                .then().statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].nome", equalTo("UBS OESTE"));
    }

    @Test
    void createReturns201WithAutoAssignedCodigo() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body(minimalJson("UBS Nova"))
                .when().post("/api/unidades")
                .then().statusCode(201)
                .body("codigo", notNullValue())
                .body("nome", equalTo("UBS NOVA"));
    }

    @Test
    void createRejectsMissingNome() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("{\"cnpj\":\"" + VALID_CNPJ + "\",\"cep\":\"87900000\",\"endereco\":\"Rua A\",\"enderecoNumero\":\"1\",\"bairro\":\"Centro\",\"municipioCodigo\":1}")
                .when().post("/api/unidades")
                .then().statusCode(400);
    }

    @Test
    void createRejectsInvalidCnpj() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"UBS A\",\"cnpj\":\"11111111111111\",\"cep\":\"87900000\",\"endereco\":\"Rua A\",\"enderecoNumero\":\"1\",\"bairro\":\"Centro\",\"municipioCodigo\":1}")
                .when().post("/api/unidades")
                .then().statusCode(422);
    }

    @Test
    void createRejectsMissingCep() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"UBS A\",\"cnpj\":\"" + VALID_CNPJ + "\",\"endereco\":\"Rua A\",\"enderecoNumero\":\"1\",\"bairro\":\"Centro\",\"municipioCodigo\":1}")
                .when().post("/api/unidades")
                .then().statusCode(400);
    }

    @Test
    void updateUpdatesUnidade() {
        Integer cod = createViaApi("UBS Antiga");

        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body(minimalJson("UBS Atualizada"))
                .when().put("/api/unidades/" + cod)
                .then().statusCode(200)
                .body("nome", equalTo("UBS ATUALIZADA"));
    }

    @Test
    void deleteDeletesUnidade() {
        Integer cod = createViaApi("UBS Efêmera");

        given().spec(asUser("SAUDE_CADASTRO"))
                .when().delete("/api/unidades/" + cod)
                .then().statusCode(204);

        given().spec(asUser("SAUDE_CADASTRO"))
                .when().get("/api/unidades/" + cod)
                .then().statusCode(404);
    }

    @Test
    void deleteBlockedByUnisetor() {
        Integer cod = createViaApi("UBS Ocupada");
        jdbc.update("INSERT INTO SAU_SETOR(SetorCod, SetorNom) VALUES(1, 'Geral') ON CONFLICT DO NOTHING");
        jdbc.update("INSERT INTO SAU_UNISETOR(UniCod, SetorCod) VALUES(?, 1)", cod);
        try {
            given().spec(asUser("SAUDE_CADASTRO"))
                    .when().delete("/api/unidades/" + cod)
                    .then().statusCode(409);
        } finally {
            jdbc.update("DELETE FROM SAU_UNISETOR WHERE UniCod = ?", cod);
        }
    }

    @Test
    void requiresAuthentication() {
        given().spec(anonymous())
                .when().get("/api/unidades")
                .then().statusCode(401);
    }

    // --- helpers ---

    private Integer createViaApi(String nome) {
        return given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body(minimalJson(nome))
                .when().post("/api/unidades")
                .then().statusCode(201)
                .extract().path("codigo");
    }

    private static String minimalJson(String nome) {
        return """
                {
                  "nome": "%s",
                  "cnpj": "%s",
                  "cep": "87900000",
                  "endereco": "Rua das Flores",
                  "enderecoNumero": "100",
                  "bairro": "Centro",
                  "municipioCodigo": 1
                }
                """.formatted(nome, VALID_CNPJ);
    }
}
