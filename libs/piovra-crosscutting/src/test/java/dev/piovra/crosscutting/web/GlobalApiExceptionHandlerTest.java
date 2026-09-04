package dev.piovra.crosscutting.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import dev.piovra.common.ErrorClass;
import dev.piovra.common.PiovraException;

/**
 * Calls the handler's methods directly rather than dispatching through MockMvc: no Spring context,
 * no servlet container, milliseconds (docs/12-development-guidelines.md section 3.2 rule 3).
 */
class GlobalApiExceptionHandlerTest {

    private final GlobalApiExceptionHandler handler = new GlobalApiExceptionHandler();

    @Test
    void validation_and_mapping_errors_map_to_bad_request() {
        assertStatus(ErrorClass.VALIDATION, HttpStatus.BAD_REQUEST);
        assertStatus(ErrorClass.MAPPING, HttpStatus.BAD_REQUEST);
    }

    @Test
    void an_auth_error_maps_to_unauthorized() {
        assertStatus(ErrorClass.AUTH, HttpStatus.UNAUTHORIZED);
    }

    @Test
    void a_rate_limit_error_maps_to_too_many_requests() {
        assertStatus(ErrorClass.RATE_LIMIT, HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void a_conflict_error_maps_to_conflict() {
        assertStatus(ErrorClass.CONFLICT, HttpStatus.CONFLICT);
    }

    @Test
    void a_marketplace_reject_maps_to_bad_gateway() {
        assertStatus(ErrorClass.MARKETPLACE_REJECT, HttpStatus.BAD_GATEWAY);
    }

    @Test
    void a_transient_error_maps_to_service_unavailable() {
        assertStatus(ErrorClass.TRANSIENT, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void an_internal_error_maps_to_internal_server_error() {
        assertStatus(ErrorClass.INTERNAL, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void the_problem_detail_carries_the_exception_code_class_and_retryability() {
        PiovraException exception =
                new PiovraException(ErrorClass.RATE_LIMIT, "EBAY_RATE_LIMIT_429", "too many requests");

        ProblemDetail problem = handler.handlePiovraException(exception);

        assertThat(problem.getProperties())
                .containsEntry("code", "EBAY_RATE_LIMIT_429")
                .containsEntry("retryable", true);
        assertThat(problem.getTitle()).isEqualTo("RATE_LIMIT");
        assertThat(problem.getDetail()).isEqualTo("too many requests");
    }

    @Test
    void an_unexpected_exception_never_leaks_its_message_to_the_response() {
        ProblemDetail problem = handler.handleUnexpected(new IllegalStateException("a secret internal detail"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getDetail()).doesNotContain("a secret internal detail");
    }

    private void assertStatus(ErrorClass errorClass, HttpStatus expected) {
        ProblemDetail problem = handler.handlePiovraException(new PiovraException(errorClass, "TEST_CODE", "boom"));
        assertThat(problem.getStatus()).isEqualTo(expected.value());
    }
}
