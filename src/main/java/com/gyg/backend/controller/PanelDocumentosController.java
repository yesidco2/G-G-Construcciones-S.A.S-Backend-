package com.gyg.backend.controller;

import com.gyg.backend.model.Usuario;
import com.gyg.backend.dto.DocumentoDTO;
import com.gyg.backend.service.DocumentosObraService;
import com.gyg.backend.service.PanelObrasService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/panel/obras/{obraId}/documentos")
public class PanelDocumentosController {
    private final PanelObrasService obras;
    private final DocumentosObraService documentos;

    public PanelDocumentosController(PanelObrasService obras, DocumentosObraService documentos) {
        this.obras = obras;
        this.documentos = documentos;
    }

    @GetMapping
    public List<DocumentoDTO> listar(@PathVariable long obraId, @AuthenticationPrincipal Usuario actor) {
        obras.obtener(obraId, actor);
        return documentos.listar(obraId);
    }

    @PostMapping
    public void subir(@PathVariable long obraId, @AuthenticationPrincipal Usuario actor,
                      @RequestParam MultipartFile archivo) throws IOException {
        obras.obtener(obraId, actor);
        documentos.guardar(obraId, archivo);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> ver(@PathVariable long obraId, @PathVariable long id, @AuthenticationPrincipal Usuario actor) {
        obras.obtener(obraId, actor);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_PDF)
                .body(documentos.leer(obraId, id));
    }
}
