package dev.piovra.common;

/** Root of the application exceptions: always carries an error class and a code. */
public class PiovraException extends RuntimeException {

    private final ErrorClass errorClass;
    private final String code;

    public PiovraException(ErrorClass errorClass, String code, String message) {
        this(errorClass, code, message, null);
    }

    public PiovraException(ErrorClass errorClass, String code, String message, Throwable cause) {
        super(message, cause);
        this.errorClass = errorClass;
        this.code = code;
    }

    public ErrorClass errorClass() {
        return errorClass;
    }

    public String code() {
        return code;
    }

    public boolean isRetryable() {
        return errorClass.isRetryable();
    }
}
