package com.gyg.backend.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "obras")
public class Obra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_obra", nullable = false)
    private String nombreObra;

    @Column(name = "codigo_seguimiento", nullable = false)
    private String codigoSeguimiento;

    @Column(nullable = false)
    private String estado = "EN_PROGRESO";

    @Column(name = "porcentaje_avance", nullable = false)
    private Integer porcentajeAvance = 0;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @OneToMany(mappedBy = "obra", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FotoObra> fotos = new ArrayList<>();

    public Obra() {
    }

    public Obra(String nombreObra, String codigoSeguimiento, String estado, Long clienteId) {
        this.nombreObra = nombreObra;
        this.codigoSeguimiento = codigoSeguimiento;
        this.estado = estado;
        this.clienteId = clienteId;
    }

    public Obra(String nombreObra, String codigoSeguimiento, String estado, Long clienteId, Integer porcentajeAvance) {
        this(nombreObra, codigoSeguimiento, estado, clienteId);
        this.porcentajeAvance = porcentajeAvance;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombreObra() {
        return nombreObra;
    }

    public void setNombreObra(String nombreObra) {
        this.nombreObra = nombreObra;
    }

    public String getCodigoSeguimiento() {
        return codigoSeguimiento;
    }

    public void setCodigoSeguimiento(String codigoSeguimiento) {
        this.codigoSeguimiento = codigoSeguimiento;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Integer getPorcentajeAvance() {
        return porcentajeAvance;
    }

    public void setPorcentajeAvance(Integer porcentajeAvance) {
        this.porcentajeAvance = porcentajeAvance;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public List<FotoObra> getFotos() {
        return fotos;
    }

    public void setFotos(List<FotoObra> fotos) {
        this.fotos = fotos;
    }

    public void addFoto(FotoObra foto) {
        fotos.add(foto);
        foto.setObra(this);
    }

    public void removeFoto(FotoObra foto) {
        fotos.remove(foto);
        foto.setObra(null);
    }
}
