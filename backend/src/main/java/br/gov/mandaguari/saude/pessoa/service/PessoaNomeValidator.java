package br.gov.mandaguari.saude.pessoa.service;

import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;

/**
 * Person-name quality rules mined from {@code sau_pesf_impl.java:3185-3247} (PesNom) and reused for
 * PesNomPai/PesNomMae (R39/R40) and PesNomSoc (R41). Each method throws {@link BusinessRule} with the
 * legacy PT-BR message + a stable code.
 *
 * <p><b>Scope note (deliberate):</b> the high-confidence, data-safe subset is enforced here — required
 * (R1), min-length (R2), surname/space (R3), no-double-space (R4), letters-only (R5). The letters-only
 * check is intentionally <em>accent-permissive</em> (Latin-1 letters + space + apostrophe) so valid
 * Brazilian names like "JOSÉ" are not rejected — the legacy runs {@code psau_limpacaracter} first and its
 * exact transform is not ported. The three exotic legacy micro-rules R6/R7/R8 ("two single-char terms",
 * "two two-char terms", "stray single character") are NOT enforced here: their regex semantics are
 * medium-confidence and enforcing them subtly wrong would reject valid names. They are recorded as an
 * open_question for KB/IDE verification before parity cutover.
 */
public final class PessoaNomeValidator {

    /** Latin letters incl. Portuguese accents, space and apostrophe (accent-permissive R5). */
    private static final String LETTERS = "[\\p{L} ']+";

    private PessoaNomeValidator() {}

    /** R1–R5 for the required registry name (PesNom). */
    public static void validateRequired(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new BusinessRule("pes.nome.required", "Informe o Nome da Pessoa!");           // R1
        }
        validateQuality(nome, "pes.nome", "Nome");
    }

    /** R39/R40: same quality rules but only when the optional name is filled. */
    public static void validateOptional(String nome, String codePrefix, String label) {
        if (nome == null || nome.isBlank()) return;
        validateQuality(nome, codePrefix, label);
    }

    /** R41: social name — no double space + letters only (no surname/min-length requirement). */
    public static void validateSocial(String nomeSocial) {
        if (nomeSocial == null || nomeSocial.isBlank()) return;
        if (nomeSocial.contains("  ")) {
            throw new BusinessRule("pes.nomeSocial.doubleSpace", "Nome Social não deve conter espaçamento duplo!"); // R41
        }
        if (!nomeSocial.trim().matches(LETTERS)) {
            throw new BusinessRule("pes.nomeSocial.letters", "Nome Social deve conter apenas letras e espaços!");   // R41
        }
    }

    private static void validateQuality(String nome, String codePrefix, String label) {
        String n = nome.trim();
        if (n.length() < 3) {                                                                    // R2
            throw new BusinessRule(codePrefix + ".minLength", label + " deve ser maior que três caracteres!");
        }
        if (!n.contains(" ")) {                                                                   // R3
            throw new BusinessRule(codePrefix + ".surname", label + " deve conter sobrenome!");
        }
        if (nome.contains("  ")) {                                                                // R4
            throw new BusinessRule(codePrefix + ".doubleSpace", label + " não deve conter espaçamento duplo!");
        }
        if (!n.matches(LETTERS)) {                                                                // R5
            throw new BusinessRule(codePrefix + ".letters", label + " deve conter apenas letras e espaços!");
        }
    }
}
