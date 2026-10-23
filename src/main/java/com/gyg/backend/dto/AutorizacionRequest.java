package com.gyg.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AutorizacionRequest(
        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no tiene un formato válido.")
        String email,
        @NotBlank(message = "El rol es obligatorio.")
        @Pattern(regexp = "^(INGENIERO|ADMIN)$",
                message = "El rol debe ser INGENIERO o ADMIN.")
        String rol) {
}
