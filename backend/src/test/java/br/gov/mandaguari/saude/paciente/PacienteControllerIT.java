package br.gov.mandaguari.saude.paciente;

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
 * Full-stack tests for the Paciente slice (/api/pacientes) — HTTP → service → JPA → PostgreSQL
 * (Testcontainers). Confirms the composite write (SYS_PES person + SAU_PAC subtype), the Portaria
 * 344/98 delete-guard (SAU_RECESP), and hard-delete-keeps-person. Synthetic PHI only.
 */
class PacienteControllerIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    static final String CNS_OK = "700000000000005";
    static final String CNS_OK2 = "700000000000013";

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM SAU_RECESP");
        jdbc.update("DELETE FROM SAU_PAC");
        jdbc.update("DELETE FROM SYS_PES");
        jdbc.update("INSERT INTO SAU_UNI (UniCod) VALUES (1) ON CONFLICT (UniCod) DO NOTHING");
        jdbc.update("INSERT INTO SAU_BAI (BaiCod, BaiNom) VALUES (1, 'Centro') ON CONFLICT (BaiCod) DO NOTHING");
        jdbc.update("INSERT INTO SYS_MUN (MunCod, MunNom) VALUES (411420, 'Mandaguari') ON CONFLICT (MunCod) DO NOTHING");
    }

    private Map<String, Object> validBody() {
        Map<String, Object> b = new HashMap<>();
        b.put("nome", "Maria Silva");
        b.put("nomeMae", "Ana Silva");
        b.put("cns", CNS_OK);
        b.put("dataNascimento", "1990-01-01");
        b.put("sexo", "F");
        b.put("alergia", "Dipirona");
        return b;
    }

    // ---- auth ----

    @Test void requiresAuthentication() {
        given().spec(anonymous()).body(validBody()).when().post("/api/pacientes").then().statusCode(401);
    }

    @Test void forbidsWrongRole() {
        given().spec(asUser("OUTRA_ROLE")).body(validBody()).when().post("/api/pacientes").then().statusCode(403);
    }

    // ---- create composite ----

    @Test void createsPersonAndPatient() { // SYS_PES + SAU_PAC, R6/R7 defaults
        int id = given().spec(asUser("SAUDE_CADASTRO")).body(validBody())
            .when().post("/api/pacientes")
            .then().statusCode(201)
                .header("Location", containsString("/api/pacientes/1"))
                .body("id", equalTo(1))
                .body("nome", equalTo("Maria Silva"))
                .body("obito", equalTo(0))
                .body("situacao", equalTo(1))
                .body("alergia", equalTo("Dipirona"))
            .extract().path("id");

        Integer pes = jdbc.queryForObject("SELECT count(*) FROM SYS_PES WHERE PesCod = ?", Integer.class, (long) id);
        Integer pac = jdbc.queryForObject("SELECT count(*) FROM SAU_PAC WHERE PacPesCod = ?", Integer.class, (long) id);
        org.assertj.core.api.Assertions.assertThat(pes).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(pac).isEqualTo(1);
    }

    @Test void rejectsMissingCpfAndCns() { // R2
        Map<String, Object> b = validBody();
        b.remove("cns");
        given().spec(asUser("SAUDE_CADASTRO")).body(b)
            .when().post("/api/pacientes")
            .then().statusCode(422).body("code", equalTo("pac.documento.required"));
    }

    @Test void rejectsInvalidCns() { // R4
        Map<String, Object> b = validBody();
        b.put("cns", "123456789012345");
        given().spec(asUser("SAUDE_CADASTRO")).body(b)
            .when().post("/api/pacientes")
            .then().statusCode(422).body("code", equalTo("pac.cns.invalid"));
    }

    @Test void rejectsDuplicateCnsAmongPatients() { // R3
        given().spec(asUser("SAUDE_CADASTRO")).body(validBody()).when().post("/api/pacientes").then().statusCode(201);
        Map<String, Object> b2 = validBody();  // same CNS
        given().spec(asUser("SAUDE_CADASTRO")).body(b2).when().post("/api/pacientes").then().statusCode(409);
    }

    // ---- read / update ----

    @Test void searchByCpfNormalizesStoredFormatting() { // D1 parity fix — PesCPFCNPJ stored formatted
        // Seed a person whose CPF is stored FORMATTED, as in the live snapshot.
        jdbc.update("INSERT INTO SYS_PES (PesCod, PesNom, PesCPFCNPJ) VALUES (900001, 'Formatado Silva', '412.867.079-00')");
        jdbc.update("INSERT INTO SAU_PAC (PacPesCod, PacSit) VALUES (900001, 1)");
        // Searching by RAW digits must still find it (both sides normalized to digits).
        given().spec(asUser("SAUDE_CADASTRO")).queryParam("cpf", "41286707900")
            .when().get("/api/pacientes")
            .then().statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].id", equalTo(900001));
    }

    @Test void rejectsDuplicateCpfStoredFormatted() { // D1 write-path: R17 uniqueness must see formatted CPF
        // An existing person whose CPF is stored FORMATTED (as in the live snapshot).
        jdbc.update("INSERT INTO SYS_PES (PesCod, PesNom, PesCPFCNPJ) VALUES (900002, 'Existente', '529.982.247-25')");
        // Creating a patient with the SAME CPF (raw digits) must be rejected as a duplicate (409), not allowed.
        Map<String, Object> b = validBody();
        b.remove("cns");
        b.put("cpfCnpj", "52998224725");
        given().spec(asUser("SAUDE_CADASTRO")).body(b)
            .when().post("/api/pacientes")
            .then().statusCode(409);
    }

    @Test void searchesByNome() {
        given().spec(asUser("SAUDE_CADASTRO")).body(validBody()).when().post("/api/pacientes").then().statusCode(201);
        given().spec(asUser("SAUDE_CADASTRO")).queryParam("nome", "Maria")
            .when().get("/api/pacientes")
            .then().statusCode(200).body("content", hasSize(1)).body("content[0].nome", equalTo("Maria Silva"));
    }

    @Test void updatesPatientAndPerson() {
        int id = given().spec(asUser("SAUDE_CADASTRO")).body(validBody())
            .when().post("/api/pacientes").then().statusCode(201).extract().path("id");
        Map<String, Object> b = validBody();
        b.put("nome", "Maria Souza Silva");
        b.put("situacaoRua", true);
        given().spec(asUser("SAUDE_CADASTRO")).body(b)
            .when().put("/api/pacientes/" + id)
            .then().statusCode(200)
                .body("nome", equalTo("Maria Souza Silva"))
                .body("situacaoRua", equalTo(true));
    }

    // ---- delete + Portaria 344 guard ----

    @Test void deletesPatientKeepingPerson() { // R15
        int id = given().spec(asUser("SAUDE_CADASTRO")).body(validBody())
            .when().post("/api/pacientes").then().statusCode(201).extract().path("id");
        given().spec(asUser("SAUDE_CADASTRO")).when().delete("/api/pacientes/" + id).then().statusCode(204);
        // SAU_PAC gone, SYS_PES person kept
        Integer pac = jdbc.queryForObject("SELECT count(*) FROM SAU_PAC WHERE PacPesCod = ?", Integer.class, (long) id);
        Integer pes = jdbc.queryForObject("SELECT count(*) FROM SYS_PES WHERE PesCod = ?", Integer.class, (long) id);
        org.assertj.core.api.Assertions.assertThat(pac).isEqualTo(0);
        org.assertj.core.api.Assertions.assertThat(pes).isEqualTo(1);
    }

    @Test void blocksDeleteWithControlledPrescription() { // R14 Portaria 344/98
        int id = given().spec(asUser("SAUDE_CADASTRO")).body(validBody())
            .when().post("/api/pacientes").then().statusCode(201).extract().path("id");
        jdbc.update("INSERT INTO SAU_RECESP (RecEspUniCod, RecEspCod, PacPesCod) VALUES (1, 1, ?)", (long) id);

        given().spec(asUser("SAUDE_CADASTRO")).when().delete("/api/pacientes/" + id).then().statusCode(409);
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/pacientes/" + id).then().statusCode(200);
    }
}
