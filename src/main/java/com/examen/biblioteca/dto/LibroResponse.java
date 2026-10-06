package com.examen.biblioteca.dto;

import com.examen.biblioteca.entity.Libro;

public class LibroResponse {

    private Long id;
    private String isbn;
    private String titulo;
    private String autor;
    private String categoria;
    private Integer stockTotal;
    private Integer stockDisponible;

    public LibroResponse() {
    }

    public static LibroResponse desde(Libro libro) {
        LibroResponse r = new LibroResponse();
        r.setId(libro.getId());
        r.setIsbn(libro.getIsbn());
        r.setTitulo(libro.getTitulo());
        r.setAutor(libro.getAutor());
        r.setCategoria(libro.getCategoria());
        r.setStockTotal(libro.getStockTotal());
        r.setStockDisponible(libro.getStockDisponible());
        return r;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Integer getStockTotal() {
        return stockTotal;
    }

    public void setStockTotal(Integer stockTotal) {
        this.stockTotal = stockTotal;
    }

    public Integer getStockDisponible() {
        return stockDisponible;
    }

    public void setStockDisponible(Integer stockDisponible) {
        this.stockDisponible = stockDisponible;
    }
}
