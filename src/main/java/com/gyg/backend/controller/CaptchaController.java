package com.gyg.backend.controller;

import com.gyg.backend.service.CaptchaService;
import com.gyg.backend.service.RateLimitService;
import com.gyg.backend.service.VerifiedSessionService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class CaptchaController {

    private final CaptchaService captchaService;
    private final VerifiedSessionService verifiedSessionService;
    private final RateLimitService rateLimitService;
    private final boolean captchaEnabled;

    public CaptchaController(
            CaptchaService captchaService,
            VerifiedSessionService verifiedSessionService,
            RateLimitService rateLimitService,
            @Value("${app.captcha.enabled:true}") boolean captchaEnabled) {
        this.captchaService = captchaService;
        this.verifiedSessionService = verifiedSessionService;
        this.rateLimitService = rateLimitService;
        this.captchaEnabled = captchaEnabled;
    }

    @PostMapping("/captcha/verify")
    public CaptchaService.TicketResponse verify(
            @RequestBody(required = false) CaptchaRequest captchaRequest,
            HttpServletRequest request) {
        rateLimitService.check(
                "captcha-verify", request, 10, Duration.ofMinutes(1),
                "Has alcanzado el límite de verificaciones CAPTCHA. Intenta más tarde.");
        JsonNode tokenNode = captchaRequest == null ? null : captchaRequest.captchaToken();
        String captchaToken = tokenNode != null && tokenNode.isTextual() ? tokenNode.textValue() : null;
        return captchaService.verifyAndIssue(captchaToken, "login", request);
    }

    @PostMapping("/session")
    public ResponseEntity<?> createSession(
            @Valid @RequestBody(required = false) SessionRequest sessionRequest,
            HttpServletRequest request) {
        var firebaseToken = VerifiedSessionService.firebaseToken(request);
        if (firebaseToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", 401, "error", "No autorizado"));
        }

        if (captchaEnabled && (sessionRequest == null
                || !captchaService.consumeTicket(sessionRequest.ticket(), request))) {
            return captchaRequired();
        }
        try {
            Instant expiresAt = verifiedSessionService.create(firebaseToken);
            return ResponseEntity.ok(new SessionResponse(true, expiresAt));
        } catch (IllegalArgumentException exception) {
            return captchaRequired();
        }
    }

    private static ResponseEntity<Map<String, Object>> captchaRequired() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "status", 403,
                "error", "Acceso denegado",
                "code", "CAPTCHA_REQUIRED",
                "message", "Completa la verificación CAPTCHA para continuar."));
    }

    public record CaptchaRequest(JsonNode captchaToken) {
    }

    public record SessionRequest(@Size(max = 128) String ticket) {
    }

    public record SessionResponse(boolean verified, Instant expiresAt) {
    }
}
