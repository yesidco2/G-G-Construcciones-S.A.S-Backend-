package com.gyg.backend.dto.panel;

import java.time.LocalDate;

public record FotoObraResponse(Long id, LocalDate fecha, String subidoPor, String url) {
}
