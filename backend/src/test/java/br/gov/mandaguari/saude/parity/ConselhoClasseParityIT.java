package br.gov.mandaguari.saude.parity;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Golden-master parity for SAU_CONCLA vs the running GeneXus app. Runs only under the {@code parity}
 * Maven profile ({@code mvn -Pparity test}). Fixtures are captured by the {@code parity-verifier}
 * agent into {@code src/test/resources/parity/SAU_CONCLA/} and replayed here.
 *
 * <p>Each scenario asserts BUSINESS equivalence with legacy (same record set / values / accept-reject
 * outcome), ignoring HTML chrome, locale formatting, and GeneXus session tokens. See SLICE-SPEC
 * SAU_CONCLA {@code parity.scenarios}.
 */
@Tag("parity")
class ConselhoClasseParityIT extends AbstractIntegrationTest {

    // TODO(parity): run `/verify-parity SAU_CONCLA` to capture legacy fixtures, then implement these.

    @Test @Disabled("TODO: capture legacy fixture — list default")
    void listDefaultMatchesLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — get by id (existing)")
    void getByIdMatchesLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — get by id (404)")
    void getMissingMatchesLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — insert valid (compare resulting SAU_CONCLA row)")
    void insertValidMatchesLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — insert codigo > 999 rejected")
    void insertOutOfRangeRejectedLikeLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — insert duplicate code rejected")
    void insertDuplicateRejectedLikeLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — update sigla + nome")
    void updateMatchesLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — delete referenced-by-profissional rejected (R3)")
    void deleteReferencedRejectedLikeLegacy() {}
}
