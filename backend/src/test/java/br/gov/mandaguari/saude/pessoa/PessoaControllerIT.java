package br.gov.mandaguari.saude.pessoa;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Full-stack tests for /api/pessoas (read-only person-resolution over SYS_PES). Social-name rule (R2/R3),
 * search/lookup, auth, and the secret/quarantine guarantee. Synthetic person data only.
 */
class PessoaControllerIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM SYS_PES WHERE PesCod IN (5001,5002)");
        jdbc.update("INSERT INTO SYS_PES (PesCod, PesNom, PesNomSoc, PesUsaNomSoc, PesCPFCNPJ, PesNumCns, PesSex) "
                + "VALUES (5001, 'MARIA REGISTRO', 'MARIA SOCIAL', true, '01111111294', '700000000000021', 'F')");
        jdbc.update("INSERT INTO SYS_PES (PesCod, PesNom, PesUsaNomSoc, PesCPFCNPJ) "
                + "VALUES (5002, 'JOAO SILVA', false, '52998224725')");
    }

    @Test
    void requiresAuthAndRole() {
        given().spec(anonymous()).when().get("/api/pessoas/5001").then().statusCode(401);
        given().spec(asUser("OUTRA_ROLE")).when().get("/api/pessoas/5001").then().statusCode(403);
    }

    @Test
    void getHonorsSocialName() { // R2/R3
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/pessoas/5001")
            .then().statusCode(200)
                .body("nome", equalTo("MARIA REGISTRO"))
                .body("usaNomeSocial", equalTo(true))
                .body("nomeExibicao", equalTo("MARIA SOCIAL"))
                .body("nomeCompleto", equalTo("MARIA SOCIAL (MARIA REGISTRO)"))
                .body("cpfCnpj", equalTo("01111111294"));     // char(18) trimmed
    }

    @Test
    void get404() {
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/pessoas/99999")
            .then().statusCode(404).body("title", notNullValue());
    }

    @Test
    void searchByRegistryAndSocialName() {
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/pessoas/search?nome=silva")
            .then().statusCode(200).body("content.id", hasItem(5002));
        // matches on the social name too
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/pessoas/search?nome=social")
            .then().statusCode(200).body("content.id", hasItem(5001));
    }

    @Test
    void searchByCpf() {
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/pessoas/search?cpf=01111111294")
            .then().statusCode(200).body("content.id", hasItem(5001));
    }

    @Test
    void lookupReturnsDisplayName() {
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/pessoas/lookup?q=maria")
            .then().statusCode(200).body("[0].nomeExibicao", equalTo("MARIA SOCIAL"));
    }

    @Test
    void responseNeverExposesSecrets() {
        String body = given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/pessoas/5001")
            .then().statusCode(200).extract().asString();
        org.assertj.core.api.Assertions.assertThat(body.toLowerCase())
                .doesNotContain("senha").doesNotContain("pessenha");
    }
}
