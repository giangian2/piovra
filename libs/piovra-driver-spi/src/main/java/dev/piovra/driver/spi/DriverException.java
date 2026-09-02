package dev.piovra.driver.spi;

/** Wraps a {@link DriverError} for the cases where throwing reads better than returning. */
public class DriverException extends RuntimeException {

    private final transient DriverError error;

    public DriverException(DriverError error) {
        this(error, null);
    }

    public DriverException(DriverError error, Throwable cause) {
        super(error.code() + ": " + error.message(), cause);
        this.error = error;
    }

    public DriverError error() {
        return error;
    }
}
