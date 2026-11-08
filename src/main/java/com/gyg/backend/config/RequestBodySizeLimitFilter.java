package com.gyg.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class RequestBodySizeLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final int maxBodySizeBytes;

    public RequestBodySizeLimitFilter(
            ObjectMapper objectMapper,
            @Value("${app.http.max-body-size:1MB}") DataSize maxBodySize) {
        this.objectMapper = objectMapper;
        this.maxBodySizeBytes = Math.toIntExact(maxBodySize.toBytes());
        if (maxBodySizeBytes < 1 || maxBodySizeBytes == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("El tamaño máximo del cuerpo debe ser mayor que cero.");
        }
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)) {
            filterChain.doFilter(request, response);
            return;
        }

        long contentLength = request.getContentLengthLong();
        if (contentLength > maxBodySizeBytes) {
            writeTooLargeResponse(request, response);
            return;
        }

        byte[] body = request.getInputStream().readNBytes(maxBodySizeBytes + 1);
        if (body.length > maxBodySizeBytes) {
            writeTooLargeResponse(request, response);
            return;
        }

        filterChain.doFilter(new ReplayableRequest(request, body), response);
    }

    private void writeTooLargeResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(413);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 413);
        body.put("error", "Payload Too Large");
        body.put("message", "El cuerpo de la solicitud supera el tamaño permitido.");
        body.put("path", request.getRequestURI());
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private static final class ReplayableRequest extends HttpServletRequestWrapper {

        private final byte[] body;
        private ServletInputStream inputStream;

        private ReplayableRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            if (inputStream == null) {
                ByteArrayInputStream source = new ByteArrayInputStream(body);
                inputStream = new ServletInputStream() {
                    @Override
                    public int read() {
                        return source.read();
                    }

                    @Override
                    public boolean isFinished() {
                        return source.available() == 0;
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setReadListener(ReadListener readListener) {
                        throw new UnsupportedOperationException("Lectura asíncrona no soportada.");
                    }
                };
            }
            return inputStream;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            String encoding = getCharacterEncoding();
            Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }
}
