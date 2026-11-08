package com.gyg.backend.dto.panel;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@JsonIgnoreProperties({"id", "rol", "role", "roles"})
public class HitoObraRequest {

    @jakarta.validation.constraints.NotBlank
    @Size(max = 255)
    private String nombre;

    @Size(max = 1000)
    private String descripcion;

    private LocalDate fechaObjetivo;
    private boolean completado;

    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object value) {
        throw new IllegalArgumentException("La solicitud contiene un campo no permitido.");
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDate getFechaObjetivo() {
        return fechaObjetivo;
    }

    public void setFechaObjetivo(LocalDate fechaObjetivo) {
        this.fechaObjetivo = fechaObjetivo;
    }

    public boolean isCompletado() {
        return completado;
    }

    public void setCompletado(boolean completado) {
        this.completado = completado;
    }
}
