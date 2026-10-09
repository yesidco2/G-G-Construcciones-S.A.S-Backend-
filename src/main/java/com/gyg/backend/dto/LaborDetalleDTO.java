package com.gyg.backend.dto;

import java.util.List;

public class LaborDetalleDTO {
    private Long id;
    private String nombre;
    private List<RiesgoDTO> riesgos;
    private List<EppDTO> epps;

    public LaborDetalleDTO() {
    }

    public LaborDetalleDTO(Long id, String nombre, List<RiesgoDTO> riesgos, List<EppDTO> epps) {
        this.id = id;
        this.nombre = nombre;
        this.riesgos = riesgos;
        this.epps = epps;
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

    public List<RiesgoDTO> getRiesgos() {
        return riesgos;
    }

    public void setRiesgos(List<RiesgoDTO> riesgos) {
        this.riesgos = riesgos;
    }

    public List<EppDTO> getEpps() {
        return epps;
    }

    public void setEpps(List<EppDTO> epps) {
        this.epps = epps;
    }
}
