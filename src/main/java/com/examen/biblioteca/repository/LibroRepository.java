package com.examen.biblioteca.repository;

import com.examen.biblioteca.entity.Libro;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LibroRepository extends JpaRepository<Libro, Long> {

    Optional<Libro> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Libro l where l.id = :id")
    Optional<Libro> findWithLockById(@Param("id") Long id);

    @Query("""
            select l from Libro l
            where (:titulo is null or lower(l.titulo) like lower(concat('%', :titulo, '%')))
              and (:categoria is null or lower(l.categoria) like lower(concat('%', :categoria, '%')))
            """)
    Page<Libro> buscar(@Param("titulo") String titulo,
                       @Param("categoria") String categoria,
                       Pageable pageable);
}
