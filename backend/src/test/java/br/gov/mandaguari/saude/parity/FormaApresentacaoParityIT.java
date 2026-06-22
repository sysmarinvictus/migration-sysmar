package br.gov.mandaguari.saude.parity;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Golden-master parity stubs for SAU_APRREM vs the legacy GeneXus sau_aprrem transaction.
 * Enable with -Pparity and a running legacy app. See SLICE-SPEC SAU_APRREM §parity.
 * NOTE (OQ-5): SAU_APRREM may be empty in the snapshot — write scenarios must seed their own rows.
 */
@Disabled("parity profile only — requires running legacy app")
class FormaApresentacaoParityIT extends AbstractIntegrationTest {

    @Test void listDefaultParity() {}
    @Test void listFilterByDescricaoParity() {}
    @Test void getByIdParity() {}
    @Test void getByIdUnknownReturns404Parity() {}
    @Test void createValidStoredUpperCaseParity() {}
    @Test void createBlankDescricaoReturns422Parity() {}
    @Test void createBlankAbreviacaoReturns422Parity() {}
    @Test void updateStoredUpperCaseParity() {}
    @Test void deleteUnusedParity() {}
    @Test void deleteReferencedByMedicamentoReturns409Parity() {}
    @Test void lookupByDescricaoParity() {}
}
