package br.gov.mandaguari.saude.parity;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Golden-master parity stubs for SAU_DIS vs the legacy GeneXus sau_dis transaction.
 * Enable with -Pparity and set LEGACY_BASE_URL env var. See SLICE-SPEC SAU_DIS.
 */
@Disabled("parity profile only — requires running legacy app")
class DistritoParityIT extends AbstractIntegrationTest {

    // Scenario 1: List all distritos — compare count and sample row against legacy work-with
    @Test void listParityMatchesLegacy() {}

    // Scenario 2: Get by codigo — field-by-field comparison (DisCod, DisNom, DisEnd, DisNum, DisCom,
    //             DisCEP, DisDDD, DisFon, DisFax, DisTipLogCod, DisBaiCod)
    @Test void getParityMatchesLegacy() {}

    // Scenario 3: Create via POST; verify resulting DB row matches what GeneXus INSERT would produce
    @Test void createParityMatchesLegacy() {}

    // Scenario 4: Update all mutable fields; verify DB row
    @Test void updateParityMatchesLegacy() {}

    // Scenario 5: Delete — verify record removed
    @Test void deleteParityMatchesLegacy() {}

    // Scenario 6: Create with invalid tipLogCod → expect 422 from new app / GXM error from legacy
    @Test void createRejectsUnknownTipLogParity() {}

    // Scenario 7: Create with alpha DDD → expect 422 from new app / GXM error from legacy
    @Test void createRejectsAlphaDddParity() {}

    // Scenario 8: Delete blocked by SAU_UNI → 409 from new app / GXM_del from legacy
    @Test void deleteBlockedByUnidadeParity() {}

    // Scenario 9: Insert valid (all fields) — verify all 11 columns match legacy output
    @Test void createAllFieldsParityMatchesLegacy() {}

    // Scenario 10: Insert minimal (DisCod=auto + DisNom only; FK codes = 0) → accepted
    @Test void createMinimalParityMatchesLegacy() {}

    // Scenario 11: Insert duplicate DisCod — 409 Conflict from new app / GXM_noupdate from legacy
    @Test void createDuplicateCodigoReturnsConflictParity() {}

    // Scenario 12: Insert with invalid DisBaiCod (non-zero, non-existent) → 422 from new app
    @Test void createRejectsUnknownBairroParity() {}

    // Scenario 13: Insert with DisTipLogCod=0 → accepted (null sentinel)
    @Test void createTipLogZeroAcceptedParity() {}

    // Scenario 14: Update bairroCodigo (valid code) → verify DisBaiCod stored correctly
    @Test void updateBairroCodigoParityMatchesLegacy() {}
}
