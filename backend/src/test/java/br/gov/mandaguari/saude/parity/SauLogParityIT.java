package br.gov.mandaguari.saude.parity;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Golden-master parity for SAU_LOG vs the legacy GeneXus write path ({@code psau_inc_log}). Runs only
 * under the {@code parity} Maven profile ({@code mvn -Pparity test}).
 *
 * <p><b>No legacy HTTP screen:</b> SAU_LOG has no public CRUD UI — parity here is over the WRITE
 * CONTRACT (the audit row produced by a committed mutation), not an HTTP response. The
 * {@code parity-verifier} agent captures, per scenario, the SAU_LOG row the LEGACY app writes for a
 * given mutation (op/tab/key/usuario/empresa/dat) into
 * {@code src/test/resources/parity/SAU_LOG/<scenario>.json}; these tests replay the same mutation on
 * the modern app and assert BUSINESS equivalence of the resulting audit row — with two INTENTIONAL
 * divergences that must be asserted as improvements, not failures:
 * <ul>
 *   <li>R7: modern writes the REAL prof/unit ids (or NULL), never the legacy hardcoded 0;</li>
 *   <li>R8/LGPD: modern leaves patient/professional NAMES + history NULL (legacy wrote '').</li>
 * </ul>
 *
 * <p>See SLICE-SPEC SAU_LOG {@code parity.scenarios}. Stubs are visible (not silently missing) so the
 * capture gap is tracked.
 */
@Tag("parity")
class SauLogParityIT extends AbstractIntegrationTest {

    // TODO(parity): run `/verify-parity SAU_LOG` to capture legacy SAU_LOG rows, then implement these.

    // scenario: after INSERT in a migrated slice (e.g. SAU_ESP) → 1 row LogOpe=INS, LogTab=<program>,
    // LogKey=<pk str>, LogUsuCod=<actor>, LogEmpCod=<tenant>, LogDat≈now.
    @Test @Disabled("TODO: capture legacy SAU_LOG row for an INSERT mutation")
    void insertEmitsEquivalentAuditRow() {}

    // scenario: after UPDATE → LogOpe=UPD.
    @Test @Disabled("TODO: capture legacy SAU_LOG row for an UPDATE mutation")
    void updateMapsToUpd() {}

    // scenario: after DELETE → LogOpe=DLT.
    @Test @Disabled("TODO: capture legacy SAU_LOG row for a DELETE mutation")
    void deleteMapsToDlt() {}

    // scenario (INTENTIONAL divergence, LGPD improvement): modern audit row carries NO patient/
    // professional name (PHI columns NULL) where legacy wrote ''. Assert equivalence ignoring this.
    @Test @Disabled("TODO: capture legacy row; assert modern omits PHI names as an improvement")
    void auditRowOmitsPhiNamesVsLegacy() {}

    // scenario (INTENTIONAL divergence, R7 fix): logpropescod/logunicod carry REAL ids when known,
    // not the legacy hardcoded 0.
    @Test @Disabled("TODO: capture legacy row; assert modern writes real prof/unit ids not 0")
    void realProfAndUnitIdsNotZeroVsLegacy() {}

    // scenario (R11 improvement): the trail is append-only — no route updates/deletes SAU_LOG rows.
    @Test @Disabled("TODO: assert no API path mutates SAU_LOG (legacy CRUD not reproduced)")
    void trailIsAppendOnlyVsLegacy() {}

    // scenario (R6): two events in the same tick for same tenant/user/key → BOTH persist (no PK loss).
    @Test @Disabled("TODO: capture/compare same-instant double event; both rows survive")
    void sameInstantEventsBothPersistVsLegacy() {}
}
