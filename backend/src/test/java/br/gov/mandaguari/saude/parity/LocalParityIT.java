package br.gov.mandaguari.saude.parity;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Golden-master parity for SAU_LOC vs the running GeneXus app. Runs only under the {@code parity}
 * Maven profile ({@code mvn -Pparity verify}). Fixtures are captured by the {@code parity-verifier}
 * agent into {@code src/test/resources/parity/SAU_LOC/} and replayed here. See SLICE-SPEC SAU_LOC
 * {@code parity.scenarios}.
 */
@Tag("parity")
class LocalParityIT extends AbstractIntegrationTest {

    // TODO(parity): run `/verify-parity SAU_LOC` to capture legacy fixtures, then implement these.

    @Test @Disabled("TODO: capture legacy fixture — list default")
    void listDefaultMatchesLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — get by id (existing)")
    void getByIdMatchesLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — get by id (404)")
    void getMissingMatchesLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — insert valid (derives municipioNome/Uf/Ibge)")
    void insertValidMatchesLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — insert missing nome rejected (R2)")
    void insertMissingNomeRejectedLikeLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — insert missing/zero municipio rejected (R3)")
    void insertMissingMunicipioRejectedLikeLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — insert unknown municipio rejected (R4)")
    void insertUnknownMunicipioRejectedLikeLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — delete allowed (no guard, R5)")
    void deleteAllowedLikeLegacy() {}
}
