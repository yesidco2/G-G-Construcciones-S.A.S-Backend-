package com.gyg.backend.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;

public class SeguimientoRequest {

    private String codigo;

    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object value) {
        throw new IllegalArgumentException("La solicitud contiene un campo no permitido.");
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
}
