package br.gov.mandaguari.saude.perfil;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Full-stack integration tests for /api/perfis — HTTP → service → JPA → PostgreSQL (Testcontainers).
 * One test per endpoint, plus security (401/403) and the key mined rules (R1/R3/R4/R5/R6). RBAC
 * maintenance is SAUDE_ADMIN-only. Synthetic data only (no PHI — RBAC catalog).
 */
class PerfilControllerIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM SAU_PRFCON");
        jdbc.update("DELETE FROM SAU_PAR4");
        jdbc.update("DELETE FROM SAU_USU");
        jdbc.update("DELETE FROM SAU_PRF");
        jdbc.update("INSERT INTO SAU_PRF (PrfCod, PrfNom) VALUES (1, 'ENFERMEIRO')");
        jdbc.update("INSERT INTO SAU_PRF (PrfCod, PrfNom) VALUES (2, 'ATENDENTE')");
    }

    @Test
    void requiresAuthentication() {
        given().spec(anonymous()).when().get("/api/perfis").then().statusCode(401);
    }

    @Test
    void forbidsWrongRole() {
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/perfis").then().statusCode(403);
    }

    @Test
    void listsAndSearches() {
        given().spec(asUser("SAUDE_ADMIN"))
            .when().get("/api/perfis?nome=enfer")
            .then().statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].nome", equalTo("ENFERMEIRO"));
    }

    @Test
    void getsByIdAnd404() {
        given().spec(asUser("SAUDE_ADMIN")).when().get("/api/perfis/1")
            .then().statusCode(200).body("nome", equalTo("ENFERMEIRO"));
        given().spec(asUser("SAUDE_ADMIN")).when().get("/api/perfis/9999")
            .then().statusCode(404).body("title", notNullValue());
    }

    @Test
    void createsWithAutoCodeAndUppercaseName() { // R1 + R3
        given().spec(asUser("SAUDE_ADMIN"))
            .body(Map.of("nome", "gestor geral"))
            .when().post("/api/perfis")
            .then().statusCode(201)
                .header("Location", containsString("/api/perfis/3"))   // MAX(1,2)+1
                .body("id", equalTo(3))
                .body("nome", equalTo("GESTOR GERAL"));
    }

    @Test
    void rejectsBlankNome() { // R2 (bean validation → 400)
        given().spec(asUser("SAUDE_ADMIN"))
            .body(Map.of("nome", ""))
            .when().post("/api/perfis")
            .then().statusCode(400).body("errors.nome", notNullValue());
    }

    @Test
    void updatesName() {
        given().spec(asUser("SAUDE_ADMIN"))
            .body(Map.of("nome", "atendente sus"))
            .when().put("/api/perfis/2")
            .then().statusCode(200).body("nome", equalTo("ATENDENTE SUS"));
    }

    @Test
    void deletesUnused() {
        given().spec(asUser("SAUDE_ADMIN")).when().delete("/api/perfis/2")
            .then().statusCode(204);
    }

    @Test
    void blocksDeleteWhenReferencedByUsuario() { // R4
        jdbc.update("INSERT INTO SAU_USU (UsuCod, UsuLogin, UsuPrfCod) VALUES (900, 'zzparity', 1)");
        given().spec(asUser("SAUDE_ADMIN")).when().delete("/api/perfis/1")
            .then().statusCode(409);
    }

    @Test
    void blocksDeleteWhenReferencedBySocialParam() { // R5
        jdbc.update("INSERT INTO SAU_PAR4 (ParEmpCod, ParProSocPrfCod) VALUES (1, 2)");
        given().spec(asUser("SAUDE_ADMIN")).when().delete("/api/perfis/2")
            .then().statusCode(409);
    }

    @Test
    void cascadeDeletesPrfconRows() { // R6
        jdbc.update("INSERT INTO SAU_PRFCON (PrfCod, PrfPrgCod) VALUES (2, 10)");
        jdbc.update("INSERT INTO SAU_PRFCON (PrfCod, PrfPrgCod) VALUES (2, 11)");
        given().spec(asUser("SAUDE_ADMIN")).when().delete("/api/perfis/2").then().statusCode(204);
        Integer remaining = jdbc.queryForObject(
                "SELECT count(*) FROM SAU_PRFCON WHERE PrfCod = 2", Integer.class);
        org.assertj.core.api.Assertions.assertThat(remaining).isZero();
    }

    @Test
    void lookupReturnsItems() {
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/perfis/lookup?q=aten")
            .then().statusCode(200).body("size()", greaterThanOrEqualTo(1))
                .body("[0].nome", notNullValue());
    }

    @Test
    void lookupRequiresAuthentication() {
        given().spec(anonymous()).when().get("/api/perfis/lookup?q=aten")
            .then().statusCode(401);
    }
}
