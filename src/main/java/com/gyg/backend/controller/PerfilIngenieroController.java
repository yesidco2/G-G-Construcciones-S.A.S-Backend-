package com.gyg.backend.controller;

import com.gyg.backend.model.Usuario;
import com.gyg.backend.service.ObraImageService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/panel/perfil")
public class PerfilIngenieroController {
    private final JdbcTemplate db;
    private final ObraImageService imagenes;

    public PerfilIngenieroController(JdbcTemplate db, ObraImageService imagenes) {
        this.db = db;
        this.imagenes = imagenes;
    }

    @GetMapping("/foto")
    public ResponseEntity<byte[]> foto(@AuthenticationPrincipal Usuario actor) {
        var fotos = db.query("SELECT contenido FROM apoyo.fotos_perfil WHERE usuario_id = ?",
                (rs, i) -> rs.getBytes(1), actor.getId());
        return fotos.isEmpty() ? ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build()
                : ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG).body(fotos.get(0));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public void guardar(@AuthenticationPrincipal Usuario actor, @RequestParam(required = false) String nombre,
                        @RequestParam(required = false) MultipartFile foto) {
        if (nombre != null && (nombre.isBlank() || nombre.strip().length() > 100)) {
            throw new IllegalArgumentException("Ingresa un nombre de hasta 100 caracteres.");
        }
        byte[] imagen = foto == null ? null : imagenes.sanitize(foto);
        db.queryForObject("SELECT id FROM public.administradores_autorizados WHERE id = ? FOR UPDATE", Long.class, actor.getId());
        if (nombre != null) {
            db.update("UPDATE public.administradores_autorizados SET nombre = ? WHERE id = ?", nombre.strip(), actor.getId());
        }
        if (imagen != null) {
            db.update("DELETE FROM apoyo.fotos_perfil WHERE usuario_id = ?", actor.getId());
            db.update("INSERT INTO apoyo.fotos_perfil(usuario_id, contenido) VALUES (?, ?)", actor.getId(), imagen);
        }
    }
}
