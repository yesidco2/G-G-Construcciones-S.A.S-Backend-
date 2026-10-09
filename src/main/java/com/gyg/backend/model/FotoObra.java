package com.gyg.backend.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "foto_obra")
public class FotoObra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private LocalDate fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obra_id", nullable = false)
    private Obra obra;

    public FotoObra() {
    }

    public FotoObra(String url, LocalDate fecha) {
        this.url = url;
        this.fecha = fecha;
    }

    public FotoObra(String url, LocalDate fecha, Obra obra) {
        this.url = url;
        this.fecha = fecha;
        this.obra = obra;
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

    public Obra getObra() {
        return obra;
    }

    public void setObra(Obra obra) {
        this.obra = obra;
    }
}
