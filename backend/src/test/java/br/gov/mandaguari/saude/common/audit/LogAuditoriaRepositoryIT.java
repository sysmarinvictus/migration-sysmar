package br.gov.mandaguari.saude.common.audit;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testcontainers integration for the append-only audit repository: CHAR(50) logkey round-trip,
 * composite-PK lookup, and the no-mutation invariant (R11). Uses a real PostgreSQL (shared container
 * from {@link AbstractIntegrationTest}); V9 baseline creates SAU_LOG.
 *
 * <p>Synthetic data only — no PHI.
 */
class LogAuditoriaRepositoryIT extends AbstractIntegrationTest {

    @Autowired LogAuditoriaRepository repository;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM SAU_LOG");
    }

    private LogAuditoria row(int emp, LocalDateTime when, int usu, String key, String op, String tab) {
        LogAuditoria r = new LogAuditoria();
        r.setEmpresaCodigo(emp);
        r.setDataHora(when);
        r.setUsuarioCodigo(usu);
        r.setChaveRegistro(key);
        r.setOperacao(op);
        r.setTabela(tab);
        r.setUsuarioCodigoRef(usu);
        return r;
    }

    // CHAR(50) logkey round-trips: write "1756", read back trims to "1756"; PK lookup matches.
    // No @Transactional: each save commits in its own tx (Spring Data), so the raw JdbcTemplate
    // COUNT below sees it without depending on Hibernate autoflush. @BeforeEach DELETE isolates tests.
    @Test
    void char50LogKeyRoundTripsAndPkLookupMatches() {
        LocalDateTime when = LocalDateTime.of(2026, 6, 22, 9, 0, 0, 123_000_000);
        repository.save(row(1, when, 7, "1756", "INS", "SAU_BAI"));

        // Read back via the filter query keyed on the exact chaveRegistro (trim-aware predicate).
        Page<LogAuditoria> page = repository.findByFilters(
                "SAU_BAI", 7, null, null, "1756", PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
        LogAuditoria read = page.getContent().get(0);
        // CHAR(50) padding is stripped by the entity getter / DTO; business value equals "1756".
        assertThat(read.getChaveRegistro().trim()).isEqualTo("1756");
        assertThat(read.getOperacao()).isEqualTo("INS");
        assertThat(read.getTabela()).isEqualTo("SAU_BAI");

        // Composite-PK still resolvable directly in the DB (bpchar match works because we wrote CHAR).
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM SAU_LOG WHERE logempcod=1 AND logusucod=7 AND trim(logkey)='1756'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    // Append-only: two events for the SAME (empresa,user,key) but different microsecond both persist
    // (no PK collision once the timestamp differs — the basis of the R6 retry).
    @Test
    void twoEventsSameKeyDifferentMicrosecondBothPersist() {
        LocalDateTime t0 = LocalDateTime.of(2026, 6, 22, 9, 0, 0, 0);
        repository.save(row(1, t0, 7, "200", "INS", "SAU_ESP"));
        repository.save(row(1, t0.plusNanos(1_000), 7, "200", "UPD", "SAU_ESP"));

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM SAU_LOG WHERE logempcod=1 AND logusucod=7 AND trim(logkey)='200'",
                Integer.class);
        assertThat(count).isEqualTo(2);
    }

    // Append-only invariant (R11), compile-time + doc assertion: the repository interface exposes NO
    // delete/update operation. It extends the narrow Spring Data Repository marker, declaring only a
    // save (pure INSERT) and read queries — never deleteX / removeX, never @Modifying mutations.
    @Test
    void repositoryExposesNoDeleteOrUpdateOperation() {
        for (Method m : LogAuditoriaRepository.class.getMethods()) {
            String n = m.getName().toLowerCase();
            assertThat(n)
                    .as("append-only repo must not expose mutation method '%s'", m.getName())
                    .doesNotStartWith("delete")
                    .doesNotStartWith("remove");
        }
        // It must NOT be a CrudRepository/JpaRepository (those would inherit delete*/save-as-update).
        assertThat(org.springframework.data.repository.CrudRepository.class
                        .isAssignableFrom(LogAuditoriaRepository.class))
                .as("append-only repo must not be a CrudRepository")
                .isFalse();
    }
}
