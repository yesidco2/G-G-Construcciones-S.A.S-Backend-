package com.gyg.backend.dto;

import com.gyg.backend.model.Severidad;

public class RiesgoDTO {
    private Long id;
    private String descripcion;
    private Severidad nivelSeveridad;

    public RiesgoDTO() {
    }

    public RiesgoDTO(Long id, String descripcion, Severidad nivelSeveridad) {
        this.id = id;
        this.descripcion = descripcion;
        this.nivelSeveridad = nivelSeveridad;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Severidad getNivelSeveridad() {
        return nivelSeveridad;
    }

    public void setNivelSeveridad(Severidad nivelSeveridad) {
        this.nivelSeveridad = nivelSeveridad;
    }
}
