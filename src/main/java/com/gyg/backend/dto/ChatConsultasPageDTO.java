package com.gyg.backend.dto;

import java.util.List;

public record ChatConsultasPageDTO(
        List<ChatConsultaDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last) {
}
