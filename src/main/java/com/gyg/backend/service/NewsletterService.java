package com.gyg.backend.service;

import com.gyg.backend.dto.NewsletterRequest;
import com.gyg.backend.model.SuscripcionNewsletter;
import com.gyg.backend.repository.NewsletterRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class NewsletterService {

    private final NewsletterRepository repository;
    private final ContactCooldownLockService lockService;
    private final String policyVersion;

    public NewsletterService(
            NewsletterRepository repository,
            ContactCooldownLockService lockService,
            @Value("${app.privacy.policy-version:2026-01}") String policyVersion) {
        this.repository = repository;
        this.lockService = lockService;
        this.policyVersion = policyVersion;
    }

    @Transactional
    public void subscribe(NewsletterRequest request) {
        if (request == null || !Boolean.TRUE.equals(request.getAceptaPolitica())) {
            throw new IllegalArgumentException("Debes aceptar la política de tratamiento de datos.");
        }
        String normalizedEmail = request.getCorreo().trim().toLowerCase(Locale.ROOT);
        lockService.lockByEmail(normalizedEmail);
        if (repository.findByCorreo(normalizedEmail).isPresent()) {
            return;
        }

        SuscripcionNewsletter subscription = new SuscripcionNewsletter();
        subscription.setCorreo(normalizedEmail);
        subscription.setAceptaPolitica(true);
        subscription.setVersionPolitica(policyVersion);
        subscription.setFechaConsentimiento(LocalDateTime.now());
        repository.save(subscription);
    }
}
