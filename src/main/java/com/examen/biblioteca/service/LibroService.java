package com.examen.biblioteca.service;

import com.examen.biblioteca.dto.LibroRequest;
import com.examen.biblioteca.dto.LibroResponse;
import com.examen.biblioteca.entity.Libro;
import com.examen.biblioteca.exception.BusinessRuleException;
import com.examen.biblioteca.exception.ResourceNotFoundException;
import com.examen.biblioteca.repository.LibroRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LibroService {

    private static final int TAMANO_MAXIMO_PAGINA = 100;

    private final LibroRepository libroRepository;

    public LibroService(LibroRepository libroRepository) {
        this.libroRepository = libroRepository;
    }

    @Transactional
    public Page<LibroResponse> listar(String titulo, String categoria, Integer page, Integer size) {
        String t = (titulo == null || titulo.isBlank()) ? null : titulo.trim();
        String c = (categoria == null || categoria.isBlank()) ? null : categoria.trim();

        int pagina = (page == null || page < 0) ? 0 : page;
        int tamanio = (size == null || size < 1) ? 10 : Math.min(size, TAMANO_MAXIMO_PAGINA);

        PageRequest solicitud = PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.ASC, "titulo"));
        return libroRepository.buscar(t, c, solicitud).map(LibroResponse::desde);
    }

    @Transactional(readOnly = true)
    public LibroResponse obtener(Long id) {
        return LibroResponse.desde(buscarEntidad(id));
    }

    @Transactional
    public LibroResponse crear(LibroRequest request) {
        String isbn = request.getIsbn().trim();
        Integer stockDisponible = request.getStockDisponible() == null
                ? request.getStockTotal()
                : request.getStockDisponible();
        validarStock(request.getStockTotal(), stockDisponible);

        Optional<Libro> existente = libroRepository.findByIsbn(isbn);
        if (existente.isPresent()) {
            Libro libroExistente = existente.get();
            if (mismoLibro(libroExistente, request, stockDisponible)) {
                return LibroResponse.desde(libroExistente);
            }
            throw new BusinessRuleException("El ISBN ya esta registrado");
        }

        Libro libro = new Libro(isbn, request.getTitulo().trim(), request.getAutor().trim(),
                request.getCategoria().trim(), request.getStockTotal(), stockDisponible);

        try {
            libroRepository.saveAndFlush(libro);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessRuleException("El ISBN ya esta registrado");
        }
        return LibroResponse.desde(libro);
    }

    @Transactional
    public LibroResponse actualizar(Long id, LibroRequest request) {
        Libro libro = buscarEntidad(id);

        String isbn = request.getIsbn().trim();
        if (!libro.getIsbn().equals(isbn) && libroRepository.existsByIsbn(isbn)) {
            throw new BusinessRuleException("El ISBN ya esta registrado");
        }

        Integer stockDisponible = request.getStockDisponible() == null
                ? libro.getStockDisponible()
                : request.getStockDisponible();
        validarStock(request.getStockTotal(), stockDisponible);

        libro.setIsbn(isbn);
        libro.setTitulo(request.getTitulo().trim());
        libro.setAutor(request.getAutor().trim());
        libro.setCategoria(request.getCategoria().trim());
        libro.setStockTotal(request.getStockTotal());
        libro.setStockDisponible(stockDisponible);

        try {
            libroRepository.saveAndFlush(libro);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessRuleException("El ISBN ya esta registrado");
        }
        return LibroResponse.desde(libro);
    }

    @Transactional
    public void eliminar(Long id) {
        Libro libro = buscarEntidad(id);
        try {
            libroRepository.delete(libro);
            libroRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessRuleException(
                    "No se puede eliminar el libro porque tiene prestamos registrados");
        }
    }

    private Libro buscarEntidad(Long id) {
        if (id == null || id <= 0) {
            throw new ResourceNotFoundException("Libro", "id", id);
        }
        return libroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro", "id", id));
    }

    private boolean mismoLibro(Libro libro, LibroRequest request, Integer stockDisponible) {
        return libro.getTitulo().equals(request.getTitulo().trim())
                && libro.getAutor().equals(request.getAutor().trim())
                && libro.getCategoria().equals(request.getCategoria().trim())
                && libro.getStockTotal().equals(request.getStockTotal())
                && libro.getStockDisponible().equals(stockDisponible);
    }

    private void validarStock(Integer stockTotal, Integer stockDisponible) {
        if (stockTotal != null && stockTotal < 0) {
            throw new IllegalArgumentException("El stock total no puede ser negativo");
        }
        if (stockDisponible != null && stockDisponible < 0) {
            throw new IllegalArgumentException("El stock disponible no puede ser negativo");
        }
        if (stockTotal != null && stockDisponible != null && stockDisponible > stockTotal) {
            throw new IllegalArgumentException("El stock disponible no puede ser mayor al stock total");
        }
    }
}
