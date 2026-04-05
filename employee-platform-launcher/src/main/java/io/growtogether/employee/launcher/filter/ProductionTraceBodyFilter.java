package io.growtogether.employee.launcher.filter;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Unified tracing filter that uses the OpenTelemetry API to:
 * <ul>
 *   <li>Set the {@code X-Trace-Id} response header for client-side correlation.</li>
 *   <li>Capture request and response bodies as span attributes for Grafana Tempo.</li>
 * </ul>
 * Sensitive fields are masked and large payloads are truncated to avoid
 * excessive storage costs.
 */
@Component
public class ProductionTraceBodyFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final int MAX_BODY_SIZE = 2048; // Truncate at 2KB
    private static final Set<String> SENSITIVE_KEYS = Set.of("password", "token", "secret", "authorization");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Wrap request and response to allow multiple reads of the body
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, MAX_BODY_SIZE);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        // Propagate trace ID as a response header for client-side correlation
        SpanContext spanContext = Span.current().getSpanContext();
        if (spanContext.isValid()) {
            wrappedResponse.setHeader(TRACE_ID_HEADER, spanContext.getTraceId());
        }

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            Span currentSpan = Span.current();
            if (currentSpan.getSpanContext().isValid()) {
                captureRequestBody(currentSpan, wrappedRequest);
                captureResponseBody(currentSpan, wrappedResponse);
            }
            // Copy the cached response body back to the original response
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void captureRequestBody(Span span, ContentCachingRequestWrapper request) {
        byte[] buf = request.getContentAsByteArray();
        if (buf.length == 0) {
            return;
        }
        try {
            String body = new String(buf, request.getCharacterEncoding());
            span.setAttribute("http.request.body", sanitizeAndTruncate(body));
        } catch (Exception e) {
            span.setAttribute("http.request.body.error", "Failed to capture: " + e.getMessage());
        }
    }

    private void captureResponseBody(Span span, ContentCachingResponseWrapper response) {
        byte[] buf = response.getContentAsByteArray();
        if (buf.length == 0) {
            return;
        }
        try {
            String body = new String(buf, StandardCharsets.UTF_8);
            span.setAttribute("http.response.body", sanitizeAndTruncate(body));
            span.setAttribute("http.response.body.size", buf.length);
        } catch (Exception e) {
            span.setAttribute("http.response.body.error", "Failed to capture: " + e.getMessage());
        }
    }

    /**
     * Masks sensitive JSON keys and truncates the body to {@link #MAX_BODY_SIZE}.
     */
    private String sanitizeAndTruncate(String body) {
        String masked = maskSensitiveData(body);
        return masked.length() > MAX_BODY_SIZE
                ? masked.substring(0, MAX_BODY_SIZE) + "...[truncated]"
                : masked;
    }

    private String maskSensitiveData(String body) {
        String masked = body;
        for (String key : SENSITIVE_KEYS) {
            masked = masked.replaceAll("(\"" + key + "\"\\s*:\\s*\")[^\"]+(\")", "$1******$2");
        }
        return masked;
    }
}