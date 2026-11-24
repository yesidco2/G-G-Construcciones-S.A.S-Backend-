package com.gyg.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "obras_ingenieros", schema = "apoyo")
public class ContenidoFotosObra {
    @Id
    private Long id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contenido_fotos")
    private Map<String, String> contenido;

    public Map<String, String> getContenido() {
        return contenido == null ? Map.of() : contenido;
    }

    public void guardar(Long fotoId, String base64) {
        var updated = new HashMap<>(getContenido());
        updated.put(fotoId.toString(), base64);
        contenido = updated;
    }

    public void eliminar(Long fotoId) {
        var updated = new HashMap<>(getContenido());
        updated.remove(fotoId.toString());
        contenido = updated;
    }
}
