package com.gyg.backend.controller;

import com.gyg.backend.dto.FotoObraDTO;
import com.gyg.backend.model.Usuario;
import com.gyg.backend.service.PanelObrasService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/panel/obras/{obraId}/fotos")
public class PanelFotosObraController {
    private final PanelObrasService obras;

    public PanelFotosObraController(PanelObrasService obras) {
        this.obras = obras;
    }

    @GetMapping
    public ResponseEntity<List<FotoObraDTO>> listar(@PathVariable @Positive Long obraId,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(obras.obtener(obraId, actor).fotos());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FotoObraDTO subir(@PathVariable @Positive Long obraId, @RequestPart("archivo") MultipartFile archivo,
            @RequestPart(value = "descripcion", required = false) String descripcion,
            @AuthenticationPrincipal Usuario actor, HttpServletRequest request) {
        return obras.subirFoto(obraId, archivo, descripcion, actor, request.getRemoteAddr());
    }

    @GetMapping("/{fotoId}")
    public ResponseEntity<byte[]> obtener(@PathVariable @Positive Long obraId, @PathVariable @Positive Long fotoId,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(obras.obtenerFoto(obraId, fotoId, actor));
    }

    @DeleteMapping("/{fotoId}")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long obraId, @PathVariable @Positive Long fotoId,
            @AuthenticationPrincipal Usuario actor, HttpServletRequest request) {
        obras.eliminarFoto(obraId, fotoId, actor, request.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
