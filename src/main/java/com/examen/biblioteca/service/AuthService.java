package com.examen.biblioteca.service;

import com.examen.biblioteca.dto.AuthResponse;
import com.examen.biblioteca.dto.LoginRequest;
import com.examen.biblioteca.dto.RegisterRequest;
import com.examen.biblioteca.entity.EstadoUsuario;
import com.examen.biblioteca.entity.Rol;
import com.examen.biblioteca.entity.Usuario;
import com.examen.biblioteca.exception.BusinessRuleException;
import com.examen.biblioteca.repository.UsuarioRepository;
import com.examen.biblioteca.security.JwtService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Registro publico. Siempre crea usuarios LECTOR y ACTIVO:
     * nunca se puede registrar un ADMIN desde el endpoint publico.
     */
    @Transactional
    public AuthResponse registrar(RegisterRequest request) {
        String email = normalizarEmail(request.getEmail());

        if (usuarioRepository.existsByEmail(email)) {
            throw new BusinessRuleException("El email ya esta registrado");
        }

        Usuario usuario = new Usuario(
                request.getNombre().trim(),
                email,
                passwordEncoder.encode(request.getPassword()),
                EstadoUsuario.ACTIVO,
                Rol.LECTOR
        );
        usuarioRepository.save(usuario);

        return new AuthResponse(jwtService.generarToken(usuario), usuario.getId(), usuario.getNombre(),
                usuario.getEmail(), usuario.getRol(), usuario.getEstado());
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizarEmail(request.getEmail());

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        return new AuthResponse(jwtService.generarToken(usuario), usuario.getId(), usuario.getNombre(),
                usuario.getEmail(), usuario.getRol(), usuario.getEstado());
    }

    private String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
