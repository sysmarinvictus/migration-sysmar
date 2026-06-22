package br.gov.mandaguari.saude.parity;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Golden-master parity for SAU_PRO (Profissional / prescritor) vs the running GeneXus app. Runs only
 * under the {@code parity} Maven profile ({@code mvn -Pparity test}). Fixtures are captured by the
 * {@code parity-verifier} agent into {@code src/test/resources/parity/profissional/} and replayed here.
 *
 * <p>Each scenario asserts BUSINESS equivalence with legacy (same record set / values / accept-reject
 * outcome), ignoring HTML chrome, locale formatting, GeneXus session tokens and encrypted-param noise.
 * Mirrors {@code parity.scenarios} in SLICE-SPEC SAU_PRO. The cases below are intentionally
 * {@code @Disabled} until the legacy fixtures are captured — coverage stays visible, not silently
 * missing.
 *
 * <p><b>SECURITY (R31):</b> {@code certificadoSenha}/{@code certificado}/{@code assinaturaImagem} must
 * NEVER appear in any legacy-vs-new payload comparison — the parity capture must redact them and the
 * new endpoints never emit them (asserted directly in ProfissionalControllerIT/ProfissionalSecurityTest).
 */
@Tag("parity")
class ProfissionalParityIT extends AbstractIntegrationTest {

    // TODO(parity): run `/verify-parity SAU_PRO` to capture legacy fixtures, then implement these.

    @Test @Disabled("TODO: capture legacy fixture — list default (page; count + first rows)")
    void listDefaultMatchesLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — search by nome (PesNom LIKE substring), R16")
    void searchByNameSubstringMatchesLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — search by CNS (=) and by CPF (=)")
    void searchByCnsAndCpfMatchesLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — view by id (joined SYS_PES + SAU_CONCLA; NO cert/senha)")
    void viewByIdMatchesLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — insert: person (SYS_PES) does not exist → reject (R1)")
    void insertMissingPersonRejectedLikeLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — insert: missing CNS → reject (R3)")
    void insertMissingCnsRejectedLikeLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — insert: invalid CNS → reject (R4)")
    void insertInvalidCnsRejectedLikeLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — insert: duplicate CNS (other person) → reject (R5)")
    void insertDuplicateCnsRejectedLikeLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — insert: invalid CPF (R6) / duplicate CPF (R7) → reject")
    void insertInvalidOrDuplicateCpfRejectedLikeLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — insert: unknown conselho (conclacod!=0) → reject (R10); 0 allowed")
    void insertUnknownConselhoRejectedLikeLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — insert defaults: situacao=1, proext=0, exportaEsus=false (R12/R13)")
    void insertDefaultsMatchLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — insert/update: nomeSoundex recomputed server-side (R15)")
    void soundexRecomputedMatchesLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — update: writes back SYS_PES name/cpf/phones (R2)")
    void updateWritesBackPersonMatchesLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — delete blocked by SAU_PROESP (R20)")
    void deleteBlockedBySpecialtyLikeLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — delete blocked by SAU_USU (R21)")
    void deleteBlockedBySystemUserLikeLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — delete blocked by SAU_UNI roles (R25)")
    void deleteBlockedByUnidadeRoleLikeLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — delete blocked by SAU_RECESP (R26, Portaria 344)")
    void deleteBlockedByControlledPrescriptionLikeLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — delete allowed when unreferenced")
    void deleteUnreferencedMatchesLegacy() {}

    @Test @Disabled("TODO: capture legacy fixture — SECURITY: certificadoSenha absent from every GET/list/view (R31)")
    void certificadoSenhaNeverInAnyPayloadLikeLegacy() {}
}
