package com.gyg.backend.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "servicios")
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "descripcion", nullable = false, length = 2000)
    private String descripcion;

    @Column(name = "precio_base_m2", nullable = false)
    private BigDecimal precioBaseM2 = BigDecimal.ZERO;

    @Column(name = "categoria", nullable = false)
    private String categoria;

    @Column(name = "activo")
    private Boolean activo = true;

    public Servicio() {
    }

    public Servicio(String titulo, String descripcion, Bloque bloque) {
        this.nombre = titulo;
        this.descripcion = descripcion;
        this.categoria = normalizarCategoria(bloque);
        this.precioBaseM2 = BigDecimal.ZERO;
        this.activo = true;
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

    public String getTitulo() {
        return nombre;
    }

    public void setTitulo(String titulo) {
        this.nombre = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecioBaseM2() {
        return precioBaseM2;
    }

    public void setPrecioBaseM2(BigDecimal precioBaseM2) {
        this.precioBaseM2 = precioBaseM2;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Bloque getBloque() {
        if (categoria == null) {
            return Bloque.CIVIL;
        }
        String normalized = categoria.trim();
        if ("INGENIERIA_CIVIL".equalsIgnoreCase(normalized) || "CONSULTORIA".equalsIgnoreCase(normalized)
                || "CIVIL".equalsIgnoreCase(normalized)) {
            return Bloque.CIVIL;
        }
        return Bloque.SST;
    }

    public void setBloque(Bloque bloque) {
        this.categoria = normalizarCategoria(bloque);
    }

    private String normalizarCategoria(Bloque bloque) {
        if (bloque == null) {
            return Bloque.CIVIL.name();
        }
        return bloque.name();
    }
}
