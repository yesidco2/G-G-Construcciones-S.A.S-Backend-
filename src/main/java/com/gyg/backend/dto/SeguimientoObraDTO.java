package com.gyg.backend.dto;

import java.util.List;

public class SeguimientoObraDTO {
    private List<DocumentoDTO> documentos = List.of();

    public List<DocumentoDTO> getDocumentos() { return documentos; }
    public void setDocumentos(List<DocumentoDTO> documentos) { this.documentos = documentos; }

    private String nombre;
    private Integer porcentajeAvance;
    private List<SeguimientoHitoDTO> hitos;
    private List<FotoObraDTO> fotos;

    public SeguimientoObraDTO() {
    }

    public SeguimientoObraDTO(
            String nombre, Integer porcentajeAvance, List<SeguimientoHitoDTO> hitos, List<FotoObraDTO> fotos) {
        this.nombre = nombre;
        this.porcentajeAvance = porcentajeAvance;
        this.hitos = hitos;
        this.fotos = fotos;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getPorcentajeAvance() {
        return porcentajeAvance;
    }

    public void setPorcentajeAvance(Integer porcentajeAvance) {
        this.porcentajeAvance = porcentajeAvance;
    }

    public List<SeguimientoHitoDTO> getHitos() {
        return hitos;
    }

    public List<FotoObraDTO> getFotos() {
        return fotos;
    }

    public void setHitos(List<SeguimientoHitoDTO> hitos) {
        this.hitos = hitos;
    }
}
