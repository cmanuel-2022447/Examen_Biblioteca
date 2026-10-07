package com.examen.biblioteca.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class PrestamoRequest {

    @NotNull(message = "El id del usuario es obligatorio")
    @Positive(message = "El id del usuario debe ser positivo")
    private Long usuarioId;

    @NotNull(message = "El id del libro es obligatorio")
    @Positive(message = "El id del libro debe ser positivo")
    private Long libroId;

    public PrestamoRequest() {
    }

    public PrestamoRequest(Long usuarioId, Long libroId) {
        this.usuarioId = usuarioId;
        this.libroId = libroId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getLibroId() {
        return libroId;
    }

    public void setLibroId(Long libroId) {
        this.libroId = libroId;
    }
}
