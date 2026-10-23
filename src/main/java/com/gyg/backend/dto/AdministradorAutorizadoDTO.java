package com.gyg.backend.dto;

import com.gyg.backend.model.AdministradorAutorizado;
import com.gyg.backend.model.Rol;

import java.time.LocalDateTime;

public record AdministradorAutorizadoDTO(
        Long id,
        String email,
        Rol rol,
        boolean activo,
        String creadoPor,
        LocalDateTime creadoEn) {

    public static AdministradorAutorizadoDTO from(AdministradorAutorizado autorizado) {
        return new AdministradorAutorizadoDTO(
                autorizado.getId(),
                autorizado.getEmail(),
                autorizado.getRol(),
                autorizado.isActivo(),
                autorizado.getCreadoPor(),
                autorizado.getCreadoEn());
    }
}
