package com.examen.biblioteca.service;

import com.examen.biblioteca.entity.EstadoPrestamo;
import com.examen.biblioteca.entity.EstadoUsuario;
import com.examen.biblioteca.entity.Usuario;
import com.examen.biblioteca.exception.ResourceNotFoundException;
import com.examen.biblioteca.repository.PrestamoRepository;
import com.examen.biblioteca.repository.UsuarioRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PrestamoRepository prestamoRepository;

    public UsuarioService(UsuarioRepository usuarioRepository, PrestamoRepository prestamoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.prestamoRepository = prestamoRepository;
    }

    /**
     * Sanciona al usuario en una transaccion propia (REQUIRES_NEW) para que el
     * cambio quede persistido aunque la creacion del prestamo se rechace y
     * su transaccion se revierta.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sancionar(Long usuarioId) {
        Usuario usuario = usuarioRepository.findWithLockById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", usuarioId));

        prestamoRepository.marcarAtrasadosDeUsuario(usuarioId, LocalDate.now(),
                EstadoPrestamo.ACTIVO, EstadoPrestamo.ATRASADO);

        usuario.setEstado(EstadoUsuario.SANCIONADO);
        usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public Usuario obtenerPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "email", email));
    }

    @Transactional(readOnly = true)
    public Usuario obtener(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }
}
