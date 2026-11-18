package com.gyg.backend.controller;

import com.gyg.backend.model.Usuario;
import com.gyg.backend.service.EmailVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/email-verification")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping
    public ResponseEntity<EmailVerificationResponse> request(
            @AuthenticationPrincipal Usuario actor,
            HttpServletRequest request) {
        EmailVerificationService.RequestResult result = emailVerificationService.request(actor, request);
        if (result == EmailVerificationService.RequestResult.ALREADY_VERIFIED) {
            return ResponseEntity.ok(new EmailVerificationResponse("already_verified"));
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new EmailVerificationResponse("sent"));
    }

    public record EmailVerificationResponse(String status) {
    }
}
