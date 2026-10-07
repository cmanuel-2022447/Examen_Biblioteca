package com.examen.biblioteca.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class LibroRequest {

    @NotBlank(message = "El ISBN es obligatorio")
    @Size(max = 30, message = "El ISBN no puede superar 30 caracteres")
    private String isbn;

    @NotBlank(message = "El titulo es obligatorio")
    @Size(max = 200, message = "El titulo no puede superar 200 caracteres")
    private String titulo;

    @NotBlank(message = "El autor es obligatorio")
    @Size(max = 150, message = "El autor no puede superar 150 caracteres")
    private String autor;

    @NotBlank(message = "La categoria es obligatoria")
    @Size(max = 100, message = "La categoria no puede superar 100 caracteres")
    private String categoria;

    @NotNull(message = "El stock total es obligatorio")
    @Min(value = 0, message = "El stock total no puede ser negativo")
    private Integer stockTotal;

    @Min(value = 0, message = "El stock disponible no puede ser negativo")
    private Integer stockDisponible;

    public LibroRequest() {
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
