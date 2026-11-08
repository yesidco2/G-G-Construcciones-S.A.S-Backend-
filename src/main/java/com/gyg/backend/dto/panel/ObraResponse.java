package com.gyg.backend.dto.panel;

import java.util.List;

public record ObraResponse(
        Long id,
        String nombre,
        String cliente,
        Long clienteId,
        String categoria,
        String estado,
        Integer progreso,
        List<HitoObraResponse> hitos,
        List<FotoObraResponse> fotos) {
}
