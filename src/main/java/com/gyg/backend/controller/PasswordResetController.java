package com.gyg.backend.controller;

import com.gyg.backend.dto.PasswordResetRequest;
import com.gyg.backend.service.CaptchaService;
import com.gyg.backend.service.PasswordResetService;
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
@RequestMapping("/api/auth/password-reset")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final CaptchaService captchaService;
    private final RateLimitService rateLimitService;
    private final int requestLimit;

    public PasswordResetController(
            PasswordResetService passwordResetService,
            CaptchaService captchaService,
            RateLimitService rateLimitService,
            @Value("${app.rate-limit.password-reset-per-hour:5}") int requestLimit) {
        this.passwordResetService = passwordResetService;
        this.captchaService = captchaService;
        this.rateLimitService = rateLimitService;
        this.requestLimit = requestLimit;
    }

    @PostMapping
    public ResponseEntity<Void> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request,
            @RequestHeader(name = "X-Captcha-Token", required = false) String captchaToken,
            HttpServletRequest httpRequest) {
        rateLimitService.check(
                "password-reset", httpRequest, requestLimit, Duration.ofHours(1),
                "Demasiadas solicitudes. Intenta nuevamente más tarde.");
        captchaService.verifySubmissionToken(captchaToken, "recuperar_contraseña", httpRequest);
        passwordResetService.request(request.email());
        return ResponseEntity.accepted().build();
    }
}
