package br.gov.mandaguari.saude.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * CNS (Cartão Nacional de Saúde) validation. 15 digits; numbers starting 1/2 use a definitive
 * algorithm, 7/8/9 a provisional one — both validated by the standard mod-11 weighted sum ≡ 0.
 * Mirrors the legacy {@code PSAU_VER_CNS} procedure. Verify edge cases against the KB before prod.
 */
public class CnsValidator implements ConstraintValidator<CNS, String> {

    private boolean allowBlank;

    @Override public void initialize(CNS c) { this.allowBlank = c.allowBlank(); }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null || value.isBlank()) return allowBlank;
        return isValidCns(value.replaceAll("\\D", ""));
    }

    public static boolean isValidCns(String d) {
        if (d == null || d.length() != 15) return false;
        char first = d.charAt(0);
        if (first != '1' && first != '2' && first != '7' && first != '8' && first != '9') return false;
        long sum = 0;
        for (int i = 0; i < 15; i++) sum += (long) (d.charAt(i) - '0') * (15 - i);
        return sum % 11 == 0;
    }
}
