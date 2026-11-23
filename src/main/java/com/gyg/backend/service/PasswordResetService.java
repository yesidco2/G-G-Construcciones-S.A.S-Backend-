package com.gyg.backend.service;

import com.gyg.backend.model.Rol;
import com.gyg.backend.repository.AdministradorAutorizadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.Locale;

@Service
public class PasswordResetService {
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    private final AdministradorAutorizadoRepository authorizationRepository;
    private final SupabaseAuth auth;

    public PasswordResetService(AdministradorAutorizadoRepository authorizationRepository, SupabaseAuth auth) {
        this.authorizationRepository = authorizationRepository;
        this.auth = auth;
    }

    public void request(String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        boolean authorized = authorizationRepository.findByEmail(normalized)
                .filter(entry -> entry.isActivo() && (entry.getRol() == Rol.ADMIN || entry.getRol() == Rol.INGENIERO)).isPresent();
        if (!authorized) return;
        try {
            auth.recover(normalized);
        } catch (RestClientException | IllegalStateException failure) {
            logger.warn("Supabase no pudo procesar una solicitud de recuperaci?n.");
        }
    }
}
