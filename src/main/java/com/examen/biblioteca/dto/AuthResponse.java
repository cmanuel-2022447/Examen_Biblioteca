package com.examen.biblioteca.dto;

import com.examen.biblioteca.entity.EstadoUsuario;
import com.examen.biblioteca.entity.Rol;

public class AuthResponse {

    private String token;
    private String tipo = "Bearer";
    private Long id;
    private String nombre;
    private String email;
    private Rol rol;
    private EstadoUsuario estado;

    public AuthResponse() {
    }

    public AuthResponse(String token, Long id, String nombre, String email, Rol rol, EstadoUsuario estado) {
        this.token = token;
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.rol = rol;
        this.estado = estado;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public EstadoUsuario getEstado() {
        return estado;
    }

    public void setEstado(EstadoUsuario estado) {
        this.estado = estado;
    }
}
