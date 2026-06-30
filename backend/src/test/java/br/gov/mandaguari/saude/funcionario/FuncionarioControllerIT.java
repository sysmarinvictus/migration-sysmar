package br.gov.mandaguari.saude.funcionario;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Full-stack tests for /api/funcionarios — HTTP → service → JPA → PostgreSQL (Testcontainers), exercising
 * the native SYS_PES projection/write-back and the mined rules. Synthetic, ZZ-style person data only.
 */
class FuncionarioControllerIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM SAU_USU");
        jdbc.update("DELETE FROM SAU_RECESP");
        jdbc.update("DELETE FROM SAU_FUN");
        jdbc.update("DELETE FROM SYS_PES WHERE PesCod IN (900,901,902)");
        jdbc.update("INSERT INTO SYS_PES (PesCod, PesNom) VALUES (900, 'JOAO FUNCIONARIO')");
        jdbc.update("INSERT INTO SYS_PES (PesCod, PesNom) VALUES (901, 'MARIA CONTROLADA')");
        jdbc.update("INSERT INTO SYS_PES (PesCod, PesNom) VALUES (902, 'PEDRO SEM FUNCAO')");
        jdbc.update("INSERT INTO SAU_FUN (FunPesCod, FunTraFon, FunSit) VALUES (900, '(44) 3232-3232', 1)");
        jdbc.update("INSERT INTO SAU_FUN (FunPesCod, FunSit) VALUES (901, 1)");
        // 900 is linked to a system user (R13 guard); 901 referenced by a controlled prescription (R14 guard)
        jdbc.update("INSERT INTO SAU_USU (UsuCod, UsuLogin, FunPesCod) VALUES (700, 'zzfun', 900)");
        // SAU_RECESP.RecEspUniCod has an FK to SAU_UNI → seed a minimal unidade (idempotent).
        jdbc.update("INSERT INTO SAU_UNI (UniCod) VALUES (1) ON CONFLICT (UniCod) DO NOTHING");
        jdbc.update("INSERT INTO SAU_RECESP (RecEspUniCod, RecEspCod, FunPesCod) VALUES (1, 1, 901)");
    }

    @Test
    void requiresAuthentication() {
        given().spec(anonymous()).when().get("/api/funcionarios").then().statusCode(401);
    }

    @Test
    void forbidsWrongRole() {
        given().spec(asUser("OUTRA_ROLE")).when().get("/api/funcionarios").then().statusCode(403);
    }

    @Test
    void listsAndSearchesByPersonName() {
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/funcionarios?nome=joao")
            .then().statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].id", equalTo(900))
                .body("content[0].nome", equalTo("JOAO FUNCIONARIO"));
    }

    @Test
    void getsByIdAnd404() {
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/funcionarios/900")
            .then().statusCode(200).body("nome", equalTo("JOAO FUNCIONARIO"));
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/funcionarios/9999")
            .then().statusCode(404).body("title", notNullValue());
    }

    @Test
    void createsForExistingPersonWithDefaultSituacao() { // R1 + R5
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("id", 902, "telefoneTrabalho", "(44) 3232-3232",
                         "pessoa", Map.of("nome", "PEDRO SEM FUNCAO")))
            .when().post("/api/funcionarios")
            .then().statusCode(201)
                .header("Location", containsString("/api/funcionarios/902"))
                .body("id", equalTo(902))
                .body("situacao", equalTo(1));
    }

    @Test
    void rejectsCreateForNonExistentPerson() { // R1
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("id", 99999, "pessoa", Map.of("nome", "FANTASMA")))
            .when().post("/api/funcionarios")
            .then().statusCode(422);
    }

    @Test
    void rejectsInvalidWorkPhone() { // R8
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("id", 902, "telefoneTrabalho", "123", "pessoa", Map.of("nome", "PEDRO")))
            .when().post("/api/funcionarios")
            .then().statusCode(422);
    }

    @Test
    void updatesAndWritesBackPersonName() { // R2
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("telefoneTrabalho", "(44) 3300-0000",
                         "pessoa", Map.of("nome", "JOAO EDITADO", "cpfCnpj", "01111111294")))
            .when().put("/api/funcionarios/900")
            .then().statusCode(200).body("nome", equalTo("JOAO EDITADO"));
        String name = jdbc.queryForObject("SELECT PesNom FROM SYS_PES WHERE PesCod = 900", String.class);
        org.assertj.core.api.Assertions.assertThat(name).isEqualTo("JOAO EDITADO");   // write-back persisted
    }

    @Test
    void blocksDeleteWhenReferencedByUsuario() { // R13
        given().spec(asUser("SAUDE_CADASTRO")).when().delete("/api/funcionarios/900")
            .then().statusCode(409);
    }

    @Test
    void blocksDeleteWhenReferencedByControlledPrescription() { // R14
        given().spec(asUser("SAUDE_CADASTRO")).when().delete("/api/funcionarios/901")
            .then().statusCode(409);
    }

    @Test
    void deletesUnreferenced() { // R15
        // 902 has no funcionário yet → create then delete (no SAU_USU/SAU_RECESP ref)
        given().spec(asUser("SAUDE_CADASTRO"))
            .body(Map.of("id", 902, "pessoa", Map.of("nome", "PEDRO SEM FUNCAO")))
            .when().post("/api/funcionarios").then().statusCode(201);
        given().spec(asUser("SAUDE_CADASTRO")).when().delete("/api/funcionarios/902")
            .then().statusCode(204);
    }

    @Test
    void lookupReturnsItemsAndRequiresRole() {
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/funcionarios/lookup?q=joao")
            .then().statusCode(200).body("[0].nome", equalTo("JOAO FUNCIONARIO"));
        given().spec(anonymous()).when().get("/api/funcionarios/lookup?q=joao").then().statusCode(401);
    }
}
