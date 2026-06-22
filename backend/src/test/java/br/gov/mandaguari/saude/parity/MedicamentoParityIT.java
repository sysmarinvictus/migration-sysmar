package br.gov.mandaguari.saude.parity;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Golden-master parity stubs for SAU_REM vs the legacy GeneXus sau_rem transaction.
 * Enable with -Pparity and a running legacy app. See SLICE-SPEC SAU_REM §parity (25 scenarios).
 */
@Disabled("parity profile only — requires running legacy app")
class MedicamentoParityIT extends AbstractIntegrationTest {

    @Test void listDefaultOrderByIdParity() {}
    @Test void listFilterByNomeParity() {}
    @Test void listFilterByTipoParity() {}
    @Test void listFilterBySituacaoAtivoParity() {}
    @Test void listFilterPsicotropicoParity() {}
    @Test void listFilterControleEspecialParity() {}
    @Test void getByIdWithSubLevelsParity() {}
    @Test void getByIdUnknownReturns404Parity() {}
    @Test void createMinimalParity() {}
    @Test void createFullPayloadParity() {}
    @Test void createDuplicateNomeAllowedParity() {}
    @Test void createInvalidTipRemRejectedParity() {}
    @Test void createControleEspecialParity() {}
    @Test void updateNomeAndSituacaoParity() {}
    @Test void putAddSauRem1RowParity() {}
    @Test void putRemoveEan13RowParity() {}
    @Test void putUniSetorUniquenessViolationParity() {}
    @Test void deleteUnusedParity() {}
    @Test void deleteWithRecesp1ReferenceParity() {}
    @Test void deleteWithRemlotReferenceParity() {}
    @Test void deleteWithInteracaoReferenceParity() {}
    @Test void lookupByNomeParity() {}
    @Test void lookupReceitaContextParity() {}
    @Test void mppFlagSetUnsetParity() {}
    @Test void controleEspecialVisibleInReceitaLookupParity() {}
}
