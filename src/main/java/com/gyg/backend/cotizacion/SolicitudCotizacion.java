package com.gyg.backend.cotizacion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record SolicitudCotizacion(
        @NotBlank @Size(max = 160) String nombre,
        @NotBlank @Email @Size(max = 254) String correo,
        @NotBlank @Size(max = 32)
        @Pattern(regexp = "(?=(?:\\D*\\d){7,15}\\D*$)\\+?[\\d ()-]+") String telefono,
        @NotBlank @Size(max = 120) String ciudad,
        @NotBlank @Size(max = 2000) String descripcion,
        @Size(max = 30) String nit,
        @Size(max = 200) String direccion,
        @NotEmpty @Size(max = 40) List<@NotNull @Valid Item> items) {

    public record Item(@NotBlank @Size(max = 80) String servicioId,
                       @NotNull @DecimalMin("0.01") @DecimalMax("999999.99")
                       @Digits(integer = 6, fraction = 2) BigDecimal cantidad) {}

    public VistaCotizacion.Cliente cliente() {
        return new VistaCotizacion.Cliente(nombre.strip(), correo.strip(), telefono.strip(),
                ciudad.strip(), descripcion.strip(), opcional(nit), opcional(direccion));
    }

    private static String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
