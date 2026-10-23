package com.gyg.backend.dto;

import jakarta.validation.constraints.Pattern;

public record ActualizarUsuarioRequest(
        @Pattern(regexp = "^(USUARIO|INGENIERO|ADMIN)$",
                message = "El rol debe ser USUARIO, INGENIERO o ADMIN.")
        String rol,
        Boolean activo) {
}
