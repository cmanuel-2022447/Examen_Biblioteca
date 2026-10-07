package com.examen.biblioteca.dto;

import com.examen.biblioteca.entity.EstadoUsuario;
import com.examen.biblioteca.entity.Rol;
import com.examen.biblioteca.entity.Usuario;

public class UsuarioResponse {

    private Long id;
    private String nombre;
    private String email;
    private EstadoUsuario estado;
    private Rol rol;

    public UsuarioResponse() {
    }

    public static UsuarioResponse desde(Usuario usuario) {
        UsuarioResponse r = new UsuarioResponse();
        r.setId(usuario.getId());
        r.setNombre(usuario.getNombre());
        r.setEmail(usuario.getEmail());
        r.setEstado(usuario.getEstado());
        r.setRol(usuario.getRol());
        return r;
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

    public EstadoUsuario getEstado() {
        return estado;
    }

    public void setEstado(EstadoUsuario estado) {
        this.estado = estado;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }
}
