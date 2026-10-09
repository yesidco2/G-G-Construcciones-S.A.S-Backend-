package com.gyg.backend.dto;

import com.gyg.backend.model.Bloque;

public class ServicioDTO {
    private Long id;
    private String titulo;
    private String descripcion;
    private Bloque bloque;

    public ServicioDTO() {
    }

    public ServicioDTO(Long id, String titulo, String descripcion, Bloque bloque) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.bloque = bloque;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Bloque getBloque() {
        return bloque;
    }

    public void setBloque(Bloque bloque) {
        this.bloque = bloque;
    }
}
