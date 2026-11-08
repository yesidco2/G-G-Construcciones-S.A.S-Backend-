package com.gyg.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AutorizacionRequest(
        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no tiene un formato válido.")
        @Size(max = 254, message = "El correo no puede superar 254 caracteres.")
        String email,
        @NotBlank(message = "El rol es obligatorio.")
        @Size(max = 20)
        @Pattern(regexp = "^(INGENIERO|ADMIN)$",
                message = "El rol debe ser INGENIERO o ADMIN.")
        String rol) {
}
