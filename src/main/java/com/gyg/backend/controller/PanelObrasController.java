package com.gyg.backend.controller;

import com.gyg.backend.dto.panel.EstadoObraRequest;
import com.gyg.backend.dto.panel.ObraRequest;
import com.gyg.backend.dto.panel.ObraResponse;
import com.gyg.backend.dto.panel.ProgresoObraRequest;
import com.gyg.backend.model.Usuario;
import com.gyg.backend.service.PanelObrasService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/panel/obras")
@Validated
public class PanelObrasController {

    private final PanelObrasService panelObrasService;

    public PanelObrasController(PanelObrasService panelObrasService) {
        this.panelObrasService = panelObrasService;
    }

    @GetMapping
    public ResponseEntity<List<ObraResponse>> listar(@AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(panelObrasService.listar(actor));
    }

    @GetMapping("/{obraId}")
    public ResponseEntity<ObraResponse> obtener(
            @PathVariable @Positive Long obraId,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(panelObrasService.obtener(obraId, actor));
    }

    @PostMapping
    public ResponseEntity<ObraResponse> crear(
            @Valid @RequestBody ObraRequest request,
            @AuthenticationPrincipal Usuario actor,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(panelObrasService.crear(request, actor, httpRequest.getRemoteAddr()));
    }

    @PutMapping("/{obraId}")
    public ResponseEntity<ObraResponse> actualizar(
            @PathVariable @Positive Long obraId,
            @Valid @RequestBody ObraRequest request,
            @AuthenticationPrincipal Usuario actor,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(
                panelObrasService.actualizar(obraId, request, actor, httpRequest.getRemoteAddr()));
    }

    @PatchMapping("/{obraId}/estado")
    public ResponseEntity<ObraResponse> cambiarEstado(
            @PathVariable @Positive Long obraId,
            @Valid @RequestBody EstadoObraRequest request,
            @AuthenticationPrincipal Usuario actor,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(
                panelObrasService.cambiarEstado(obraId, request, actor, httpRequest.getRemoteAddr()));
    }

    @PatchMapping("/{obraId}/progreso")
    public ResponseEntity<ObraResponse> cambiarProgreso(
            @PathVariable @Positive Long obraId,
            @Valid @RequestBody ProgresoObraRequest request,
            @AuthenticationPrincipal Usuario actor,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(
                panelObrasService.cambiarProgreso(obraId, request, actor, httpRequest.getRemoteAddr()));
    }

    @DeleteMapping("/{obraId}")
    public ResponseEntity<Void> eliminar(
            @PathVariable @Positive Long obraId,
            @AuthenticationPrincipal Usuario actor,
            HttpServletRequest httpRequest) {
        panelObrasService.eliminarLogicamente(obraId, actor, httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{obraId}/ingenieros/{usuarioId}")
    public ResponseEntity<ObraResponse> asignarIngeniero(
            @PathVariable @Positive Long obraId,
            @PathVariable @Positive Long usuarioId,
            @AuthenticationPrincipal Usuario actor,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(
                panelObrasService.asignarIngeniero(obraId, usuarioId, actor, httpRequest.getRemoteAddr()));
    }

    @PostMapping(path = "/{obraId}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ObraResponse> subirFoto(
            @PathVariable @Positive Long obraId,
            @RequestPart("archivo") MultipartFile archivo,
            @AuthenticationPrincipal Usuario actor,
            HttpServletRequest httpRequest) throws IOException {
        return ResponseEntity.ok(
                panelObrasService.subirFoto(obraId, archivo, actor, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/{obraId}/fotos/{fotoId}")
    public ResponseEntity<byte[]> obtenerFoto(
            @PathVariable @Positive Long obraId,
            @PathVariable @Positive Long fotoId,
            @AuthenticationPrincipal Usuario actor) throws IOException {
        PanelObrasService.StoredImage image = panelObrasService.obtenerFoto(obraId, fotoId, actor);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(image.contentType()));
        headers.setContentDisposition(ContentDisposition.inline()
                .filename("foto-obra.png", StandardCharsets.UTF_8)
                .build());
        headers.set("X-Content-Type-Options", "nosniff");
        return new ResponseEntity<>(image.content(), headers, HttpStatus.OK);
    }
}
