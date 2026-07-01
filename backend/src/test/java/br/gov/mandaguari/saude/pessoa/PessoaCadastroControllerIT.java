package br.gov.mandaguari.saude.pessoa;

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
 * Full-stack integration tests for the SAU_PESF person-cadastro write path
 * (POST/PUT/DELETE /api/pessoas) — HTTP → service → JPA → PostgreSQL (Testcontainers).
 * Synthetic data only (SYS_PES is PHI — no real person is committed). Valid CNS/CPF are
 * check-digit-valid synthetic values; the delete-guard is exercised via a seeded SAU_PRO row.
 */
class PessoaCadastroControllerIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    static final String CNS_OK = "700000000000005";
    static final String CPF_OK = "11144477735";

    @BeforeEach
    void seed() {
        clean();
        // Shared reference tables live in the singleton container across IT classes → idempotent upsert.
        jdbc.update("INSERT INTO SAU_TIPLOG (TipLogCod, TipLogNom) VALUES (1, 'Rua') ON CONFLICT (TipLogCod) DO NOTHING");
        jdbc.update("INSERT INTO SAU_BAI (BaiCod, BaiNom) VALUES (1, 'Centro') ON CONFLICT (BaiCod) DO NOTHING");
        jdbc.update("INSERT INTO SYS_MUN (MunCod, MunNom) VALUES (411420, 'Mandaguari') ON CONFLICT (MunCod) DO NOTHING");
        jdbc.update("INSERT INTO SAU_CBOR (CborCod, CborDes) VALUES ('225125', 'Médico') ON CONFLICT (CborCod) DO NOTHING");
        jdbc.update("INSERT INTO SAU_ETN (EtnCod, EtnNom) VALUES (1, 'Guarani') ON CONFLICT (EtnCod) DO NOTHING");
        jdbc.update("INSERT INTO SAU_PAIS (PaisCod, PaisNom) VALUES (10, 'Brasil') ON CONFLICT (PaisCod) DO NOTHING");
        jdbc.update("INSERT INTO SAU_ORGEMI (OrgEmiCod, OrgEmiDes) VALUES (1, 'SSP') ON CONFLICT (OrgEmiCod) DO NOTHING");
    }

    void clean() {
        jdbc.update("DELETE FROM SAU_PRO");
        jdbc.update("DELETE FROM SAU_FUN");
        jdbc.update("DELETE FROM SAU_PAC");
        jdbc.update("DELETE FROM SYS_PES");
    }

    private Map<String, Object> validBody() {
        Map<String, Object> b = new HashMap<>();
        b.put("nome", "Maria Silva");
        b.put("nomeMae", "Ana Silva");
        b.put("cns", CNS_OK);
        b.put("dataNascimento", "1990-01-01");
        b.put("sexo", "F");
        b.put("corCod", 1);
        b.put("nacionalidadeTipo", 1);
        b.put("paisCod", 10);
        b.put("municipioNascCod", 411420);
        b.put("cep", "87100000");
        b.put("tipoLogradouroCod", 1);
        b.put("endereco", "Rua das Flores");
        b.put("enderecoNumero", "100");
        b.put("bairroCod", 1);
        b.put("municipioCod", 411420);
        return b;
    }

    // ---- auth ----

    @Test
    void requiresAuthentication() {
        given().spec(anonymous()).body(validBody()).when().post("/api/pessoas").then().statusCode(401);
    }

    @Test
    void forbidsWrongRole() {
        given().spec(asUser("OUTRA_ROLE")).body(validBody()).when().post("/api/pessoas").then().statusCode(403);
    }

    // ---- create ----

    @Test
    void createsValidPerson() { // R48/R49 + PK MAX+1 (empty table → 1)
        given().spec(asUser("SAUDE_CADASTRO")).body(validBody())
            .when().post("/api/pessoas")
            .then().statusCode(201)
                .header("Location", containsString("/api/pessoas/1"))
                .body("id", equalTo(1))
                .body("tipoPessoa", equalTo(2))
                .body("nome", equalTo("Maria Silva"))
                .body("dataCadastro", notNullValue());
    }

    @Test
    void rejectsInvalidCns() { // R43 → 422 with code
        Map<String, Object> b = validBody();
        b.put("cns", "123456789012345");
        given().spec(asUser("SAUDE_CADASTRO")).body(b)
            .when().post("/api/pessoas")
            .then().statusCode(422).body("code", equalTo("pes.cns.invalid"));
    }

    @Test
    void rejectsMissingBairro() { // R15 → 422
        Map<String, Object> b = validBody();
        b.remove("bairroCod");
        given().spec(asUser("SAUDE_CADASTRO")).body(b)
            .when().post("/api/pessoas")
            .then().statusCode(422).body("code", equalTo("pes.bairro.required"));
    }

    @Test
    void rejectsDuplicateCpf() { // R45 → 409
        Map<String, Object> b1 = validBody();
        b1.put("cpfCnpj", CPF_OK);
        given().spec(asUser("SAUDE_CADASTRO")).body(b1).when().post("/api/pessoas").then().statusCode(201);

        Map<String, Object> b2 = validBody();
        b2.put("cpfCnpj", CPF_OK);
        given().spec(asUser("SAUDE_CADASTRO")).body(b2)
            .when().post("/api/pessoas")
            .then().statusCode(409);
    }

    // ---- update ----

    @Test
    void updatesPerson() {
        int id = given().spec(asUser("SAUDE_CADASTRO")).body(validBody())
            .when().post("/api/pessoas").then().statusCode(201).extract().path("id");

        Map<String, Object> b = validBody();
        b.put("nome", "Maria Souza Silva");
        b.put("telefone", "(44) 3232-1010");
        given().spec(asUser("SAUDE_CADASTRO")).body(b)
            .when().put("/api/pessoas/" + id)
            .then().statusCode(200)
                .body("nome", equalTo("Maria Souza Silva"))
                .body("telefone", equalTo("(44) 3232-1010"));
    }

    // ---- delete + guards ----

    @Test
    void deletesUnreferencedPerson() {
        int id = given().spec(asUser("SAUDE_CADASTRO")).body(validBody())
            .when().post("/api/pessoas").then().statusCode(201).extract().path("id");

        given().spec(asUser("SAUDE_CADASTRO")).when().delete("/api/pessoas/" + id).then().statusCode(204);
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/pessoas/" + id).then().statusCode(404);
    }

    @Test
    void blocksDeleteWhenProfissionalExists() { // R53 → 409
        int id = given().spec(asUser("SAUDE_CADASTRO")).body(validBody())
            .when().post("/api/pessoas").then().statusCode(201).extract().path("id");
        jdbc.update("INSERT INTO SAU_PRO (ProPesCod, ProSit) VALUES (?, 1)", (long) id);

        given().spec(asUser("SAUDE_CADASTRO")).when().delete("/api/pessoas/" + id).then().statusCode(409);
        // still present
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/pessoas/" + id).then().statusCode(200);
    }
}
