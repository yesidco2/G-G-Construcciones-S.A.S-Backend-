package com.gyg.backend.controller;

import com.gyg.backend.dto.PerfilDTO;
import com.gyg.backend.model.Usuario;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public PerfilDTO obtenerPerfil(@AuthenticationPrincipal Usuario usuario) {
        boolean autorizado = usuario.getRol().name().equals("ADMIN")
                || usuario.getRol().name().equals("INGENIERO");
        return new PerfilDTO(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getRol(),
                autorizado,
                usuario.isEmailVerificado());
    }
}
