package br.gov.mandaguari.saude.parity;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Golden-master parity for SAU_ESP vs the running GeneXus app. Runs only under the {@code parity}
 * Maven profile ({@code mvn -Pparity test}). Fixtures are captured by the {@code parity-verifier}
 * agent into {@code src/test/resources/parity/SAU_ESP/} and replayed here.
 *
 * <p>Each scenario asserts BUSINESS equivalence with legacy (same record set / values / accept-reject
 * outcome), ignoring HTML chrome, locale formatting, and GeneXus session tokens. See SLICE-SPEC
 * SAU_ESP `parity.scenarios`.
 */
@Tag("parity")
class EspecialidadeParityIT extends AbstractIntegrationTest {

    // TODO(parity): run `/verify-parity SAU_ESP` to capture legacy fixtures, then implement these.

    @Test @Disabled("TODO: capture legacy fixture — list default")
    void listDefaultMatchesLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — get by id (existing)")
    void getByIdMatchesLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — get by id (404)")
    void getMissingMatchesLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — insert valid (compare resulting SAU_ESP row)")
    void insertValidMatchesLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — insert duplicate code rejected")
    void insertDuplicateRejectedLikeLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — insert missing name rejected")
    void insertMissingNameRejectedLikeLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — update name + agenda params")
    void updateMatchesLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — delete referenced-by-profissional rejected (R4)")
    void deleteReferencedRejectedLikeLegacy() {}
}
