package br.gov.mandaguari.saude.especialidade;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Proves the per-program RBAC wiring for /api/especialidades once enforcement is flipped on
 * ({@code rbac.enforcement=enforce}) — the pure {@code pisauthorized("SAU_ESP", mode)} verdict from the
 * live-style {@code SAU_PRFCON} matrix governs each operation, deny-by-default. Its sibling
 * {@link EspecialidadeControllerIT} covers the default {@code shadow} mode (coarse role still decides).
 *
 * <p>Tokens carry a numeric {@code UsuCod} subject (see {@link #asUserId}); the coarse role in the token
 * is irrelevant under enforce. Synthetic data only — the specialty catalog carries no PHI.
 */
@TestPropertySource(properties = "rbac.enforcement=enforce")
class EspecialidadeRbacEnforceIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM SAU_PROESP");
        jdbc.update("DELETE FROM SAU_ESP");
        jdbc.update("DELETE FROM SAU_PRFCON");
        jdbc.update("DELETE FROM SAU_USU");
        jdbc.update("DELETE FROM SAU_PRF");

        jdbc.update("INSERT INTO SAU_ESP (EspCod, EspNom) VALUES (1, 'Cardiologia')");
        jdbc.update("INSERT INTO SAU_ESP (EspCod, EspNom) VALUES (2, 'Pediatria')");

        // profile 2 may CON+ALT on SAU_ESP, but NOT INC/EXC — mirrors a real SAU_PRFCON grant
        jdbc.update("INSERT INTO SAU_PRF (PrfCod, PrfNom) VALUES (2, 'CADASTRO_ESP')");
        jdbc.update("INSERT INTO SAU_PRFCON (PrfCod, PrfPrgCod, PrfPrgCon, PrfPrgInc, PrfPrgAlt, PrfPrgExc) "
                + "VALUES (2, 'SAU_ESP', 1, 0, 1, 0)");

        // user 20 = profile 2 (partial rights); user 10 = SYSMAR (all); user 40 = no permission rows
        jdbc.update("INSERT INTO SAU_USU (UsuCod, UsuLogin, UsuSysmar, UsuPrfCod, UsuBloq) VALUES (20, 'esp', false, 2, 0)");
        jdbc.update("INSERT INTO SAU_USU (UsuCod, UsuLogin, UsuSysmar, UsuBloq) VALUES (10, 'root', true, 0)");
        jdbc.update("INSERT INTO SAU_USU (UsuCod, UsuLogin, UsuSysmar, UsuBloq) VALUES (40, 'semperm', false, 0)");
    }

    @Test
    void anonymousIs401() {
        given().spec(anonymous()).when().get("/api/especialidades").then().statusCode(401);
    }

    @Test
    void nonNumericPrincipalIsDenied() {
        // legacy coarse-role token (sub="tester") can never resolve a UsuCod → deny-by-default
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/especialidades").then().statusCode(403);
    }

    @Test
    void numericUserWithoutPermissionIsForbidden() {
        given().spec(asUserId(40)).when().get("/api/especialidades").then().statusCode(403);
    }

    @Test
    void profileGrantsConAndAlt() {
        given().spec(asUserId(20)).when().get("/api/especialidades").then().statusCode(200);      // CON=1
        given().spec(asUserId(20)).body(Map.of("nome", "Pediatria Geral"))
            .when().put("/api/especialidades/2").then().statusCode(200);                            // ALT=1
    }

    @Test
    void profileDeniesIncAndExc() {
        given().spec(asUserId(20)).body(Map.of("codigo", 30, "nome", "Nova"))
            .when().post("/api/especialidades").then().statusCode(403);                             // INC=0
        given().spec(asUserId(20)).when().delete("/api/especialidades/2").then().statusCode(403);   // EXC=0
    }

    @Test
    void sysmarUserBypassesMatrix() {
        given().spec(asUserId(10)).when().delete("/api/especialidades/2").then().statusCode(204);   // R2 bypass
    }
}
