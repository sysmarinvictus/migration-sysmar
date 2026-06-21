package br.gov.mandaguari.saude.parity;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Golden-master parity stubs for SAU_UNISETOR vs the legacy GeneXus sau_unisetor transaction.
 * Enable with -Pparity and set LEGACY_BASE_URL env var. See SLICE-SPEC SAU_UNISETOR.
 */
@Disabled("parity profile only — requires running legacy app")
class UniSetorParityIT extends AbstractIntegrationTest {

    @Test void listParityMatchesLegacy() {}
    @Test void getParityMatchesLegacy() {}
    @Test void createValidParityMatchesLegacy() {}
    @Test void createDuplicateKeyReturnsConflictParity() {}
    @Test void createRejectsNonExistentUnidadeParity() {}
    @Test void updateNomeParityMatchesLegacy() {}
    @Test void updateSituacaoInativoParity() {}
    @Test void deleteUnreferencedParityMatchesLegacy() {}
    @Test void deleteBlockedBySauPar5Parity() {}
    @Test void deleteBlockedBySauUsuUni1Parity() {}
    @Test void deleteBlockedBySauRemLotParity() {}
    @Test void deleteBlockedBySauRemUnisetorParity() {}
    @Test void nomeStoredAsUppercaseParity() {}
    @Test void lookupFilteredByUnidadeParity() {}
}
