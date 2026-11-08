package com.gyg.backend.dto.panel;

import java.time.LocalDate;

public record HitoObraResponse(
        Long id,
        String nombre,
        String descripcion,
        LocalDate fechaObjetivo,
        boolean completado) {
}
