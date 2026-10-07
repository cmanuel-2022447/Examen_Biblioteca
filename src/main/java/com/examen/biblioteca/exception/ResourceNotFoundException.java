package com.examen.biblioteca.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String recurso, String campo, Object valor) {
        super("No existe " + recurso + " con " + campo + ": " + valor);
    }

    public ResourceNotFoundException(String mensaje) {
        super(mensaje);
    }
}
