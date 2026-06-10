package br.gov.mandaguari.saude.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/** Valid Cartão Nacional de Saúde (15 digits, mod-11 weighting). Port of GeneXus {@code PSAU_VER_CNS}. */
@Documented
@Constraint(validatedBy = CnsValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface CNS {
    String message() default "CNS inválido";
    boolean allowBlank() default false;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
