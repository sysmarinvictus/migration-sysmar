package br.gov.mandaguari.saude.usuariounidade;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Full-stack tests for the SAU_USUUNI capability matrix (/api/usuarios/{usuCod}/unidades) —
 * HTTP → service → JPA → PostgreSQL (Testcontainers). Exercises the composite-PK grant/update/revoke,
 * FK checks, and the V11 PK promotion (composite PK must allow one user across multiple units).
 * No PHI — capability flags only. SAUDE_ADMIN.
 */
class UsuarioUnidadeControllerIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    static final int USU = 7, UNI = 3, UNI2 = 4;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM SAU_USUUNI");
        jdbc.update("INSERT INTO SAU_USU (UsuCod) VALUES (7) ON CONFLICT (UsuCod) DO NOTHING");
        jdbc.update("INSERT INTO SAU_UNI (UniCod) VALUES (3) ON CONFLICT (UniCod) DO NOTHING");
        jdbc.update("INSERT INTO SAU_UNI (UniCod) VALUES (4) ON CONFLICT (UniCod) DO NOTHING");
        jdbc.update("INSERT INTO SAU_ESP (EspCod, EspNom) VALUES (1, 'Cardiologia') ON CONFLICT (EspCod) DO NOTHING");
    }

    @Test
    void requiresAuthentication() {
        given().spec(anonymous()).when().get("/api/usuarios/7/unidades").then().statusCode(401);
    }

    @Test
    void forbidsNonAdmin() { // SAUDE_ADMIN required (this is authorization administration)
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/usuarios/7/unidades").then().statusCode(403);
    }

    @Test
    void grantsAccessAndPersistsFlags() { // R2/R4/R5 + full boolean mapping
        given().spec(asUser("SAUDE_ADMIN"))
            .queryParam("uniCod", UNI)
            .body(Map.of("bloqueioFarmacia", true, "permiteBnafar", true, "especialidadeCod", 1))
            .when().post("/api/usuarios/7/unidades")
            .then().statusCode(201)
                .header("Location", containsString("/api/usuarios/7/unidades/3"))
                .body("usuCod", equalTo(7))
                .body("uniCod", equalTo(3))
                .body("bloqueioFarmacia", equalTo(true))
                .body("permiteBnafar", equalTo(true))
                .body("bloqueioAgenda", nullValue());   // R5: unset flags stay null
    }

    @Test
    void oneUserAcrossMultipleUnits() { // proves the composite PK (V11 promotion) — same user, two units
        given().spec(asUser("SAUDE_ADMIN")).queryParam("uniCod", UNI).body(Map.of("bloqueioAgenda", true))
            .when().post("/api/usuarios/7/unidades").then().statusCode(201);
        given().spec(asUser("SAUDE_ADMIN")).queryParam("uniCod", UNI2).body(Map.of("bloqueioAgenda", false))
            .when().post("/api/usuarios/7/unidades").then().statusCode(201);
        given().spec(asUser("SAUDE_ADMIN")).when().get("/api/usuarios/7/unidades")
            .then().statusCode(200).body("$", hasSize(2));
    }

    @Test
    void rejectsUnknownUnit() { // R2
        given().spec(asUser("SAUDE_ADMIN")).queryParam("uniCod", 999).body(Map.of("bloqueioAgenda", true))
            .when().post("/api/usuarios/7/unidades")
            .then().statusCode(422).body("code", equalTo("usuuni.unidade.notfound"));
    }

    @Test
    void rejectsDuplicateGrant() { // R4
        given().spec(asUser("SAUDE_ADMIN")).queryParam("uniCod", UNI).body(Map.of())
            .when().post("/api/usuarios/7/unidades").then().statusCode(201);
        given().spec(asUser("SAUDE_ADMIN")).queryParam("uniCod", UNI).body(Map.of())
            .when().post("/api/usuarios/7/unidades").then().statusCode(409);
    }

    @Test
    void rejectsUnknownEspecialidade() { // R3
        given().spec(asUser("SAUDE_ADMIN")).queryParam("uniCod", UNI).body(Map.of("especialidadeCod", 777))
            .when().post("/api/usuarios/7/unidades")
            .then().statusCode(422).body("code", equalTo("usuuni.especialidade.notfound"));
    }

    @Test
    void updatesFlags() {
        given().spec(asUser("SAUDE_ADMIN")).queryParam("uniCod", UNI).body(Map.of("bloqueioFarmacia", true))
            .when().post("/api/usuarios/7/unidades").then().statusCode(201);
        given().spec(asUser("SAUDE_ADMIN")).body(Map.of("bloqueioFarmacia", false, "bloqueioAgenda", true))
            .when().put("/api/usuarios/7/unidades/3")
            .then().statusCode(200)
                .body("bloqueioFarmacia", equalTo(false))
                .body("bloqueioAgenda", equalTo(true));
    }

    @Test
    void revokesAccess() { // R6
        given().spec(asUser("SAUDE_ADMIN")).queryParam("uniCod", UNI).body(Map.of())
            .when().post("/api/usuarios/7/unidades").then().statusCode(201);
        given().spec(asUser("SAUDE_ADMIN")).when().delete("/api/usuarios/7/unidades/3").then().statusCode(204);
        given().spec(asUser("SAUDE_ADMIN")).when().get("/api/usuarios/7/unidades")
            .then().statusCode(200).body("$", hasSize(0));
    }

    @Test
    void listUnknownUser404() { // R1
        given().spec(asUser("SAUDE_ADMIN")).when().get("/api/usuarios/9999/unidades").then().statusCode(404);
    }
}
