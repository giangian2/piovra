package dev.piovra.crosscutting.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.piovra.common.ErrorClass;
import dev.piovra.common.PiovraException;

/**
 * Maps the {@link PiovraException} hierarchy's {@link ErrorClass} to an HTTP status and a
 * {@link ProblemDetail} body, so no controller writes its own try/catch (docs/12-development-guidelines.md
 * section 5.2). Never leaks a stack trace in the response body.
 */
@RestControllerAdvice
public class GlobalApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalApiExceptionHandler.class);

    @ExceptionHandler(PiovraException.class)
    public ProblemDetail handlePiovraException(PiovraException exception) {
        HttpStatus status = statusFor(exception.errorClass());
        if (status.is5xxServerError()) {
            log.error("request failed: code={}", exception.code(), exception);
        } else {
            log.warn("request rejected: code={} message={}", exception.code(), exception.getMessage());
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle(exception.errorClass().name());
        problem.setProperty("code", exception.code());
        problem.setProperty("retryable", exception.isRetryable());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        log.error("unexpected error", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "unexpected error");
    }

    private static HttpStatus statusFor(ErrorClass errorClass) {
        return switch (errorClass) {
            case VALIDATION, MAPPING -> HttpStatus.BAD_REQUEST;
            case AUTH -> HttpStatus.UNAUTHORIZED;
            case RATE_LIMIT -> HttpStatus.TOO_MANY_REQUESTS;
            case CONFLICT -> HttpStatus.CONFLICT;
            case MARKETPLACE_REJECT -> HttpStatus.BAD_GATEWAY;
            case TRANSIENT -> HttpStatus.SERVICE_UNAVAILABLE;
            case INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
