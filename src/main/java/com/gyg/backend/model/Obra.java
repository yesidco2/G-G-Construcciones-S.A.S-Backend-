package com.gyg.backend.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@DynamicUpdate
@Table(name = "obras_ingenieros", schema = "apoyo")
public class Obra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_obra", nullable = false)
    private String nombreObra;

    @Column(name = "codigo_hash", nullable = false, unique = true, length = 64)
    private String codigoHash;

    @JsonIgnore
    @Column(name = "codigo_cifrado", length = 160)
    private String codigoCifrado;

    @Column(name = "codigo_legacy", nullable = false)
    private boolean codigoLegacy;

    @Column(nullable = false)
    private String estado = "EN_PROGRESO";

    @Column(name = "porcentaje_avance", nullable = false)
    private Integer porcentajeAvance = 0;

    @Column(name = "cliente_id")
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<HitoObra> hitos = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<AsignacionObra> asignaciones = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<FotoObraGuardada> fotos = new ArrayList<>();

    public List<AsignacionObra> getAsignaciones() {
        return asignaciones;
    }

    public List<FotoObraGuardada> getFotos() {
        return fotos;
    }

    public boolean estaAsignadaA(Long usuarioId) {
        return usuarioId != null && asignaciones.stream().anyMatch(a -> usuarioId.equals(a.usuarioId()));
    }

    public FotoObraGuardada buscarFoto(Long fotoId) {
        return fotos.stream().filter(foto -> fotoId.equals(foto.id())).findFirst()
                .orElseThrow(() -> new com.gyg.backend.exception.ResourceNotFoundException("No existe la foto solicitada."));
    }

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

    public String getCodigoCifrado() {
        return codigoCifrado;
    }

    public void setCodigoCifrado(String codigoCifrado) {
        this.codigoCifrado = codigoCifrado;
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

    public List<HitoObra> getHitos() {
        return hitos;
    }

    public void setHitos(List<HitoObra> hitos) {
        this.hitos = hitos;
    }

    public void addHito(HitoObra hito) {
        hitos.add(hito);
    }

    public void clearHitos() {
        hitos.clear();
    }

}
