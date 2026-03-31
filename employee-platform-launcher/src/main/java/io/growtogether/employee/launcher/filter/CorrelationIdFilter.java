package io.growtogether.employee.launcher.filter;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_ATTRIBUTE = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = resolveCorrelationId();
        request.setAttribute(CORRELATION_ID_ATTRIBUTE, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        MDC.put(CORRELATION_ID_ATTRIBUTE, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Try to upgrade fallback id to trace id if tracing context becomes available later in the chain.
            String currentTraceId = resolveTraceId();
            if (isValidTraceId(currentTraceId) && !response.isCommitted()) {
                response.setHeader(CORRELATION_ID_HEADER, currentTraceId);
                request.setAttribute(CORRELATION_ID_ATTRIBUTE, currentTraceId);
            }
            MDC.remove(CORRELATION_ID_ATTRIBUTE);
        }
    }

    private String resolveCorrelationId() {
        String traceId = resolveTraceId();
        if (isValidTraceId(traceId)) {
            return traceId;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String resolveTraceId() {
        String traceIdFromMdc = MDC.get("traceId");
        if (isValidTraceId(traceIdFromMdc)) {
            return traceIdFromMdc;
        }

        SpanContext spanContext = Span.current().getSpanContext();
        if (spanContext.isValid()) {
            return spanContext.getTraceId();
        }
        return null;
    }

    private boolean isValidTraceId(String traceId) {
        return traceId != null && traceId.length() == 32 && !"00000000000000000000000000000000".equals(traceId);
    }
}

