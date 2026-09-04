package dev.piovra.crosscutting.web;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import dev.piovra.common.Ids;
import dev.piovra.crosscutting.MdcKeys;
import dev.piovra.events.EventHeaders;

/**
 * The HTTP-side counterpart of {@code MdcRecordInterceptor} on the Kafka side (piovra-kafka-support):
 * every request gets a correlation id, propagated through the MDC so logs are correlatable and
 * echoed back so a client can quote it when reporting an issue.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_HEADER = EventHeaders.CORRELATION_ID;
    public static final String TENANT_HEADER = "X-Piovra-Tenant";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = firstNonBlank(request.getHeader(CORRELATION_HEADER), Ids.newId());
        MDC.put(MdcKeys.CORRELATION_ID, correlationId);
        response.setHeader(CORRELATION_HEADER, correlationId);

        String tenant = request.getHeader(TENANT_HEADER);
        if (tenant != null && !tenant.isBlank()) {
            MDC.put(MdcKeys.TENANT, tenant);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MdcKeys.CORRELATION_ID);
            MDC.remove(MdcKeys.TENANT);
        }
    }

    private static String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
