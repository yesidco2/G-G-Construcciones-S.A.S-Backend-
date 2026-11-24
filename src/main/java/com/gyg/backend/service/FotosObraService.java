package com.gyg.backend.service;

import com.gyg.backend.exception.ResourceNotFoundException;
import com.gyg.backend.model.ContenidoFotosObra;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class FotosObraService {
    private final EntityManager entityManager;

    public FotosObraService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public ContenidoFotosObra contenido(Long obraId) {
        ContenidoFotosObra fotos = entityManager.find(ContenidoFotosObra.class, obraId);
        if (fotos == null) {
            throw new ResourceNotFoundException("No existe la obra solicitada.");
        }
        return fotos;
    }

    public byte[] obtener(Long obraId, Long fotoId) {
        String base64 = contenido(obraId).getContenido().get(fotoId.toString());
        if (base64 == null) {
            throw new ResourceNotFoundException("No existe la foto solicitada.");
        }
        return Base64.getDecoder().decode(base64);
    }
}
