package com.gyg.backend.service;

import com.gyg.backend.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.gyg.backend.dto.DocumentoDTO;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
public class DocumentosObraService {

    private final JdbcTemplate db;
    public DocumentosObraService(JdbcTemplate db) { this.db = db; }

    public List<DocumentoDTO> listar(long obra) {
        return db.query("SELECT id, nombre FROM apoyo.documentos_obra WHERE obra_id = ? ORDER BY id",
                (rs, i) -> new DocumentoDTO(rs.getLong(1), rs.getString(2)), obra);
    }

    public byte[] leer(long obra, long id) {
        return db.query("SELECT contenido FROM apoyo.documentos_obra WHERE obra_id = ? AND id = ?",
                (rs, i) -> rs.getBytes(1), obra, id).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado."));
    }

    @Transactional
    public void guardar(long obra, MultipartFile archivo) throws IOException {
        if (archivo.isEmpty() || archivo.getSize() > 5L * 1024 * 1024) {
            throw new IllegalArgumentException("El PDF debe tener contenido y pesar hasta 5 MB.");
        }
        byte[] contenido = archivo.getBytes();
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || nombre.isBlank() || nombre.length() > 200 ||
                !new String(contenido, 0, Math.min(5, contenido.length), StandardCharsets.US_ASCII).equals("%PDF-")) {
            throw new IllegalArgumentException("Selecciona un archivo PDF válido con nombre de hasta 200 caracteres.");
        }
        String huella;
        try {
            huella = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        db.queryForObject("SELECT id FROM apoyo.obras_ingenieros WHERE id = ? AND eliminada_en IS NULL FOR UPDATE", Long.class, obra);
        if (db.queryForObject("SELECT COUNT(*) FROM apoyo.documentos_obra WHERE obra_id = ? AND huella = ?", Long.class, obra, huella) > 0) return;
        if (db.queryForObject("SELECT COUNT(*) FROM apoyo.documentos_obra WHERE obra_id = ?", Long.class, obra) >= 20) {
            throw new IllegalArgumentException("Cada obra admite hasta 20 documentos.");
        }
        db.update("INSERT INTO apoyo.documentos_obra(obra_id, nombre, huella, contenido) VALUES (?, ?, ?, ?)",
                obra, nombre, huella, contenido);
    }
}
