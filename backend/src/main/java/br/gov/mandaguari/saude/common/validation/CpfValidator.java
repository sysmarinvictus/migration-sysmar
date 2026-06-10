package br.gov.mandaguari.saude.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** CPF check-digit validation (mod 11). Mirrors the legacy GeneXus CPF validation procedure. */
public class CpfValidator implements ConstraintValidator<CPF, String> {

    private boolean allowBlank;

    @Override public void initialize(CPF c) { this.allowBlank = c.allowBlank(); }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null || value.isBlank()) return allowBlank;
        String digits = value.replaceAll("\\D", "");
        return isValidCpf(digits);
    }

    /** Public so the service layer and unit tests can reuse it directly. */
    public static boolean isValidCpf(String d) {
        if (d == null || d.length() != 11) return false;
        if (d.chars().distinct().count() == 1) return false; // 000... etc. are rejected
        int d1 = checkDigit(d, 9, 10);
        int d2 = checkDigit(d, 10, 11);
        return d1 == (d.charAt(9) - '0') && d2 == (d.charAt(10) - '0');
    }

    private static int checkDigit(String d, int len, int startWeight) {
        int sum = 0, weight = startWeight;
        for (int i = 0; i < len; i++) sum += (d.charAt(i) - '0') * weight--;
        int mod = sum % 11;
        return (mod < 2) ? 0 : 11 - mod;
    }
}
