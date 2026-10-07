package com.examen.biblioteca.security;

import com.examen.biblioteca.exception.ErrorResponse;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object atributo = request.getAttribute(JpaUserDetailsService.ATRIBUTO_ERROR_JWT);
        String mensaje = (atributo instanceof String texto && !texto.isBlank())
                ? texto
                : "Autenticacion requerida: envie un token valido en la cabecera Authorization";

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse body = new ErrorResponse(401, HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                mensaje, request.getRequestURI());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
