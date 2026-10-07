package com.examen.biblioteca.repository;

import com.examen.biblioteca.entity.EstadoPrestamo;
import com.examen.biblioteca.entity.Prestamo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    List<Prestamo> findByUsuarioIdOrderByFechaPrestamoDesc(Long usuarioId);

    List<Prestamo> findByEstadoOrderByFechaDevolucionEsperadaAsc(EstadoPrestamo estado);

    long countByUsuarioIdAndEstadoIn(Long usuarioId, Collection<EstadoPrestamo> estados);

    boolean existsByUsuarioIdAndEstadoInAndFechaDevolucionEsperadaBefore(Long usuarioId,
                                                                         Collection<EstadoPrestamo> estados,
                                                                         LocalDate fecha);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Prestamo p where p.id = :id")
    Optional<Prestamo> findWithLockById(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Prestamo p set p.estado = :atrasado
            where p.usuario.id = :usuarioId and p.estado = :activo
              and p.fechaDevolucionReal is null and p.fechaDevolucionEsperada < :hoy
            """)
    int marcarAtrasadosDeUsuario(@Param("usuarioId") Long usuarioId,
                                 @Param("hoy") LocalDate hoy,
                                 @Param("activo") EstadoPrestamo activo,
                                 @Param("atrasado") EstadoPrestamo atrasado);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Prestamo p set p.estado = :atrasado
            where p.estado = :activo and p.fechaDevolucionReal is null
              and p.fechaDevolucionEsperada < :hoy
            """)
    int marcarTodosAtrasados(@Param("hoy") LocalDate hoy,
                             @Param("activo") EstadoPrestamo activo,
                             @Param("atrasado") EstadoPrestamo atrasado);
}
