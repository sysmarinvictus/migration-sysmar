package br.gov.mandaguari.saude.impedimento;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Full-stack integration tests for /api/impedimentos — HTTP → service → JPA → PostgreSQL
 * (Testcontainers, shared container via AbstractIntegrationTest). One test per endpoint plus
 * key rule coverage.
 *
 * Seed data uses synthetic values only (no PHI — SAU_IMP has phi_fields=[]).
 * CborCod seeded as '225125' (CHAR(6), confirmed from live DB).
 * SAU_PROESP uses ProPesCod BIGINT (fixed in V1__baseline.sql 2026-06-21).
 */
class ImpedimentoControllerIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        cleanAll();
        jdbc.update("INSERT INTO SAU_CBOR (CborCod, CborDes) VALUES ('225125', 'Médico cardiologista')");
        jdbc.update("INSERT INTO SAU_ESP (EspCod, EspNom, EspSit, EspCborCod) VALUES (1, 'Cardiologia', 'A', '225125')");
        jdbc.update("INSERT INTO SYS_PES (PesCod, PesNom) VALUES (100, 'Dr. Silva Sintético')");
        jdbc.update("INSERT INTO SAU_PRO (ProPesCod, ProSit) VALUES (100, 1)");
        jdbc.update("INSERT INTO SAU_PROESP (ProPesCod, EspCod) VALUES (100, 1)");
        jdbc.update("INSERT INTO SAU_IMP (ImpCod, ImpDat, ImpDatIni, ImpDatFim, ProPesCod, EspCod) "
                + "VALUES (1, '2026-06-21', '2026-07-01', '2026-07-31', 100, 1)");
    }

    void cleanAll() {
        jdbc.update("DELETE FROM SAU_IMP");
        jdbc.update("DELETE FROM SAU_PROESP");
        jdbc.update("DELETE FROM SAU_PRO");
        jdbc.update("DELETE FROM SYS_PES");
        jdbc.update("DELETE FROM SAU_ESP");
        jdbc.update("DELETE FROM SAU_CBOR");
    }

    @Test
    void requiresAuthentication() {
        given().spec(anonymous()).when().get("/api/impedimentos").then().statusCode(401);
    }

    @Test
    void forbidsWrongRole() {
        given().spec(asUser("OUTRA_ROLE")).when().get("/api/impedimentos").then().statusCode(403);
    }

    @Test
    void listsPaginated() {
        given().spec(asUser("SAUDE_CADASTRO"))
            .when().get("/api/impedimentos")
            .then().statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].codigo", equalTo(1))
                .body("content[0].profissionalNome", equalTo("Dr. Silva Sintético"))
                .body("content[0].especialidadeNome", equalTo("Cardiologia"))
                .body("content[0].cboDescricao", equalTo("Médico cardiologista"));
    }

    @Test
    void getsByIdAnd404() {
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/impedimentos/1")
            .then().statusCode(200)
                .body("codigo", equalTo(1))
                .body("dataInicio", equalTo("2026-07-01"));

        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/impedimentos/9999")
            .then().statusCode(404).body("title", notNullValue());
    }

    @Test
    void createsValid() { // R4: next PK = 2
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of(
                "dataCadastro", "2026-06-21",
                "dataInicio",   "2026-08-01",
                "dataFim",      "2026-08-31",
                "profissionalCodigo",  100,
                "especialidadeCodigo", 1))
            .when().post("/api/impedimentos")
            .then().statusCode(201)
                .header("Location", containsString("/api/impedimentos/2"))
                .body("codigo", equalTo(2));
    }

    @Test
    void defaultsDataCadastroToday() { // R6
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of(
                "dataInicio",   "2026-09-01",
                "dataFim",      "2026-09-30",
                "profissionalCodigo",  100,
                "especialidadeCodigo", 1))
            .when().post("/api/impedimentos")
            .then().statusCode(201)
                .body("dataCadastro", notNullValue());
    }

    @Test
    void rejectsMissingProfissional() { // R8
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of(
                "dataInicio",   "2026-08-01",
                "dataFim",      "2026-08-31",
                "profissionalCodigo",  999L,
                "especialidadeCodigo", 1))
            .when().post("/api/impedimentos")
            .then().statusCode(422).body("code", equalTo("imp.profissional.notfound"));
    }

    @Test
    void rejectsMissingProEspPair() { // R11: insert esp=2 (not in PROESP for pro=100)
        jdbc.update("INSERT INTO SAU_ESP (EspCod, EspNom) VALUES (2, 'Pediatria')");
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of(
                "dataInicio",   "2026-08-01",
                "dataFim",      "2026-08-31",
                "profissionalCodigo",  100,
                "especialidadeCodigo", 2))
            .when().post("/api/impedimentos")
            .then().statusCode(422).body("code", equalTo("imp.proesp.notfound"));
    }

    @Test
    void rejectsMissingDataInicio() { // R13 — @NotNull
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of(
                "dataFim",             "2026-08-31",
                "profissionalCodigo",  100,
                "especialidadeCodigo", 1))
            .when().post("/api/impedimentos")
            .then().statusCode(400).body("errors.dataInicio", notNullValue());
    }

    @Test
    void updatesRecord() {
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of(
                "dataCadastro",        "2026-06-21",
                "dataInicio",          "2026-10-01",
                "dataFim",             "2026-10-31",
                "profissionalCodigo",  100,
                "especialidadeCodigo", 1))
            .when().put("/api/impedimentos/1")
            .then().statusCode(200)
                .body("dataInicio", equalTo("2026-10-01"));
    }

    @Test
    void deletesRecord() {
        given().spec(asUser("SAUDE_CADASTRO")).when().delete("/api/impedimentos/1")
            .then().statusCode(204);

        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/impedimentos/1")
            .then().statusCode(404);
    }

    @Test
    void deleteRequiresAuth() {
        given().spec(anonymous()).when().delete("/api/impedimentos/1")
            .then().statusCode(401);
    }
}
