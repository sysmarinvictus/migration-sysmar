package br.gov.mandaguari.saude.parity;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Golden-master parity stubs for SAU_UNI vs the legacy GeneXus sau_uni transaction.
 * Enable with -Pparity and set LEGACY_BASE_URL env var. See SLICE-SPEC SAU_UNI §parity.
 */
@Disabled("parity profile only — requires running legacy app")
class UnidadeParityIT extends AbstractIntegrationTest {

    // ── main transaction ────────────────────────────────────────────────────
    @Test void listParityMatchesLegacy() {}
    @Test void getParityReturnsAllColumnsAndDerivedMunicipio() {}
    @Test void lookupFilteredByQueryParity() {}
    @Test void createValidMinimumParityMatchesLegacy() {}
    @Test void createBlankNomeReturns422Parity() {}
    @Test void createInvalidCnpjReturns422Parity() {}
    @Test void createInvalidPhoneReturns422Parity() {}
    @Test void createHiperdiaWithoutCnesReturns422Parity() {}
    @Test void createOrgEmiWithoutDiretorReturns422Parity() {}
    @Test void createSameDiretorAutorizadorReturns422Parity() {}   // R26
    @Test void updateNomeStoredUppercaseParity() {}
    @Test void updateSituacaoDesativadoParity() {}
    @Test void deleteUnreferencedReturns204Parity() {}
    @Test void deleteWithUnisetorChildrenReturns409Parity() {}
    @Test void deleteWithUsuUniReferencesReturns409Parity() {}
    @Test void deleteWithRem1ReferenceReturns409Parity() {}

    // ── sub-tables ───────────────────────────────────────────────────────────
    @Test void addHiperdiaProfissionalValidReturns201Parity() {}
    @Test void addHiperdiaProfissionalWithoutDatIncReturns422Parity() {}
    @Test void addSisPreNatalInvalidCboReturns422Parity() {}
    @Test void addSalaDuplicateCodReturns409Parity() {}
}
