package br.gov.mandaguari.saude.bairro;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class BairroControllerIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @Test
    void createReturns201WithAutoAssignedCodigo() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("""
                        {"nome":"Jardim das Flores"}
                        """)
                .when().post("/api/bairros")
                .then().statusCode(201)
                .body("codigo", notNullValue())
                .body("nome", equalTo("Jardim das Flores"));
    }

    @Test
    void createRejectsMissingNome() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("{}")
                .when().post("/api/bairros")
                .then().statusCode(400);
    }

    @Test
    void createRejectsDuplicateNome() {
        createViaApi("Vila Única");

        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("""
                        {"nome":"Vila Única"}
                        """)
                .when().post("/api/bairros")
                .then().statusCode(409);
    }

    @Test
    void createRejectsDuplicateNomeCaseInsensitive() {
        createViaApi("Alto Paraná");

        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("""
                        {"nome":"ALTO PARANÁ"}
                        """)
                .when().post("/api/bairros")
                .then().statusCode(409);
    }

    @Test
    void listReturnsPaged() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .when().get("/api/bairros")
                .then().statusCode(200)
                .body("content", notNullValue());
    }

    @Test
    void getByIdReturns200() {
        Integer id = createViaApi("Bairro GET Teste");

        given().spec(asUser("SAUDE_CADASTRO"))
                .when().get("/api/bairros/{id}", id)
                .then().statusCode(200)
                .body("codigo", equalTo(id));
    }

    @Test
    void getByIdReturns404() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .when().get("/api/bairros/9999999")
                .then().statusCode(404);
    }

    @Test
    void updateNome() {
        Integer id = createViaApi("Nome Original IT");

        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("""
                        {"nome":"Nome Atualizado IT"}
                        """)
                .when().put("/api/bairros/{id}", id)
                .then().statusCode(200)
                .body("nome", equalTo("Nome Atualizado IT"));
    }

    @Test
    void updateAllowsKeepingSameName() {
        Integer id = createViaApi("Nome Imutável IT");

        given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("""
                        {"nome":"Nome Imutável IT"}
                        """)
                .when().put("/api/bairros/{id}", id)
                .then().statusCode(200)
                .body("nome", equalTo("Nome Imutável IT"));
    }

    @Test
    void deleteUnusedReturns204() {
        Integer id = createViaApi("Para Excluir IT");

        given().spec(asUser("SAUDE_CADASTRO"))
                .when().delete("/api/bairros/{id}", id)
                .then().statusCode(204);
    }

    @Test
    void deleteBlockedByPessoa() {
        Integer id = createViaApi("Bairro com Pessoa IT");
        jdbc.update("insert into SYS_PES(PesCod,PesBaiCod) values(9001,?)", id);
        try {
            given().spec(asUser("SAUDE_CADASTRO"))
                    .when().delete("/api/bairros/{id}", id)
                    .then().statusCode(409);
        } finally {
            jdbc.update("delete from SYS_PES where PesCod=9001");
        }
    }

    @Test
    void deleteBlockedByDistrito() {
        Integer id = createViaApi("Bairro com Distrito IT");
        jdbc.update("insert into SAU_DIS(DisCod,DisBaiCod) values(91,?)", id);
        try {
            given().spec(asUser("SAUDE_CADASTRO"))
                    .when().delete("/api/bairros/{id}", id)
                    .then().statusCode(409);
        } finally {
            jdbc.update("delete from SAU_DIS where DisCod=91");
        }
    }

    @Test
    void lookupReturnsItems() {
        createViaApi("Bairro Lookup IT");

        given().spec(asUser("SAUDE_CADASTRO"))
                .queryParam("q", "Lookup")
                .when().get("/api/bairros/lookup")
                .then().statusCode(200)
                .body("$", not(empty()));
    }

    @Test
    void requiresAuthenticationForCreate() {
        given().spec(anonymous())
                .contentType(ContentType.JSON)
                .body("""
                        {"nome":"Sem auth"}
                        """)
                .when().post("/api/bairros")
                .then().statusCode(401);
    }

    @Test
    void requiresCorrectRoleForCreate() {
        given().spec(asUser("OUTRA_ROLE"))
                .contentType(ContentType.JSON)
                .body("""
                        {"nome":"Role errada"}
                        """)
                .when().post("/api/bairros")
                .then().statusCode(403);
    }

    // --- helpers ---

    private Integer createViaApi(String nome) {
        return given().spec(asUser("SAUDE_CADASTRO"))
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"" + nome + "\"}")
                .when().post("/api/bairros")
                .then().statusCode(201)
                .extract().path("codigo");
    }
}
