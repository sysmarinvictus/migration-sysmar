package br.gov.mandaguari.saude.receituarioespecial;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Full-stack tests for the Receituário Controle Especial slice (/api/receituarios-especiais) —
 * HTTP → service → JPA → PostgreSQL (Testcontainers). Confirms the master+child aggregate write, the
 * per-unit sequential numbering (R1), the mined validations, and the regulatory delete-block (R29/OQ2).
 * Synthetic PHI only.
 */
class ReceituarioEspecialControllerIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    static final long PAC = 500L;
    static final long PRO = 100L;
    static final int UNI = 1;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM SAU_RECESP1");
        jdbc.update("DELETE FROM SAU_RECESP");
        jdbc.update("DELETE FROM SAU_PAC");
        jdbc.update("DELETE FROM SAU_PRO");
        jdbc.update("DELETE FROM SAU_REM");
        jdbc.update("DELETE FROM SAU_REMOBS");
        jdbc.update("DELETE FROM SYS_PES");
        jdbc.update("INSERT INTO SAU_UNI (UniCod) VALUES (1) ON CONFLICT (UniCod) DO NOTHING");
        // patient (SYS_PES person + SAU_PAC subtype, active, with CNS)
        jdbc.update("INSERT INTO SYS_PES (PesCod, PesNom, PesNumCns, PesNasDat, PesUsaNomSoc) "
                + "VALUES (?, 'Maria Silva', '700000000000005', DATE '1990-01-01', false)", PAC);
        jdbc.update("INSERT INTO SAU_PAC (PacPesCod, PacSit) VALUES (?, 1)", PAC);
        // prescriber (SYS_PES person + SAU_PRO subtype)
        jdbc.update("INSERT INTO SYS_PES (PesCod, PesNom) VALUES (?, 'Dr House')", PRO);
        jdbc.update("INSERT INTO SAU_PRO (ProPesCod) VALUES (?)", PRO);
        // catalog drug + posology
        jdbc.update("INSERT INTO SAU_REM (RemCod, RemNom) VALUES (42, 'CLONAZEPAM 2MG')");
        jdbc.update("INSERT INTO SAU_REMOBS (RemObsCod, RemObsDes) VALUES (7, '1 comprimido ao dia')");
    }

    private Map<String, Object> item(Integer remCod, String prescricao, Integer tipo) {
        Map<String, Object> i = new HashMap<>();
        if (remCod != null) i.put("medicamentoCodigo", remCod);
        if (prescricao != null) i.put("prescricao", prescricao);
        i.put("tipoReceita", tipo);
        i.put("tipoUso", 1);
        return i;
    }

    private Map<String, Object> validBody() {
        Map<String, Object> b = new HashMap<>();
        b.put("unidadeCodigo", UNI);
        b.put("data", "2026-06-01");
        b.put("pacienteCodigo", PAC);
        b.put("prescritorCodigo", PRO);
        List<Map<String, Object>> itens = new ArrayList<>();
        itens.add(item(0, "Rivotril 2mg", 3));   // free-text controlled line
        b.put("itens", itens);
        return b;
    }

    // ---- auth ----

    @Test void requiresAuthentication() {
        given().spec(anonymous()).body(validBody()).when().post("/api/receituarios-especiais").then().statusCode(401);
    }

    @Test void forbidsWrongRole() {
        given().spec(asUser("OUTRA_ROLE")).body(validBody()).when().post("/api/receituarios-especiais").then().statusCode(403);
    }

    // ---- create + sequential numbering (R1) ----

    @Test void createsAndAllocatesSequentialNumberPerUnit() {
        given().spec(asUser("SAUDE_CADASTRO")).body(validBody())
            .when().post("/api/receituarios-especiais")
            .then().statusCode(201)
                .header("Location", containsString("/api/receituarios-especiais/1/1"))
                .body("numero", equalTo(1))
                .body("pacienteNome", equalTo("Maria Silva"))
                .body("pacienteIdade", equalTo(36))            // R15
                .body("itens", hasSize(1))
                .body("itens[0].sequencia", equalTo(1))        // R26
                .body("itens[0].prescricao", equalTo("Rivotril 2mg"));

        // second prescription in the same unit → number 2
        given().spec(asUser("SAUDE_CADASTRO")).body(validBody())
            .when().post("/api/receituarios-especiais")
            .then().statusCode(201).body("numero", equalTo(2));

        Integer master = jdbc.queryForObject("SELECT count(*) FROM SAU_RECESP WHERE RecEspUniCod = 1", Integer.class);
        Integer lines = jdbc.queryForObject("SELECT count(*) FROM SAU_RECESP1 WHERE RecEspUniCod = 1", Integer.class);
        org.assertj.core.api.Assertions.assertThat(master).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(lines).isEqualTo(2);
    }

    @Test void defaultsPrescriptionTextFromMedication() { // R19
        Map<String, Object> b = validBody();
        b.put("itens", List.of(item(42, "ignored", 3)));
        given().spec(asUser("SAUDE_CADASTRO")).body(b)
            .when().post("/api/receituarios-especiais")
            .then().statusCode(201).body("itens[0].prescricao", equalTo("CLONAZEPAM 2MG"));
    }

    // ---- validations ----

    @Test void rejectsMissingDate() { // R4
        Map<String, Object> b = validBody();
        b.remove("data");
        given().spec(asUser("SAUDE_CADASTRO")).body(b)
            .when().post("/api/receituarios-especiais")
            .then().statusCode(422).body("code", equalTo("recesp.data.required"));
    }

    @Test void rejectsUnknownPatient() { // R10
        Map<String, Object> b = validBody();
        b.put("pacienteCodigo", 999999);
        given().spec(asUser("SAUDE_CADASTRO")).body(b)
            .when().post("/api/receituarios-especiais")
            .then().statusCode(422).body("code", equalTo("recesp.paciente.notfound"));
    }

    @Test void rejectsInactivePatient() { // R12
        jdbc.update("UPDATE SAU_PAC SET PacSit = 2 WHERE PacPesCod = ?", PAC);
        given().spec(asUser("SAUDE_CADASTRO")).body(validBody())
            .when().post("/api/receituarios-especiais")
            .then().statusCode(422).body("code", equalTo("recesp.paciente.inativo"));
    }

    @Test void warnsWhenPatientHasNoCns() { // R13 — non-blocking
        jdbc.update("UPDATE SYS_PES SET PesNumCns = NULL WHERE PesCod = ?", PAC);
        given().spec(asUser("SAUDE_CADASTRO")).body(validBody())
            .when().post("/api/receituarios-especiais")
            .then().statusCode(201).body("avisos", hasItem(containsString("CNS")));
    }

    @Test void rejectsMissingPrescriptionText() { // R20
        Map<String, Object> b = validBody();
        b.put("itens", List.of(item(0, null, 3)));
        given().spec(asUser("SAUDE_CADASTRO")).body(b)
            .when().post("/api/receituarios-especiais")
            .then().statusCode(422).body("code", equalTo("recesp.item.prescricao.required"));
    }

    @Test void rejectsMissingReceituarioType() { // R23
        Map<String, Object> b = validBody();
        b.put("itens", List.of(item(0, "Rivotril", 0)));
        given().spec(asUser("SAUDE_CADASTRO")).body(b)
            .when().post("/api/receituarios-especiais")
            .then().statusCode(422).body("code", equalTo("recesp.item.tipo.required"));
    }

    // ---- read / copy / delete ----

    @Test void getReturnsMasterAndItems() {
        given().spec(asUser("SAUDE_CADASTRO")).body(validBody()).when().post("/api/receituarios-especiais").then().statusCode(201);
        given().spec(asUser("SAUDE_CADASTRO"))
            .when().get("/api/receituarios-especiais/1/1")
            .then().statusCode(200)
                .body("numero", equalTo(1))
                .body("pacienteCodigo", equalTo(500))
                .body("itens", hasSize(1));
    }

    @Test void copyClonesWithNewNumber() { // R31
        given().spec(asUser("SAUDE_CADASTRO")).body(validBody()).when().post("/api/receituarios-especiais").then().statusCode(201);
        given().spec(asUser("SAUDE_CADASTRO"))
            .when().post("/api/receituarios-especiais/1/1/copia")
            .then().statusCode(201).body("numero", equalTo(2)).body("itens", hasSize(1));
    }

    @Test void blocksDeleteForRetention() { // R29 / Portaria 344/98 (OQ2)
        given().spec(asUser("SAUDE_CADASTRO")).body(validBody()).when().post("/api/receituarios-especiais").then().statusCode(201);
        given().spec(asUser("SAUDE_CADASTRO"))
            .when().delete("/api/receituarios-especiais/1/1")
            .then().statusCode(409);
        // still there
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/receituarios-especiais/1/1").then().statusCode(200);
    }
}
