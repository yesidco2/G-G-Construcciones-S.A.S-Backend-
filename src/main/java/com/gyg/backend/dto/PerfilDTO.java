package com.gyg.backend.dto;

import com.gyg.backend.model.Rol;

public record PerfilDTO(
        Long id,
        String email,
        String nombre,
        Rol rol,
        boolean autorizado,
        boolean emailVerificado) {
}
