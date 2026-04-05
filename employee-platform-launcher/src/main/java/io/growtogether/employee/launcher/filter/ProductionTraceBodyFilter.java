package io.growtogether.employee.launcher.filter;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

@Component
@Order(1)
class ProductionTraceBodyFilter extends OncePerRequestFilter {

    private static final int MAX_BODY_LENGTH = 8192;
    private static final AttributeKey<String> HTTP_REQUEST_BODY = AttributeKey.stringKey("http.request.body");
    private static final AttributeKey<String> HTTP_RESPONSE_BODY = AttributeKey.stringKey("http.response.body");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Span span = Span.current();
        if (!span.isRecording()) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request, MAX_BODY_LENGTH);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            captureRequestBody(span, requestWrapper);
            captureResponseBody(span, responseWrapper);
            responseWrapper.copyBodyToResponse();
        }
    }

    private void captureRequestBody(Span span, ContentCachingRequestWrapper request) {
        captureBody(span, HTTP_REQUEST_BODY, request.getContentAsByteArray(),
                request.getContentType(), request.getCharacterEncoding());
    }

    private void captureResponseBody(Span span, ContentCachingResponseWrapper response) {
        captureBody(span, HTTP_RESPONSE_BODY, response.getContentAsByteArray(),
                response.getContentType(), response.getCharacterEncoding());
    }

    private void captureBody(Span span, AttributeKey<String> key, byte[] content,
                              String contentType, String encoding) {
        if (content.length > 0 && isTextContentType(contentType)) {
            String body = new String(content, resolveCharset(encoding));
            span.setAttribute(key, truncate(body));
        }
    }

    private boolean isTextContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            return mediaType.isCompatibleWith(MediaType.APPLICATION_JSON)
                    || mediaType.isCompatibleWith(MediaType.TEXT_PLAIN)
                    || mediaType.isCompatibleWith(MediaType.TEXT_XML)
                    || mediaType.isCompatibleWith(MediaType.APPLICATION_XML)
                    || "text".equalsIgnoreCase(mediaType.getType());
        } catch (InvalidMediaTypeException e) {
            return false;
        }
    }

    private Charset resolveCharset(String encoding) {
        if (encoding == null) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(encoding);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
            return StandardCharsets.UTF_8;
        }
    }

    private String truncate(String body) {
        if (body.length() <= MAX_BODY_LENGTH) {
            return body;
        }
        return body.substring(0, MAX_BODY_LENGTH) + "...[truncated]";
    }
}
