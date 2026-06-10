package br.gov.mandaguari.saude.common.error;

/** Domain exceptions mapped to RFC-7807 responses by {@link GlobalExceptionHandler}. */
public final class DomainExceptions {
    private DomainExceptions() {}

    /** Requested resource does not exist → 404. */
    public static class NotFound extends RuntimeException {
        public NotFound(String message) { super(message); }
    }

    /** Business-rule violation (e.g. duplicate key, invalid state, min>max) → 409/422. */
    public static class BusinessRule extends RuntimeException {
        private final String code;
        public BusinessRule(String code, String message) { super(message); this.code = code; }
        public String code() { return code; }
    }

    /** Operation blocked because the resource is referenced elsewhere → 409. */
    public static class Conflict extends RuntimeException {
        public Conflict(String message) { super(message); }
    }
}
