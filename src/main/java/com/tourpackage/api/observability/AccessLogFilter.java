package com.tourpackage.api.observability;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * One line per request: method, path, status, duration.
 *
 * <p>Logged after the chain completes so the status and timing are real. The
 * level is chosen by outcome — a 500 is a warning in the log whether or not
 * anyone is watching the metrics, and a slow 200 is worth seeing without turning
 * on debug for everything.
 *
 * <p>Query strings are deliberately not logged. They carry password-reset and
 * verification tokens on this API, and a log is the last place those should end
 * up in plaintext.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("access");

    private final long slowRequestMillis;

    public AccessLogFilter(@Value("${app.observability.slow-request-ms:1000}") long slowRequestMillis) {
        this.slowRequestMillis = slowRequestMillis;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Health probes run every few seconds forever; logging them buries
        // everything else.
        // contains() rather than startsWith(): the URI carries the servlet
        // context path ("/api/actuator/health"), so an anchored match misses.
        String path = request.getRequestURI();
        return path.contains("/actuator/health") || path.contains("/actuator/prometheus");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long millis = (System.nanoTime() - start) / 1_000_000;
            int status = response.getStatus();
            String method = request.getMethod();
            String path = request.getRequestURI();

            if (status >= 500) {
                log.warn("{} {} -> {} ({}ms)", method, path, status, millis);
            } else if (millis >= slowRequestMillis) {
                log.warn("{} {} -> {} ({}ms) SLOW", method, path, status, millis);
            } else {
                log.info("{} {} -> {} ({}ms)", method, path, status, millis);
            }
        }
    }

}
