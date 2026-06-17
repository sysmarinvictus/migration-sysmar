package br.gov.mandaguari.saude.common.error;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/** Translates exceptions to RFC-7807 {@link ProblemDetail} responses (replaces GeneXus AnyError flags). */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASE = "https://receituario.mandaguari.gov.br/problems/";

    @ExceptionHandler(DomainExceptions.NotFound.class)
    ProblemDetail handleNotFound(DomainExceptions.NotFound ex) {
        return problem(HttpStatus.NOT_FOUND, "not-found", "Recurso não encontrado", ex.getMessage());
    }

    @ExceptionHandler(DomainExceptions.Conflict.class)
    ProblemDetail handleConflict(DomainExceptions.Conflict ex) {
        return problem(HttpStatus.CONFLICT, "conflict", "Conflito", ex.getMessage());
    }

    /** R14: duplicate PK from MAX+1 race condition → 409. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        return problem(HttpStatus.CONFLICT, "duplicate-key", "Conflito de dados", "Registro duplicado.");
    }

    @ExceptionHandler(DomainExceptions.BusinessRule.class)
    ProblemDetail handleBusinessRule(DomainExceptions.BusinessRule ex) {
        ProblemDetail pd = problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.code(),
                "Regra de negócio violada", ex.getMessage());
        pd.setProperty("code", ex.code());
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleBeanValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "validation",
                "Falha de validação", "Um ou mais campos são inválidos.");
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraint(ConstraintViolationException ex) {
        return problem(HttpStatus.BAD_REQUEST, "validation", "Falha de validação", ex.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create(BASE + type));
        return pd;
    }
}
