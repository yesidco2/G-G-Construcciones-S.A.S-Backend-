package com.gyg.backend.dto.panel;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties({"id", "rol", "role", "roles"})
public class ObraUpdateRequest {

    private String nombre;
    private Long clienteId;
    private String categoria;
    private EstadoObra estado;
    private Integer progreso;
    private String descripcion;
    private LocalDate fechaInicio;
    private LocalDate fechaFinEstimada;
    private List<HitoObraRequest> hitos;
    private boolean nombrePresent;
    private boolean clienteIdPresent;
    private boolean categoriaPresent;
    private boolean estadoPresent;
    private boolean progresoPresent;
    private boolean descripcionPresent;
    private boolean fechaInicioPresent;
    private boolean fechaFinEstimadaPresent;
    private boolean hitosPresent;

    public boolean hasNombre() {
        return nombrePresent;
    }

    public boolean hasClienteId() {
        return clienteIdPresent;
    }

    public boolean hasCategoria() {
        return categoriaPresent;
    }

    public boolean hasEstado() {
        return estadoPresent;
    }

    public boolean hasProgreso() {
        return progresoPresent;
    }

    public boolean hasDescripcion() {
        return descripcionPresent;
    }

    public boolean hasFechaInicio() {
        return fechaInicioPresent;
    }

    public boolean hasFechaFinEstimada() {
        return fechaFinEstimadaPresent;
    }

    public boolean hasHitos() {
        return hitosPresent;
    }

    @Size(min = 1, max = 255)
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombrePresent = true;
        this.nombre = nombre;
    }

    @Positive
    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteIdPresent = true;
        this.clienteId = clienteId;
    }

    @Pattern(regexp = "CIVIL|SST")
    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoriaPresent = true;
        this.categoria = categoria;
    }

    public EstadoObra getEstado() {
        return estado;
    }

    public void setEstado(EstadoObra estado) {
        this.estadoPresent = true;
        this.estado = estado;
    }

    @Min(0)
    @Max(100)
    public Integer getProgreso() {
        return progreso;
    }

    public void setProgreso(Integer progreso) {
        this.progresoPresent = true;
        this.progreso = progreso;
    }

    @Size(max = 2000)
    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcionPresent = true;
        this.descripcion = descripcion;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicioPresent = true;
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFinEstimada() { return fechaFinEstimada; }
    public void setFechaFinEstimada(LocalDate fechaFinEstimada) {
        this.fechaFinEstimadaPresent = true;
        this.fechaFinEstimada = fechaFinEstimada;
    }

    @Valid
    @Size(max = 50)
    public List<HitoObraRequest> getHitos() {
        return hitos;
    }

    public void setHitos(List<HitoObraRequest> hitos) {
        this.hitosPresent = true;
        this.hitos = hitos;
    }

    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object value) {
        throw new IllegalArgumentException("La solicitud contiene un campo no permitido.");
    }
}
