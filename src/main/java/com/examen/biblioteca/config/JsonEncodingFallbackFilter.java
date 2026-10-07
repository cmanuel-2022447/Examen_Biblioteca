package com.examen.biblioteca.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JsonEncodingFallbackFilter extends OncePerRequestFilter {

    private static final Charset CP1252 = Charset.forName("windows-1252");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).contains("application/json")) {
            filterChain.doFilter(request, response);
            return;
        }

        byte[] cuerpo = request.getInputStream().readAllBytes();
        if (cuerpo.length > 0 && !esUtf8Valido(cuerpo)) {
            cuerpo = new String(cuerpo, CP1252).getBytes(StandardCharsets.UTF_8);
        }
        filterChain.doFilter(new CuerpoWrapper(request, cuerpo), response);
    }

    private boolean esUtf8Valido(byte[] bytes) {
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return true;
        } catch (CharacterCodingException ex) {
            return false;
        }
    }

    private static final class CuerpoWrapper extends HttpServletRequestWrapper {

        private final byte[] cuerpo;

        private CuerpoWrapper(HttpServletRequest request, byte[] cuerpo) {
            super(request);
            this.cuerpo = cuerpo;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream flujo = new ByteArrayInputStream(cuerpo);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return flujo.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                }

                @Override
                public int read() {
                    return flujo.read();
                }

                @Override
                public int read(byte[] b, int off, int len) {
                    return flujo.read(b, off, len);
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() {
            return cuerpo.length;
        }

        @Override
        public long getContentLengthLong() {
            return cuerpo.length;
        }

        @Override
        public String getHeader(String name) {
            if ("Content-Length".equalsIgnoreCase(name)) {
                return String.valueOf(cuerpo.length);
            }
            return super.getHeader(name);
        }
    }
}
