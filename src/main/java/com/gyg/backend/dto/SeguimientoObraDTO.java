package com.gyg.backend.dto;

import java.util.List;

public class SeguimientoObraDTO {
    private String nombre;
    private Integer porcentajeAvance;
    private List<FotoObraDTO> fotos;

    public SeguimientoObraDTO() {
    }

    public SeguimientoObraDTO(String nombre, Integer porcentajeAvance, List<FotoObraDTO> fotos) {
        this.nombre = nombre;
        this.porcentajeAvance = porcentajeAvance;
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

    public List<FotoObraDTO> getFotos() {
        return fotos;
    }

    public void setFotos(List<FotoObraDTO> fotos) {
        this.fotos = fotos;
    }
}
