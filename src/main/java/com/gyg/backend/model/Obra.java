package com.gyg.backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
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

    @Column(name = "codigo_hash", nullable = false, unique = true, length = 64)
    private String codigoHash;

    @Column(name = "codigo_legacy", nullable = false)
    private boolean codigoLegacy;

    @Column(nullable = false)
    private String estado = "EN_PROGRESO";

    @Column(name = "porcentaje_avance", nullable = false)
    private Integer porcentajeAvance = 0;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "cliente", nullable = false)
    private String cliente = "Cliente";

    @Column(name = "categoria", nullable = false)
    private String categoria = "CIVIL";

    @Column(name = "descripcion", length = 2000)
    private String descripcion;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin_estimada")
    private LocalDate fechaFinEstimada;

    @Column(name = "eliminada_en")
    private LocalDateTime eliminadaEn;

    @OneToMany(mappedBy = "obra", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FotoObra> fotos = new ArrayList<>();

    @OneToMany(mappedBy = "obra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HitoObra> hitos = new ArrayList<>();

    public Obra() {
    }

    public Obra(String nombreObra, String estado, Long clienteId) {
        this.nombreObra = nombreObra;
        this.estado = estado;
        this.clienteId = clienteId;
    }

    public Obra(String nombreObra, String estado, Long clienteId, Integer porcentajeAvance) {
        this(nombreObra, estado, clienteId);
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

    public String getCodigoHash() {
        return codigoHash;
    }

    public void setCodigoHash(String codigoHash) {
        this.codigoHash = codigoHash;
    }

    public boolean isCodigoLegacy() {
        return codigoLegacy;
    }

    public void setCodigoLegacy(boolean codigoLegacy) {
        this.codigoLegacy = codigoLegacy;
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

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
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

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFinEstimada() {
        return fechaFinEstimada;
    }

    public void setFechaFinEstimada(LocalDate fechaFinEstimada) {
        this.fechaFinEstimada = fechaFinEstimada;
    }

    public LocalDateTime getEliminadaEn() {
        return eliminadaEn;
    }

    public void setEliminadaEn(LocalDateTime eliminadaEn) {
        this.eliminadaEn = eliminadaEn;
    }

    public List<FotoObra> getFotos() {
        return fotos;
    }

    public void setFotos(List<FotoObra> fotos) {
        this.fotos = fotos;
    }

    public List<HitoObra> getHitos() {
        return hitos;
    }

    public void setHitos(List<HitoObra> hitos) {
        this.hitos = hitos;
    }

    public void addHito(HitoObra hito) {
        hitos.add(hito);
        hito.setObra(this);
    }

    public void clearHitos() {
        hitos.clear();
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
