package br.gov.mandaguari.saude.parity;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Golden-master parity stubs for Posologia (SAU_REMOBS).
 * Enable by running {@code mvn -Pparity verify} once a reachable legacy instance
 * and a non-production DB snapshot are available (see SLICE-SPEC parity block).
 */
@Tag("parity")
@Disabled("Parity deferred — no reachable legacy instance (see SLICE-SPEC SAU_REMOBS open questions)")
class PosologiaParityIT {

    @Test void listDefault() {}
    @Test void getByIdExisting() {}
    @Test void getByIdNotFound() {}
    @Test void insertValidAutoCode() {}
    @Test void insertMissingDescricao() {}
    @Test void updateDescricao() {}
    @Test void deleteUnused() {}
    @Test void deleteReferencedByRemposo() {}
    @Test void deleteReferencedByRecesp1() {}
    @Test void lookupByDescricao() {}
}
