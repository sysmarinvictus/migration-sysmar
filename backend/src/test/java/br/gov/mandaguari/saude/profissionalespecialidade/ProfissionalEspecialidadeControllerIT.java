package br.gov.mandaguari.saude.profissionalespecialidade;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Full-stack integration tests for /api/profissionais/{proPesCod}/especialidades — HTTP → service →
 * JPA → PostgreSQL (Testcontainers, shared container). One test per endpoint plus the R5 delete-guard
 * (SAU_IMP) and auth checks. Synthetic seed only (SAU_PROESP phi_fields=[]).
 */
class ProfissionalEspecialidadeControllerIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    static final long PRO = 100L;

    @BeforeEach
    void seed() {
        cleanAll();
        jdbc.update("INSERT INTO SAU_ESP (EspCod, EspNom, EspSit) VALUES (1, 'Cardiologia', 1)");
        jdbc.update("INSERT INTO SAU_ESP (EspCod, EspNom, EspSit) VALUES (2, 'Pediatria', 1)");
        jdbc.update("INSERT INTO SYS_PES (PesCod, PesNom) VALUES (100, 'Dr. Silva Sintético')");
        jdbc.update("INSERT INTO SAU_PRO (ProPesCod, ProSit) VALUES (100, 1)");
        jdbc.update("INSERT INTO SAU_PROESP (ProPesCod, EspCod, ProEspSit, ProEspPri) VALUES (100, 1, 1, 0)");
    }

    void cleanAll() {
        jdbc.update("DELETE FROM SAU_IMP");
        jdbc.update("DELETE FROM SAU_PROESP");
        jdbc.update("DELETE FROM SAU_PRO");
        jdbc.update("DELETE FROM SYS_PES");
        jdbc.update("DELETE FROM SAU_ESP");
    }

    @Test
    void requiresAuthentication() {
        given().spec(anonymous()).when().get("/api/profissionais/100/especialidades").then().statusCode(401);
    }

    @Test
    void forbidsWrongRole() {
        given().spec(asUser("OUTRA_ROLE")).when().get("/api/profissionais/100/especialidades").then().statusCode(403);
    }

    @Test
    void listsSpecialties() {
        given().spec(asUser("SAUDE_CADASTRO"))
            .when().get("/api/profissionais/100/especialidades")
            .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].especialidadeId", equalTo(1))
                .body("[0].situacao", equalTo(1))
                .body("[0].prioritario", equalTo(false));
    }

    @Test
    void listUnknownProfissional404() {
        given().spec(asUser("SAUDE_CADASTRO"))
            .when().get("/api/profissionais/999/especialidades")
            .then().statusCode(404);
    }

    @Test
    void addsSpecialty() { // R3 sit=1, R4 pri from body
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("especialidadeId", 2, "prioritario", true))
            .when().post("/api/profissionais/100/especialidades")
            .then().statusCode(201)
                .header("Location", containsString("/api/profissionais/100/especialidades/2"))
                .body("especialidadeId", equalTo(2))
                .body("situacao", equalTo(1))
                .body("prioritario", equalTo(true));
    }

    @Test
    void rejectsDuplicate() { // R7 — esp 1 already linked
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("especialidadeId", 1))
            .when().post("/api/profissionais/100/especialidades")
            .then().statusCode(409);
    }

    @Test
    void rejectsUnknownEspecialidade() { // R2
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("especialidadeId", 777))
            .when().post("/api/profissionais/100/especialidades")
            .then().statusCode(422).body("code", equalTo("proesp.especialidade.notfound"));
    }

    @Test
    void updatesFlags() {
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("prioritario", true, "situacao", 2, "agendaManhaQtd", 5))
            .when().put("/api/profissionais/100/especialidades/1")
            .then().statusCode(200)
                .body("prioritario", equalTo(true))
                .body("situacao", equalTo(2))
                .body("agendaManhaQtd", equalTo(5));
    }

    @Test
    void deletesSpecialty() {
        given().spec(asUser("SAUDE_CADASTRO"))
            .when().delete("/api/profissionais/100/especialidades/1")
            .then().statusCode(204);
        given().spec(asUser("SAUDE_CADASTRO"))
            .when().get("/api/profissionais/100/especialidades")
            .then().statusCode(200).body("$", hasSize(0));
    }

    @Test
    void deleteBlockedByImpedimento() { // R5 — an Impedimento for (100,1) blocks removal
        jdbc.update("INSERT INTO SAU_IMP (ImpCod, ImpDat, ImpDatIni, ImpDatFim, ProPesCod, EspCod) "
                + "VALUES (1, '2026-06-21', '2026-07-01', '2026-07-31', 100, 1)");
        given().spec(asUser("SAUDE_CADASTRO"))
            .when().delete("/api/profissionais/100/especialidades/1")
            .then().statusCode(409);
        // still present
        given().spec(asUser("SAUDE_CADASTRO"))
            .when().get("/api/profissionais/100/especialidades")
            .then().statusCode(200).body("$", hasSize(1));
    }

    @Test
    void deleteRequiresAuth() {
        given().spec(anonymous()).when().delete("/api/profissionais/100/especialidades/1")
            .then().statusCode(401);
    }
}
