package com.tourpackage.api.observability;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Gives every request an id, puts it in the logging context, and returns it to
 * the caller.
 *
 * <p>Without this, a report of "it failed around 3pm" means grepping a log where
 * concurrent requests are interleaved line by line. With it, the id in the
 * response header selects exactly the lines belonging to that one request.
 *
 * <p>An inbound {@code X-Request-Id} is honoured so a trace started by a load
 * balancer or the frontend survives the hop — but it is length-capped and
 * stripped of anything but safe characters first, because it goes into log lines
 * and a response header, and neither should be forgeable into extra content.
 *
 * <p>Ordered ahead of Spring Security so that authentication failures are logged
 * with an id too; those are exactly the requests someone will want to trace.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    private static final int MAX_ID_LENGTH = 64;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = sanitize(request.getHeader(HEADER));
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);

        try {
            chain.doFilter(request, response);
        } finally {
            // Threads are pooled and reused. Leaving the id behind would stamp
            // it onto whatever unrelated request lands on this thread next.
            MDC.remove(MDC_KEY);
        }
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.length() > MAX_ID_LENGTH ? value.substring(0, MAX_ID_LENGTH) : value;
        String safe = trimmed.replaceAll("[^A-Za-z0-9._-]", "");
        return safe.isBlank() ? null : safe;
    }

}
