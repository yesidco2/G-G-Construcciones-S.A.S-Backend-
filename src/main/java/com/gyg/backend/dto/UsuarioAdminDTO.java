package com.gyg.backend.dto;

import com.gyg.backend.model.Rol;
import com.gyg.backend.model.Usuario;

public record UsuarioAdminDTO(Long id, String email, String nombre, Rol rol, boolean activo) {

    public static UsuarioAdminDTO from(Usuario usuario) {
        return new UsuarioAdminDTO(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getRol(),
                usuario.isActivo());
    }
}
