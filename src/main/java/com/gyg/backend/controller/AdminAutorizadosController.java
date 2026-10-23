package com.gyg.backend.controller;

import com.gyg.backend.dto.AdministradorAutorizadoDTO;
import com.gyg.backend.dto.AutorizacionRequest;
import com.gyg.backend.model.Usuario;
import com.gyg.backend.service.AdministracionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/autorizados")
public class AdminAutorizadosController {

    private final AdministracionService administracionService;

    public AdminAutorizadosController(AdministracionService administracionService) {
        this.administracionService = administracionService;
    }

    @GetMapping
    public List<AdministradorAutorizadoDTO> listar() {
        return administracionService.listarAutorizados();
    }

    @PostMapping
    public ResponseEntity<AdministradorAutorizadoDTO> crear(
            @Valid @RequestBody AutorizacionRequest request,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(administracionService.crearAutorizacion(request, actor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario actor) {
        administracionService.eliminarAutorizacion(id, actor);
        return ResponseEntity.noContent().build();
    }
}
