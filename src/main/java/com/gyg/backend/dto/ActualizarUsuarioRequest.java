package com.gyg.backend.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarUsuarioRequest(
        @Size(max = 20)
        @Pattern(regexp = "^(INGENIERO|ADMIN)$",
                message = "El rol debe ser INGENIERO o ADMIN.")
        String rol,
        Boolean activo) {
}
