package com.gyg.backend.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.gyg.backend.model.Usuario;
import com.gyg.backend.exception.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

@Service
public class EmailVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationService.class);

    private final ObjectProvider<FirebaseAuth> firebaseAuthProvider;
    private final CustomEmailService emailService;
    private final RateLimitService rateLimitService;
    private final int requestsPerHour;

    public EmailVerificationService(
            ObjectProvider<FirebaseAuth> firebaseAuthProvider,
            CustomEmailService emailService,
            RateLimitService rateLimitService,
            @org.springframework.beans.factory.annotation.Value(
                    "${app.rate-limit.email-verification-per-hour:3}") int requestsPerHour) {
        this.firebaseAuthProvider = firebaseAuthProvider;
        this.emailService = emailService;
        this.rateLimitService = rateLimitService;
        this.requestsPerHour = requestsPerHour;
    }

    public RequestResult request(Usuario actor, HttpServletRequest request) {
        if (actor == null || actor.getFirebaseUid() == null || !actor.isActivo()) {
            throw new AccessDeniedException("No se puede verificar esta cuenta.");
        }
        rateLimitService.check(
                "email-verification-ip", request, requestsPerHour, Duration.ofHours(1),
                "Has alcanzado el límite de reenvíos. Intenta nuevamente más tarde.");
        rateLimitService.check(
                "email-verification-account-" + hash(actor.getFirebaseUid()),
                request,
                requestsPerHour,
                Duration.ofHours(1),
                "Has alcanzado el límite de reenvíos. Intenta nuevamente más tarde.");

        FirebaseAuth firebaseAuth = firebaseAuthProvider.getIfAvailable();
        if (firebaseAuth == null || !emailService.isConfigured()) {
            throw new EmailDeliveryException();
        }

        UserRecord user;
        try {
            user = firebaseAuth.getUser(actor.getFirebaseUid());
        } catch (FirebaseAuthException exception) {
            logger.warn("Firebase email verification lookup failed with code {}.",
                    exception.getAuthErrorCode());
            throw new EmailDeliveryException(exception);
        }
        if (user.isDisabled()) {
            throw new AccessDeniedException("No se puede verificar esta cuenta.");
        }
        if (user.isEmailVerified()) {
            return RequestResult.ALREADY_VERIFIED;
        }
        String email = user.getEmail();
        if (email == null || email.isBlank()) {
            throw new AccessDeniedException("La cuenta no tiene un correo electrónico verificable.");
        }

        try {
            String actionLink = firebaseAuth.generateEmailVerificationLink(email);
            emailService.sendEmailVerification(email, user.getDisplayName(), actionLink);
        } catch (FirebaseAuthException | EmailDeliveryException exception) {
            if (exception instanceof FirebaseAuthException firebaseException) {
                logger.warn("Firebase email verification link generation failed with code {}.",
                        firebaseException.getAuthErrorCode());
            } else {
                logger.error("Email verification delivery failed: {}.",
                        SmtpFailureDiagnostics.describe(exception));
            }
            throw new EmailDeliveryException(exception);
        }
        return RequestResult.SENT;
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible.", exception);
        }
    }

    public enum RequestResult {
        SENT,
        ALREADY_VERIFIED
    }
}
