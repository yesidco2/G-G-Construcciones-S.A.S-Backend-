package com.gyg.backend.dto.panel;

import java.time.LocalDate;

public record FotoObraResponse(Long id, String url, LocalDate fecha, String descripcion, String subidoPor) {
}
