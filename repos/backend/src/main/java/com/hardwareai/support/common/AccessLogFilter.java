package com.hardwareai.support.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Outermost access log. Records every HTTP exchange (method, path, status, latency, caller) so
 * front-end and back-end can be correlated by the propagated {@code X-Request-Id}.
 *
 * <p>Runs before RequestContextFilter so the correlation id is available to every inner component;
 * it re-asserts the MDC before its own log line because inner filters clear it on their way out.</p>
 */
@Component("supportAccessLogFilter")
class AccessLogFilter extends OncePerRequestFilter implements OrderedFilter {

    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.length() > 100) requestId = UUID.randomUUID().toString();
        MDC.put(RequestContextFilter.REQUEST_ID, requestId);
        response.setHeader("X-Request-Id", requestId);

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String remote = request.getRemoteAddr();
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            int status = response.getStatus();
            MDC.put(RequestContextFilter.REQUEST_ID, requestId); // inner filters may have cleared it
            if (status >= 500) {
                log.error("ACCESS {} {} from {} -> {} in {}ms", method, uri, remote, status, millis);
            } else if (status >= 400) {
                log.warn("ACCESS {} {} from {} -> {} in {}ms", method, uri, remote, status, millis);
            } else {
                log.info("ACCESS {} {} from {} -> {} in {}ms", method, uri, remote, status, millis);
            }
            MDC.remove(RequestContextFilter.REQUEST_ID);
        }
    }
}
