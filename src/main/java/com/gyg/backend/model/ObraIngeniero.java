package com.gyg.backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "obras_ingenieros",
        uniqueConstraints = @UniqueConstraint(name = "uk_obra_ingeniero", columnNames = {"obra_id", "usuario_id"}))
public class ObraIngeniero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obra_id", nullable = false)
    private Obra obra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "asignado_en", nullable = false)
    private LocalDateTime asignadoEn = LocalDateTime.now();

    @Column(name = "asignado_por", nullable = false)
    private String asignadoPor;

    public ObraIngeniero() {
    }

    public ObraIngeniero(Obra obra, Usuario usuario, String asignadoPor) {
        this.obra = obra;
        this.usuario = usuario;
        this.asignadoPor = asignadoPor;
    }

    public Long getId() {
        return id;
    }

    public Obra getObra() {
        return obra;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDateTime getAsignadoEn() {
        return asignadoEn;
    }

    public String getAsignadoPor() {
        return asignadoPor;
    }
}
