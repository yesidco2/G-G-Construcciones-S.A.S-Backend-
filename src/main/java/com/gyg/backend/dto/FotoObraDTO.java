package com.gyg.backend.dto;

import java.time.LocalDate;

public class FotoObraDTO {
    private String url;
    private LocalDate fecha;

    public FotoObraDTO() {
    }

    public FotoObraDTO(String url, LocalDate fecha) {
        this.url = url;
        this.fecha = fecha;
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
}
