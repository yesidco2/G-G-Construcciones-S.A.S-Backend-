package com.gyg.backend.controller;

import com.gyg.backend.dto.ActualizarUsuarioRequest;
import com.gyg.backend.dto.UsuarioAdminDTO;
import com.gyg.backend.model.Usuario;
import com.gyg.backend.service.AdministracionService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminUsuariosController {

    private final AdministracionService administracionService;

    public AdminUsuariosController(AdministracionService administracionService) {
        this.administracionService = administracionService;
    }

    @GetMapping
    public List<UsuarioAdminDTO> listar() {
        return administracionService.listarUsuarios();
    }

    @PatchMapping("/{id}")
    public UsuarioAdminDTO actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarUsuarioRequest request,
            @AuthenticationPrincipal Usuario actor) {
        return administracionService.actualizarUsuario(id, request, actor);
    }
}
