package com.examen.biblioteca.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<ErrorResponse> build(HttpServletRequest request, HttpStatus status, String mensaje) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), status.getReasonPhrase(), mensaje, request.getRequestURI()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(request, HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> reglaDeNegocio(BusinessRuleException ex, HttpServletRequest request) {
        return build(request, HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        ErrorResponse body = new ErrorResponse(400, "Bad Request", "Datos invalidos", request.getRequestURI());
        body.setFields(campos);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> restricciones(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v ->
                campos.put(v.getPropertyPath().toString(), v.getMessage()));
        ErrorResponse body = new ErrorResponse(400, "Bad Request", "Datos invalidos", request.getRequestURI());
        body.setFields(campos);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> cuerpoInvalido(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(request, HttpStatus.BAD_REQUEST, "Cuerpo de la peticion invalido o JSON malformado");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> parametroInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String mensaje = "El parametro '" + ex.getName() + "' tiene un valor invalido";
        return build(request, HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> parametroFaltante(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return build(request, HttpStatus.BAD_REQUEST, "Falta el parametro obligatorio: " + ex.getParameterName());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> argumentoInvalido(IllegalArgumentException ex, HttpServletRequest request) {
        String mensaje = ex.getMessage() == null ? "Peticion invalida" : ex.getMessage();
        return build(request, HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> credenciales(BadCredentialsException ex, HttpServletRequest request) {
        return build(request, HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> autenticacion(AuthenticationException ex, HttpServletRequest request) {
        String mensaje = ex.getMessage() == null ? "No autorizado" : ex.getMessage();
        return build(request, HttpStatus.UNAUTHORIZED, mensaje);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accesoDenegado(AccessDeniedException ex, HttpServletRequest request) {
        return build(request, HttpStatus.FORBIDDEN, "No tiene permisos para ejecutar esta operacion");
    }

    @ExceptionHandler({org.springframework.dao.DataIntegrityViolationException.class})
    public ResponseEntity<ErrorResponse> integridad(org.springframework.dao.DataIntegrityViolationException ex,
                                                    HttpServletRequest request) {
        String mensaje = "La operacion viola una restriccion de integridad de datos (posible duplicado)";
        if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("email")) {
            mensaje = "El email ya esta registrado";
        } else if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("isbn")) {
            mensaje = "El ISBN ya esta registrado";
        }
        return build(request, HttpStatus.CONFLICT, mensaje);
    }

    @ExceptionHandler({org.springframework.dao.OptimisticLockingFailureException.class,
            org.springframework.dao.PessimisticLockingFailureException.class,
            org.springframework.dao.CannotAcquireLockException.class})
    public ResponseEntity<ErrorResponse> bloqueo(org.springframework.dao.DataAccessException ex,
                                                 HttpServletRequest request) {
        return build(request, HttpStatus.CONFLICT,
                "La operacion no pudo completarse por acceso concurrente al recurso, intente de nuevo");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodoNoSoportado(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return build(request, HttpStatus.METHOD_NOT_ALLOWED, "Metodo no soportado: " + ex.getMethod());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> mediaNoSoportada(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return build(request, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de contenido no soportado, use application/json");
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponse> recursoInexistente(Exception ex, HttpServletRequest request) {
        return build(request, HttpStatus.NOT_FOUND, "El recurso solicitado no existe");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(request, HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrio un error interno, intente nuevamente");
    }
}
