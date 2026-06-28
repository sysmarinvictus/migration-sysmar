package br.gov.mandaguari.saude.common.audit;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Full-stack integration tests for GET /api/admin/auditoria — security, pagination, LGPD PHI
 * omission, the date-range (42P18) filter regression, and the self-audited read (R10).
 *
 * <p>Seed rows go straight into SAU_LOG via JdbcTemplate (synthetic values; one row carries a
 * non-null lognomepaciente to PROVE the DTO never leaks it). audit.empresa-codigo=1 in test config.
 */
class AuditoriaControllerIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 6, 22, 8, 0, 0);

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM SAU_LOG");
        // SAU_ESP rows for usuario 7 (two events) + one with PHI populated (must never surface).
        insert(BASE,            7, "INS", "SAU_ESP", "10", null);
        insert(BASE.plusHours(1), 7, "UPD", "SAU_ESP", "10",
                "Paciente Sintético da Silva");                 // lognomepaciente populated on purpose
        insert(BASE.plusHours(2), 9, "INS", "SAU_BAI", "1756", null);
    }

    private void insert(LocalDateTime when, int usu, String op, String tab, String key, String nomePac) {
        jdbc.update("INSERT INTO SAU_LOG "
                + "(logempcod, logdat, logusucod, logope, logtab, logkey, usucod, lognomepaciente, logpacpescod, loghistorico) "
                + "VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Timestamp.valueOf(when), usu, op, tab, key, usu, nomePac,
                nomePac != null ? 555L : null,
                nomePac != null ? "histórico sensível sintético" : null);
    }

    private long viewRowsOnSauLog() {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM SAU_LOG WHERE logope='VIE' AND logtab='SAU_LOG'", Long.class);
        return n == null ? 0 : n;
    }

    // Endpoint security: anonymous → 401.
    @Test
    void requiresAuthentication() {
        given().spec(anonymous()).when().get("/api/admin/auditoria").then().statusCode(401);
    }

    // Endpoint security: wrong role → 403 (R10 — reading the trail needs audit authority).
    @Test
    void forbidsNonAdminRole() {
        given().spec(asUser("SAUDE_CADASTRO")).when().get("/api/admin/auditoria").then().statusCode(403);
    }

    // ADMIN → 200, paginated (Spring Data page envelope).
    @Test
    void adminGetsPaginatedPage() {
        given().spec(asUser("ADMIN"))
            .when().get("/api/admin/auditoria")
            .then().statusCode(200)
                .body("content", hasSize(3))
                .body("totalElements", equalTo(3))
                .body("content[0].tabela", notNullValue());
    }

    // LGPD: the response NEVER contains PHI columns, even when the underlying row has them populated.
    @Test
    void responseNeverContainsPhiColumns() {
        String body = given().spec(asUser("ADMIN"))
            .when().get("/api/admin/auditoria")
            .then().statusCode(200)
                // DTO record has no such properties at all.
                .body("content.nomePaciente", everyItem(nullValue()))
                .body("content.pacienteCodigo", everyItem(nullValue()))
                .body("content.historico", everyItem(nullValue()))
                .body("content.nomeProfissional", everyItem(nullValue()))
                .body("content.situacao", everyItem(nullValue()))
            .extract().asString();
        // And the raw JSON never leaks the seeded patient name / history literals.
        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("Paciente Sintético da Silva")
                .doesNotContain("histórico sensível sintético")
                .doesNotContain("\"pacienteCodigo\"")
                .doesNotContain("\"historico\"");
    }

    // Filter by tabela.
    @Test
    void filtersByTabela() {
        given().spec(asUser("ADMIN")).queryParam("tabela", "SAU_BAI")
            .when().get("/api/admin/auditoria")
            .then().statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].tabela", equalTo("SAU_BAI"))
                .body("content[0].chaveRegistro", equalTo("1756"));
    }

    // Filter by usuarioCodigo.
    @Test
    void filtersByUsuarioCodigo() {
        given().spec(asUser("ADMIN")).queryParam("usuarioCodigo", 9)
            .when().get("/api/admin/auditoria")
            .then().statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].usuarioCodigo", equalTo(9));
    }

    // Filter by BOTH date bounds — the 42P18 regression guard (untyped NULL date param).
    @Test
    void filtersByDateRangeBothBounds() {
        // In-range window catches the two BASE..BASE+1h SAU_ESP rows, excludes the BASE+2h SAU_BAI row.
        given().spec(asUser("ADMIN"))
            .queryParam("dataHoraFrom", "2026-06-22T07:30:00")
            .queryParam("dataHoraTo",   "2026-06-22T09:30:00")
            .when().get("/api/admin/auditoria")
            .then().statusCode(200).body("content", hasSize(2));
        // Out-of-range window in the PAST excludes everything (predicate actually applied, not
        // ignored). A past window is used deliberately: the prior GET self-audits a VIEW row stamped
        // at test-time now(), so a FUTURE window could spuriously capture it.
        given().spec(asUser("ADMIN"))
            .queryParam("dataHoraFrom", "2000-01-01T00:00:00")
            .queryParam("dataHoraTo",   "2000-01-02T00:00:00")
            .when().get("/api/admin/auditoria")
            .then().statusCode(200).body("content", hasSize(0));
    }

    // Filter by chaveRegistro (trim-aware against CHAR(50)).
    @Test
    void filtersByChaveRegistro() {
        given().spec(asUser("ADMIN")).queryParam("chaveRegistro", "1756")
            .when().get("/api/admin/auditoria")
            .then().statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].chaveRegistro", equalTo("1756"));
    }

    // R10: the read itself is audited — a VIEW row on SAU_LOG appears after a GET.
    @Test
    void readIsSelfAudited() {
        long before = viewRowsOnSauLog();
        given().spec(asUser("ADMIN")).when().get("/api/admin/auditoria").then().statusCode(200);
        org.assertj.core.api.Assertions.assertThat(viewRowsOnSauLog())
                .as("a GET must persist one VIEW/SAU_LOG self-audit row")
                .isGreaterThan(before);
    }
}
