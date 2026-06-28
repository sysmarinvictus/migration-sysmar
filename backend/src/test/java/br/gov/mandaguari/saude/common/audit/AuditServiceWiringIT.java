package br.gov.mandaguari.saude.common.audit;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cross-cutting wiring proof: calling the shared {@link AuditService} (the same bean every migrated
 * slice — SAU_ESP, SAU_IMP, SAU_PRO… — already calls) actually writes a real SAU_LOG row through the
 * full Spring/JPA/REQUIRES_NEW path into Postgres. This closes the deferred-audit gap for ALL slices
 * at once, so a green run here means every existing slice's mutations now leave an audit trail.
 *
 * <p>Tenant is {@code audit.empresa-codigo=1} from the test profile. Actor resolves to the configured
 * default (the test SecurityContext has no numeric principal). Synthetic key only — no PHI.
 */
class AuditServiceWiringIT extends AbstractIntegrationTest {

    @Autowired AuditService auditService;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM SAU_LOG");
    }

    @Test
    void recordPersistsRealSauLogRowForTenantOne() {
        auditService.record("CREATE", "SAU_ESP", 4242);

        // REQUIRES_NEW already committed the row; read it straight back from the physical table.
        Map<String, Object> r = jdbc.queryForMap(
                "SELECT logempcod, logope, logtab, trim(logkey) AS key, logusucod, usucod, "
                + "       lognomepaciente, logpacpescod, loghistorico "
                + "FROM SAU_LOG WHERE logtab='SAU_ESP' AND trim(logkey)='4242'");

        assertThat(r.get("logempcod")).isEqualTo(1);     // empresa-codigo=1 (test config)
        assertThat(r.get("logope")).isEqualTo("INS");    // R4
        assertThat(r.get("key")).isEqualTo("4242");      // R5 stringified key, CHAR round-trip
        assertThat(r.get("usucod")).isEqualTo(r.get("logusucod")); // R1/OQ7 duplicate
        // LGPD (R8): no PHI / free text written.
        assertThat(r.get("lognomepaciente")).isNull();
        assertThat(r.get("logpacpescod")).isNull();
        assertThat(r.get("loghistorico")).isNull();
    }
}
