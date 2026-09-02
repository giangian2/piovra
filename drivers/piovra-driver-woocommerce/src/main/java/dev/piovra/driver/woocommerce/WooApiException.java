package dev.piovra.driver.woocommerce;

import java.time.Duration;

/** An error returned by the WooCommerce API, carrying the native code ({@code woocommerce_rest_...}). */
public class WooApiException extends RuntimeException {

    private final int status;
    private final String wooCode;
    private final transient Duration retryAfter;

    public WooApiException(int status, String wooCode, String message, Duration retryAfter) {
        super(message);
        this.status = status;
        this.wooCode = wooCode;
        this.retryAfter = retryAfter;
    }

    public int status() {
        return status;
    }

    public String wooCode() {
        return wooCode;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
