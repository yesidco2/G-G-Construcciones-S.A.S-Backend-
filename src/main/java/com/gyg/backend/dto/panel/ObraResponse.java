package com.gyg.backend.dto.panel;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ObraResponse(
        Long id,
        String nombre,
        String cliente,
        Long clienteId,
        String categoria,
        String estado,
        Integer progreso,
        List<HitoObraResponse> hitos,
        List<FotoObraResponse> fotos,
        String codigoSeguimiento) {
}
