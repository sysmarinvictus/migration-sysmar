package br.gov.mandaguari.saude.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/** Valid Brazilian CPF (11 digits, check-digit). Port of GeneXus {@code psau_val_cnpjcpf} (CPF path). */
@Documented
@Constraint(validatedBy = CpfValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface CPF {
    String message() default "CPF inválido";
    boolean allowBlank() default false;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
