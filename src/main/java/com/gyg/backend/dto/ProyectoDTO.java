package com.gyg.backend.dto;

public class ProyectoDTO {
    private Long id;
    private String nombre;
    private String categoria;
    private String descripcion;
    private String metricaDestacada;
    private String imagenUrl;

    public ProyectoDTO() {
    }

    public ProyectoDTO(Long id, String nombre, String categoria, String descripcion, String metricaDestacada, String imagenUrl) {
        this.id = id;
        this.nombre = nombre;
        this.categoria = categoria;
        this.descripcion = descripcion;
        this.metricaDestacada = metricaDestacada;
        this.imagenUrl = imagenUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getMetricaDestacada() {
        return metricaDestacada;
    }

    public void setMetricaDestacada(String metricaDestacada) {
        this.metricaDestacada = metricaDestacada;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }
}
