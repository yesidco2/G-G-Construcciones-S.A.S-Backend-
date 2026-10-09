package com.gyg.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "riesgos")
public class Riesgo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_severidad", nullable = false)
    private Severidad nivelSeveridad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "labor_id", nullable = false)
    private Labor labor;

    public Riesgo() {
    }

    public Riesgo(String descripcion, Severidad nivelSeveridad) {
        this.descripcion = descripcion;
        this.nivelSeveridad = nivelSeveridad;
    }

    public Riesgo(String descripcion, Severidad nivelSeveridad, Labor labor) {
        this.descripcion = descripcion;
        this.nivelSeveridad = nivelSeveridad;
        this.labor = labor;
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

    public Labor getLabor() {
        return labor;
    }

    public void setLabor(Labor labor) {
        this.labor = labor;
    }
}
