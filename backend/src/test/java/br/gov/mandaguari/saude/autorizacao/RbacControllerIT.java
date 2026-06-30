package br.gov.mandaguari.saude.autorizacao;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Full-stack tests for the RBAC cluster — program catalog (SAU_PRG/SAU_PRGGRP) + the permission grids
 * (SAU_PRFCON/SAU_USUCON, composite PKs) + the R8 resolver — HTTP → service → JPA → PostgreSQL
 * (Testcontainers). Proves composite-PK persistence and end-to-end precedence resolution. Synthetic data.
 */
class RbacControllerIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM SAU_PRFCON");
        jdbc.update("DELETE FROM SAU_USUCON");
        jdbc.update("DELETE FROM SAU_USU");
        jdbc.update("DELETE FROM SAU_PRG");
        jdbc.update("DELETE FROM SAU_PRGGRP");
        jdbc.update("DELETE FROM SAU_PRF");

        jdbc.update("INSERT INTO SAU_PRGGRP (GrpCod, GrpNom) VALUES (1, 'ATENDIMENTO')");
        jdbc.update("INSERT INTO SAU_PRG (PrgCod, PrgNom, GrpCod, PrgAdm, PrgMed, PrgAcessoPub) "
                + "VALUES ('ATEMED', 'Atendimento médico', 1, 0, 1, false)");
        jdbc.update("INSERT INTO SAU_PRG (PrgCod, PrgNom, GrpCod) VALUES ('AGENDA', 'Agenda', 1)");
        jdbc.update("INSERT INTO SAU_PRF (PrfCod, PrfNom) VALUES (2, 'ENFERMEIRO')");

        // users: 10 = SYSMAR; 20 = profile 2; 30 = no profile (per-user tier)
        jdbc.update("INSERT INTO SAU_USU (UsuCod, UsuLogin, UsuSysmar, UsuBloq) VALUES (10, 'root', true, 0)");
        jdbc.update("INSERT INTO SAU_USU (UsuCod, UsuLogin, UsuSysmar, UsuPrfCod, UsuBloq) VALUES (20, 'enf', false, 2, 0)");
        jdbc.update("INSERT INTO SAU_USU (UsuCod, UsuLogin, UsuSysmar, UsuBloq) VALUES (30, 'avulso', false, 0)");

        // profile 2 may CON+ALT on ATEMED (not INC/EXC); user 30 may CON on AGENDA
        jdbc.update("INSERT INTO SAU_PRFCON (PrfCod, PrfPrgCod, PrfPrgCon, PrfPrgInc, PrfPrgAlt, PrfPrgExc) "
                + "VALUES (2, 'ATEMED', 1, 0, 1, 0)");
        jdbc.update("INSERT INTO SAU_USUCON (UsuCod, PrgCod, UsuCon, UsuInc, UsuAlt, UsuExc) "
                + "VALUES (30, 'AGENDA', 1, 0, 0, 0)");
    }

    // ── program catalog ───────────────────────────────────────────────────────
    @Test
    void programas_requireAuthAndRole() {
        given().spec(anonymous()).when().get("/api/programas").then().statusCode(401);
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/programas").then().statusCode(403);
    }

    @Test
    void listsProgramasAndGrupos() {
        given().spec(asUser("SAUDE_ADMIN")).when().get("/api/programas?q=ate")
            .then().statusCode(200).body("content.id", hasItem("ATEMED"));
        given().spec(asUser("SAUDE_ADMIN")).when().get("/api/programas/grupos")
            .then().statusCode(200).body("nome", hasItem("ATENDIMENTO"));
    }

    @Test
    void createsAndBlocksDeleteWhenReferenced() {
        given().spec(asUser("SAUDE_ADMIN"))
            .body(Map.of("id", "NOVOPRG", "nome", "Novo", "admin", true))
            .when().post("/api/programas")
            .then().statusCode(201).body("admin", equalTo(true));
        // ATEMED is referenced by SAU_PRFCON → delete blocked
        given().spec(asUser("SAUDE_ADMIN")).when().delete("/api/programas/ATEMED").then().statusCode(409);
        // NOVOPRG is unreferenced → deletable
        given().spec(asUser("SAUDE_ADMIN")).when().delete("/api/programas/NOVOPRG").then().statusCode(204);
    }

    @Test
    void programaLookupIsAuthenticatedOnly() {
        given().spec(anonymous()).when().get("/api/programas/lookup?q=ate").then().statusCode(401);
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/programas/lookup?q=ate")
            .then().statusCode(200).body("[0].id", notNullValue());
    }

    // ── resolver (R8 precedence) end-to-end ─────────────────────────────────────
    @Test
    void sysmarUserIsGrantedEverything() {
        check(10, "ATEMED", "EXC").body("granted", equalTo(true));
        check(10, "AGENDA", "ALT").body("granted", equalTo(true));
    }

    @Test
    void profileUserUsesPrfconPerMode() {
        check(20, "ATEMED", "CON").body("granted", equalTo(true));   // prfcon con=1
        check(20, "ATEMED", "ALT").body("granted", equalTo(true));   // prfcon alt=1
        check(20, "ATEMED", "INC").body("granted", equalTo(false));  // prfcon inc=0
        // profile precedence: user 20 has no AGENDA prfcon row → denied (does NOT fall through to per-user)
        check(20, "AGENDA", "CON").body("granted", equalTo(false));
    }

    @Test
    void noProfileUserUsesUsucon() {
        check(30, "AGENDA", "CON").body("granted", equalTo(true));   // usucon con=1
        check(30, "AGENDA", "ALT").body("granted", equalTo(false));  // usucon alt=0
        check(30, "ATEMED", "CON").body("granted", equalTo(false));  // no usucon row → deny
    }

    // ── grids: composite-PK upsert + read ───────────────────────────────────────
    @Test
    void upsertsPerfilPermissionAndResolverReflectsIt() {
        // grant profile 2 → CON on AGENDA (new composite-PK row)
        given().spec(asUser("SAUDE_ADMIN"))
            .body(Map.of("consultar", true, "incluir", false, "alterar", false, "excluir", false))
            .when().put("/api/autorizacao/perfis/2/permissoes/AGENDA")
            .then().statusCode(200).body("consultar", equalTo(true));
        // persisted + readable
        given().spec(asUser("SAUDE_ADMIN")).when().get("/api/autorizacao/perfis/2/permissoes")
            .then().statusCode(200).body("programaId", hasItem("AGENDA"));
        // resolver now grants user 20 (profile 2) CON on AGENDA
        check(20, "AGENDA", "CON").body("granted", equalTo(true));
    }

    @Test
    void readsUsuarioPermissionGrid() {
        given().spec(asUser("SAUDE_ADMIN")).when().get("/api/autorizacao/usuarios/30/permissoes")
            .then().statusCode(200).body("programaId", hasItem("AGENDA"));
    }

    @Test
    void upsertsUsuarioPermissionAndResolverReflectsIt() {
        // user 30 (no profile) initially cannot ALT AGENDA (usucon alt=0)
        check(30, "AGENDA", "ALT").body("granted", equalTo(false));
        // grant ALT via per-user upsert (existing composite-PK row updated)
        given().spec(asUser("SAUDE_ADMIN"))
            .body(Map.of("consultar", true, "incluir", false, "alterar", true, "excluir", false))
            .when().put("/api/autorizacao/usuarios/30/permissoes/AGENDA")
            .then().statusCode(200).body("alterar", equalTo(true));
        // resolver now grants it (per-user fallback tier)
        check(30, "AGENDA", "ALT").body("granted", equalTo(true));
        // and a brand-new (user, program) row can be created via upsert
        given().spec(asUser("SAUDE_ADMIN"))
            .body(Map.of("consultar", true, "incluir", false, "alterar", false, "excluir", false))
            .when().put("/api/autorizacao/usuarios/30/permissoes/ATEMED")
            .then().statusCode(200);
        check(30, "ATEMED", "CON").body("granted", equalTo(true));
    }

    @Test
    void autorizacaoRequiresAdmin() {
        given().spec(asUser("SAUDE_CADASTRO")).when()
            .get("/api/autorizacao/check?usuCod=10&programaId=ATEMED&mode=CON").then().statusCode(403);
        given().spec(anonymous()).when()
            .get("/api/autorizacao/check?usuCod=10&programaId=ATEMED&mode=CON").then().statusCode(401);
    }

    private io.restassured.response.ValidatableResponse check(int usuCod, String prg, String mode) {
        return given().spec(asUser("SAUDE_ADMIN")).when()
                .get("/api/autorizacao/check?usuCod={u}&programaId={p}&mode={m}", usuCod, prg, mode)
                .then().statusCode(200);
    }
}
