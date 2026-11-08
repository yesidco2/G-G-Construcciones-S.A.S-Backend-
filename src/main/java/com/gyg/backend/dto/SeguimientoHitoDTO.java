package com.gyg.backend.dto;

import java.time.LocalDate;

public record SeguimientoHitoDTO(
        String nombre,
        String descripcion,
        LocalDate fechaObjetivo,
        boolean completado) {
}
