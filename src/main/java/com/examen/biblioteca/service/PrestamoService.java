package com.examen.biblioteca.service;

import com.examen.biblioteca.dto.PrestamoRequest;
import com.examen.biblioteca.dto.PrestamoResponse;
import com.examen.biblioteca.entity.EstadoPrestamo;
import com.examen.biblioteca.entity.EstadoUsuario;
import com.examen.biblioteca.entity.Libro;
import com.examen.biblioteca.entity.Prestamo;
import com.examen.biblioteca.entity.Rol;
import com.examen.biblioteca.entity.Usuario;
import com.examen.biblioteca.exception.BusinessRuleException;
import com.examen.biblioteca.exception.ResourceNotFoundException;
import com.examen.biblioteca.repository.LibroRepository;
import com.examen.biblioteca.repository.PrestamoRepository;
import com.examen.biblioteca.repository.UsuarioRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Reglas de negocio implementadas:
 *  1. No se prestan libros con stockDisponible = 0.
 *  2. Un LECTOR tiene un maximo de 3 prestamos activos.
 *  3. Los prestamos duran 14 dias.
 *  4. Los prestamos con fecha esperada vencida pasan a estado ATRASADO.
 *  5. Un LECTOR con prestamo atrasado se sanciona y se rechaza el nuevo prestamo.
 *  6. Un usuario SANCIONADO no puede realizar nuevos prestamos.
 *  7. stockDisponible nunca es negativo ni supera stockTotal.
 *  8. Si alguna operacion falla, la transaccion se revierte completo (sin datos parciales).
 *
 * El orden de bloqueo es siempre: Usuario -> Prestamos -> Libro, para evitar
 * interbloqueos entre creaciones y devoluciones concurrentes.
 */
@Service
public class PrestamoService {

    public static final int DIAS_PRESTAMO = 14;
    public static final int MAX_PRESTAMOS_LECTOR = 3;

    private final PrestamoRepository prestamoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LibroRepository libroRepository;
    private final UsuarioService usuarioService;

    public PrestamoService(PrestamoRepository prestamoRepository,
                           UsuarioRepository usuarioRepository,
                           LibroRepository libroRepository,
                           UsuarioService usuarioService) {
        this.prestamoRepository = prestamoRepository;
        this.usuarioRepository = usuarioRepository;
        this.libroRepository = libroRepository;
        this.usuarioService = usuarioService;
    }

    @Transactional
    public PrestamoResponse crearPrestamo(PrestamoRequest request) {
        LocalDate hoy = LocalDate.now();
        List<EstadoPrestamo> ocupados = List.of(EstadoPrestamo.ACTIVO, EstadoPrestamo.ATRASADO);

        Usuario preliminar = usuarioRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", request.getUsuarioId()));

        boolean tieneAtrasos = prestamoRepository.existsByUsuarioIdAndEstadoInAndFechaDevolucionEsperadaBefore(
                preliminar.getId(), ocupados, hoy);

        // Regla 5: lector con prestamo atrasado -> sancion permanente + rechazo
        if (preliminar.getRol() == Rol.LECTOR && tieneAtrasos) {
            usuarioService.sancionar(preliminar.getId());
            throw new BusinessRuleException(
                    "El usuario tiene prestamos atrasados: fue sancionado y no se creo el prestamo");
        }

        // Bloqueo pesimista del usuario: serializa prestamos del mismo usuario
        Usuario usuario = usuarioRepository.findWithLockById(preliminar.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", request.getUsuarioId()));

        // Regla 6
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new BusinessRuleException("El usuario esta sancionado y no puede realizar nuevos prestamos");
        }

        // Regla 4
        prestamoRepository.marcarAtrasadosDeUsuario(usuario.getId(), hoy,
                EstadoPrestamo.ACTIVO, EstadoPrestamo.ATRASADO);

        // Regla 2
        if (usuario.getRol() == Rol.LECTOR) {
            long activos = prestamoRepository.countByUsuarioIdAndEstadoIn(usuario.getId(), ocupados);
            if (activos >= MAX_PRESTAMOS_LECTOR) {
                throw new BusinessRuleException("El usuario ya tiene el maximo de "
                        + MAX_PRESTAMOS_LECTOR + " prestamos activos");
            }
        }

        // Bloqueo pesimista del libro: dos solicitudes simultaneas sobre el ultimo
        // ejemplar se serializan y solo una puede tomarlo
        Libro libro = libroRepository.findWithLockById(request.getLibroId())
                .orElseThrow(() -> new ResourceNotFoundException("Libro", "id", request.getLibroId()));

        // Reglas 1 y 7
        if (libro.getStockDisponible() == null || libro.getStockDisponible() <= 0) {
            throw new BusinessRuleException("El libro no tiene ejemplares disponibles (stock = 0)");
        }

        libro.setStockDisponible(libro.getStockDisponible() - 1);
        libroRepository.save(libro);

        Prestamo prestamo = new Prestamo(usuario, libro, hoy, hoy.plusDays(DIAS_PRESTAMO),
                EstadoPrestamo.ACTIVO);
        prestamoRepository.saveAndFlush(prestamo);

        return PrestamoResponse.desde(prestamo);
    }

    @Transactional
    public PrestamoResponse devolver(Long id) {
        Prestamo prestamo = prestamoRepository.findWithLockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prestamo", "id", id));

        if (prestamo.getEstado() == EstadoPrestamo.DEVUELTO || prestamo.getFechaDevolucionReal() != null) {
            throw new BusinessRuleException("El prestamo ya fue devuelto, no se puede devolver dos veces");
        }

        prestamo.setFechaDevolucionReal(LocalDate.now());
        prestamo.setEstado(EstadoPrestamo.DEVUELTO);
        prestamoRepository.save(prestamo);

        Libro libro = libroRepository.findWithLockById(prestamo.getLibro().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Libro", "id", prestamo.getLibro().getId()));

        int nuevoStock = libro.getStockDisponible() + 1;
        if (nuevoStock > libro.getStockTotal()) {
            nuevoStock = libro.getStockTotal();
        }
        libro.setStockDisponible(nuevoStock);
        libroRepository.save(libro);

        prestamoRepository.flush();
        return PrestamoResponse.desde(prestamo);
    }

    @Transactional
    public List<PrestamoResponse> misPrestamos(String email) {
        String correo = email == null ? "" : email.trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByEmail(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "email", email));

        prestamoRepository.marcarAtrasadosDeUsuario(usuario.getId(), LocalDate.now(),
                EstadoPrestamo.ACTIVO, EstadoPrestamo.ATRASADO);

        return prestamoRepository.findByUsuarioIdOrderByFechaPrestamoDesc(usuario.getId())
                .stream()
                .map(PrestamoResponse::desde)
                .toList();
    }

    @Transactional
    public List<PrestamoResponse> listarAtrasados() {
        prestamoRepository.marcarTodosAtrasados(LocalDate.now(),
                EstadoPrestamo.ACTIVO, EstadoPrestamo.ATRASADO);

        return prestamoRepository.findByEstadoOrderByFechaDevolucionEsperadaAsc(EstadoPrestamo.ATRASADO)
                .stream()
                .map(PrestamoResponse::desde)
                .toList();
    }
}
