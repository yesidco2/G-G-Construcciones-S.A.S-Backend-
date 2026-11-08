package com.gyg.backend.controller;

import com.gyg.backend.dto.NewsletterRequest;
import com.gyg.backend.service.CaptchaService;
import com.gyg.backend.service.NewsletterService;
import com.gyg.backend.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/newsletter")
public class NewsletterController {

    private final NewsletterService newsletterService;
    private final CaptchaService captchaService;
    private final RateLimitService rateLimitService;
    private final int requestLimit;
    private final boolean captchaEnabled;

    public NewsletterController(
            NewsletterService newsletterService,
            CaptchaService captchaService,
            RateLimitService rateLimitService,
            @Value("${app.rate-limit.contacto-per-hour:5}") int requestLimit,
            @Value("${app.captcha.enabled:true}") boolean captchaEnabled) {
        this.newsletterService = newsletterService;
        this.captchaService = captchaService;
        this.rateLimitService = rateLimitService;
        this.requestLimit = requestLimit;
        this.captchaEnabled = captchaEnabled;
    }

    @PostMapping
    public ResponseEntity<Void> subscribe(
            @Valid @RequestBody NewsletterRequest request,
            @RequestHeader(name = "X-Captcha-Token", required = false) String captchaToken,
            HttpServletRequest httpRequest) {
        rateLimitService.check(
                "newsletter", httpRequest, requestLimit, Duration.ofHours(1),
                "Has alcanzado el límite de suscripciones. Intenta más tarde.");
        if (captchaEnabled) {
            captchaService.verifySubmissionToken(captchaToken, httpRequest);
        }
        newsletterService.subscribe(request);
        return ResponseEntity.noContent().build();
    }
}
