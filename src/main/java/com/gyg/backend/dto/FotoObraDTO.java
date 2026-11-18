package com.gyg.backend.dto;

import java.time.LocalDate;

public class FotoObraDTO {
    private Long id;
    private String url;
    private LocalDate fecha;
    private String descripcion;

    public FotoObraDTO() {
    }

    public FotoObraDTO(Long id, String url, LocalDate fecha, String descripcion) {
        this.id = id;
        this.url = url;
        this.fecha = fecha;
        this.descripcion = descripcion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
