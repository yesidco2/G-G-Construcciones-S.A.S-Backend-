package com.gyg.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "epps")
public class EPP {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "labor_id", nullable = false)
    private Labor labor;

    public EPP() {
    }

    public EPP(String nombre, String tipo) {
        this.nombre = nombre;
        this.tipo = tipo;
    }

    public EPP(String nombre, String tipo, Labor labor) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.labor = labor;
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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Labor getLabor() {
        return labor;
    }

    public void setLabor(Labor labor) {
        this.labor = labor;
    }
}
