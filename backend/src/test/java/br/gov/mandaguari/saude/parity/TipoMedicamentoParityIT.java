package br.gov.mandaguari.saude.parity;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Golden-master parity for SAU_TIPREM vs the running GeneXus app. Runs only under the {@code parity}
 * Maven profile ({@code mvn -Pparity verify}). Fixtures are captured by the {@code parity-verifier}
 * agent into {@code src/test/resources/parity/SAU_TIPREM/} and replayed here. See SLICE-SPEC
 * SAU_TIPREM {@code parity.scenarios}.
 */
@Tag("parity")
class TipoMedicamentoParityIT extends AbstractIntegrationTest {

    // TODO(parity): run `/verify-parity SAU_TIPREM` to capture legacy fixtures, then implement these.

    @Test @Disabled("TODO: capture legacy fixture — list default")
    void listDefaultMatchesLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — get by id (existing)")
    void getByIdMatchesLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — get by id (404)")
    void getMissingMatchesLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — insert valid")
    void insertValidMatchesLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — insert missing descricao rejected (R2)")
    void insertMissingDescricaoRejectedLikeLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — insert duplicate code rejected (R1)")
    void insertDuplicateRejectedLikeLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — update descricao")
    void updateMatchesLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — delete referenced-by-medicamento rejected (R3)")
    void deleteReferencedRejectedLikeLegacy() {}
}
