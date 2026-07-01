package br.gov.mandaguari.saude.parametro;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Full-stack tests for the SAU_PAR singleton config (/api/parametros) — HTTP → service → JPA →
 * PostgreSQL (Testcontainers). The test tenant (audit.empresa-codigo) is 1, so the singleton row is
 * seeded with ParEmpCod=1. SAUDE_ADMIN. No PHI (system config).
 */
class ParametroControllerIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM SAU_PAR");
        jdbc.update("INSERT INTO SAU_PAR (ParEmpCod, ParInaUsuDias, ParSenUsuDias, ParSecr) VALUES (1, 30, 60, 'SMS Inicial')");
    }

    @Test
    void requiresAuthentication() {
        given().spec(anonymous()).when().get("/api/parametros").then().statusCode(401);
    }

    @Test
    void forbidsNonAdmin() {
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/parametros").then().statusCode(403);
    }

    @Test
    void readsSingleton() {
        given().spec(asUser("SAUDE_ADMIN")).when().get("/api/parametros")
            .then().statusCode(200)
                .body("empresaCod", equalTo(1))
                .body("inatividadeUsuarioDias", equalTo(30))
                .body("secretaria", equalTo("SMS Inicial"));
    }

    @Test
    void updatesGeral() {
        Map<String, Object> b = new HashMap<>();
        b.put("validadeReceita", true);
        b.put("validadeReceitaSimplesDias", 30);
        b.put("inatividadeUsuarioDias", 45);
        b.put("senhaUsuarioDias", 90);
        b.put("secretaria", "Secretaria Municipal de Saúde");
        b.put("secretariaCep", "87100000");
        b.put("cadastroSemCns", true);
        given().spec(asUser("SAUDE_ADMIN")).body(b)
            .when().put("/api/parametros/geral")
            .then().statusCode(200)
                .body("inatividadeUsuarioDias", equalTo(45))
                .body("senhaUsuarioDias", equalTo(90))
                .body("secretaria", equalTo("Secretaria Municipal de Saúde"))
                .body("cadastroSemCns", equalTo(true));
    }

    @Test
    void rejectsMissingDias() { // R1
        Map<String, Object> b = new HashMap<>();
        b.put("inatividadeUsuarioDias", 0);   // required, must be > 0
        b.put("senhaUsuarioDias", 60);
        given().spec(asUser("SAUDE_ADMIN")).body(b)
            .when().put("/api/parametros/geral")
            .then().statusCode(422).body("code", equalTo("par.dias.required"));
    }

    @Test
    void rejectsDiasOver180() { // R2
        Map<String, Object> b = new HashMap<>();
        b.put("inatividadeUsuarioDias", 30);
        b.put("senhaUsuarioDias", 200);
        given().spec(asUser("SAUDE_ADMIN")).body(b)
            .when().put("/api/parametros/geral")
            .then().statusCode(422).body("code", equalTo("par.dias.max"));
    }

    @Test
    void updatesAmbulatorial() {
        given().spec(asUser("SAUDE_ADMIN"))
            .body(Map.of("exigeCid10Atestado", true, "estornarAtendimento", false, "imprimeRiscoMaterno", 1))
            .when().put("/api/parametros/ambulatorial")
            .then().statusCode(200)
                .body("exigeCid10Atestado", equalTo(true))
                .body("imprimeRiscoMaterno", equalTo(1));
    }
}
