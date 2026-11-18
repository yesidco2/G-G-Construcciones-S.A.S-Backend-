package com.gyg.backend.service;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.gyg.backend.model.Rol;
import com.gyg.backend.repository.AdministradorAutorizadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    private final AdministradorAutorizadoRepository authorizationRepository;
    private final ObjectProvider<FirebaseAuth> firebaseAuthProvider;
    private final CustomEmailService emailService;

    public PasswordResetService(
            AdministradorAutorizadoRepository authorizationRepository,
            ObjectProvider<FirebaseAuth> firebaseAuthProvider,
            CustomEmailService emailService) {
        this.authorizationRepository = authorizationRepository;
        this.firebaseAuthProvider = firebaseAuthProvider;
        this.emailService = emailService;
    }

    public void request(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        FirebaseAuth firebaseAuth = firebaseAuthProvider.getIfAvailable();
        if (firebaseAuth == null) {
            logger.error("Password reset request skipped because Firebase Auth is unavailable.");
            return;
        }
        if (!emailService.isConfigured()) {
            logger.error("Password reset request skipped because SMTP is not configured.");
            return;
        }

        boolean authorized = authorizationRepository.findByEmail(normalizedEmail)
                .filter(authorization -> authorization.isActivo()
                        && authorization.getRol() != Rol.USUARIO)
                .isPresent();
        if (!authorized) {
            return;
        }

        try {
            UserRecord user = firebaseAuth.getUserByEmail(normalizedEmail);
            if (user.isDisabled() || user.getEmail() == null || user.getEmail().isBlank()) {
                return;
            }
            String actionLink = firebaseAuth.generatePasswordResetLink(user.getEmail());
            emailService.sendPasswordReset(user.getEmail(), user.getDisplayName(), actionLink);
        } catch (FirebaseAuthException exception) {
            AuthErrorCode errorCode = exception.getAuthErrorCode();
            if (errorCode != AuthErrorCode.EMAIL_NOT_FOUND
                    && errorCode != AuthErrorCode.USER_NOT_FOUND
                    && errorCode != AuthErrorCode.USER_DISABLED) {
                logger.warn("Firebase password reset link generation failed with code {}.", errorCode);
            }
        } catch (EmailDeliveryException exception) {
            logger.error("Password reset email delivery failed: {}.",
                    SmtpFailureDiagnostics.describe(exception));
        }
    }
}
