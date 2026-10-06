package com.examen.biblioteca.dto;

import com.examen.biblioteca.entity.EstadoPrestamo;
import com.examen.biblioteca.entity.Prestamo;

import java.time.LocalDate;

public class PrestamoResponse {

    private Long id;
    private Long usuarioId;
    private String usuarioNombre;
    private String usuarioEmail;
    private Long libroId;
    private String libroIsbn;
    private String libroTitulo;
    private LocalDate fechaPrestamo;
    private LocalDate fechaDevolucionEsperada;
    private LocalDate fechaDevolucionReal;
    private EstadoPrestamo estado;

    public PrestamoResponse() {
    }

    public static PrestamoResponse desde(Prestamo prestamo) {
        PrestamoResponse r = new PrestamoResponse();
        r.setId(prestamo.getId());
        if (prestamo.getUsuario() != null) {
            r.setUsuarioId(prestamo.getUsuario().getId());
            r.setUsuarioNombre(prestamo.getUsuario().getNombre());
            r.setUsuarioEmail(prestamo.getUsuario().getEmail());
        }
        if (prestamo.getLibro() != null) {
            r.setLibroId(prestamo.getLibro().getId());
            r.setLibroIsbn(prestamo.getLibro().getIsbn());
            r.setLibroTitulo(prestamo.getLibro().getTitulo());
        }
        r.setFechaPrestamo(prestamo.getFechaPrestamo());
        r.setFechaDevolucionEsperada(prestamo.getFechaDevolucionEsperada());
        r.setFechaDevolucionReal(prestamo.getFechaDevolucionReal());
        r.setEstado(prestamo.getEstado());
        return r;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public void setUsuarioNombre(String usuarioNombre) {
        this.usuarioNombre = usuarioNombre;
    }

    public String getUsuarioEmail() {
        return usuarioEmail;
    }

    public void setUsuarioEmail(String usuarioEmail) {
        this.usuarioEmail = usuarioEmail;
    }

    public Long getLibroId() {
        return libroId;
    }

    public void setLibroId(Long libroId) {
        this.libroId = libroId;
    }

    public String getLibroIsbn() {
        return libroIsbn;
    }

    public void setLibroIsbn(String libroIsbn) {
        this.libroIsbn = libroIsbn;
    }

    public String getLibroTitulo() {
        return libroTitulo;
    }

    public void setLibroTitulo(String libroTitulo) {
        this.libroTitulo = libroTitulo;
    }

    public LocalDate getFechaPrestamo() {
        return fechaPrestamo;
    }

    public void setFechaPrestamo(LocalDate fechaPrestamo) {
        this.fechaPrestamo = fechaPrestamo;
    }

    public LocalDate getFechaDevolucionEsperada() {
        return fechaDevolucionEsperada;
    }

    public void setFechaDevolucionEsperada(LocalDate fechaDevolucionEsperada) {
        this.fechaDevolucionEsperada = fechaDevolucionEsperada;
    }

    public LocalDate getFechaDevolucionReal() {
        return fechaDevolucionReal;
    }

    public void setFechaDevolucionReal(LocalDate fechaDevolucionReal) {
        this.fechaDevolucionReal = fechaDevolucionReal;
    }

    public EstadoPrestamo getEstado() {
        return estado;
    }

    public void setEstado(EstadoPrestamo estado) {
        this.estado = estado;
    }
}
