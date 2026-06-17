package br.gov.mandaguari.saude.tipologradouro;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class TipoLogradouroControllerIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM SAU_DIS");  // SAU_DIS stubs FK to SAU_TIPLOG
        jdbc.update("DELETE FROM SAU_TIPLOG");
        jdbc.update("INSERT INTO SAU_TIPLOG(TipLogCod,TipLogNom,TipLogSig) VALUES(1,'Rua','R.')");
        jdbc.update("INSERT INTO SAU_TIPLOG(TipLogCod,TipLogNom,TipLogSig) VALUES(2,'Avenida','Av.')");
    }

    @Test
    void listReturnsPaged() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .when().get("/api/tipos-logradouro")
                .then().statusCode(200)
                .body("content.size()", equalTo(2));
    }

    @Test
    void searchBySigla() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .queryParam("q", "Av.")
                .when().get("/api/tipos-logradouro")
                .then().statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].sigla", equalTo("Av."));
    }

    @Test
    void getByIdReturns200() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .when().get("/api/tipos-logradouro/1")
                .then().statusCode(200)
                .body("codigo", equalTo(1))
                .body("sigla", equalTo("R."));
    }

    @Test
    void getByIdReturns404() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .when().get("/api/tipos-logradouro/9999")
                .then().statusCode(404);
    }

    @Test
    void lookupReturnsItems() {
        given().spec(asUser("SAUDE_CADASTRO"))
                .queryParam("q", "Rua")
                .when().get("/api/tipos-logradouro/lookup")
                .then().statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].sigla", equalTo("R."));
    }

    @Test
    void requiresAuthentication() {
        given().spec(anonymous())
                .when().get("/api/tipos-logradouro")
                .then().statusCode(401);
    }
}
