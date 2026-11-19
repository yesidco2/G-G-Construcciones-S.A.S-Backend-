package com.gyg.backend.dto.panel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gyg.backend.dto.FotoObraDTO;

import java.time.LocalDate;
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
        String descripcion,
        LocalDate fechaInicio,
        LocalDate fechaFinEstimada,
        List<HitoObraResponse> hitos,
        List<FotoObraDTO> fotos,
        String codigoSeguimiento) {

    @Override
    public String toString() {
        return "ObraResponse[id=" + id + "]";
    }
}
