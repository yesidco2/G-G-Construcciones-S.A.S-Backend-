package com.gyg.backend.dto;

public class DatosLegalesDTO {
    private String nit;
    private String direccion;

    public DatosLegalesDTO() {
    }

    public DatosLegalesDTO(String nit, String direccion) {
        this.nit = nit;
        this.direccion = direccion;
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }
}
