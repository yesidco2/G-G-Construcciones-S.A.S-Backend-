package com.gyg.backend.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.gyg.backend.model.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmailVerificationServiceTest {

    @Test
    void shouldSendOfficialVerificationLinkToAuthenticatedFirebaseAccount() throws Exception {
        FirebaseAuth auth = mock(FirebaseAuth.class);
        UserRecord user = user(false, false);
        when(auth.getUser("firebase-uid")).thenReturn(user);
        when(auth.generateEmailVerificationLink("owner@example.com"))
                .thenReturn("https://example.firebaseapp.com/__/auth/action?mode=verifyEmail");
        CustomEmailService email = configuredEmail();
        RateLimitService rateLimitService = mock(RateLimitService.class);
        EmailVerificationService service = service(auth, email, rateLimitService);

        EmailVerificationService.RequestResult result =
                service.request(actor(), requestFrom("192.0.2.1"));

        assertEquals(EmailVerificationService.RequestResult.SENT, result);
        verify(auth).getUser("firebase-uid");
        verify(auth).generateEmailVerificationLink("owner@example.com");
        verify(email).sendEmailVerification(
                eq("owner@example.com"),
                eq("Account Owner"),
                eq("https://example.firebaseapp.com/__/auth/action?mode=verifyEmail"));
        verify(rateLimitService, times(2)).check(
                anyString(), any(HttpServletRequest.class), anyInt(), any(Duration.class), anyString());
    }

    @Test
    void shouldNotSendWhenFirebaseAlreadyMarksAddressVerified() throws Exception {
        FirebaseAuth auth = mock(FirebaseAuth.class);
        UserRecord user = user(true, false);
        when(auth.getUser("firebase-uid")).thenReturn(user);
        CustomEmailService email = configuredEmail();

        EmailVerificationService.RequestResult result =
                service(auth, email, mock(RateLimitService.class))
                        .request(actor(), requestFrom("192.0.2.1"));

        assertEquals(EmailVerificationService.RequestResult.ALREADY_VERIFIED, result);
        verify(auth, never()).generateEmailVerificationLink(anyString());
        verify(email, never()).sendEmailVerification(anyString(), any(), anyString());
    }

    @Test
    void shouldRejectDisabledOrInactiveAccounts() throws Exception {
        FirebaseAuth auth = mock(FirebaseAuth.class);
        UserRecord user = user(false, true);
        when(auth.getUser("firebase-uid")).thenReturn(user);
        EmailVerificationService service = service(
                auth, configuredEmail(), mock(RateLimitService.class));

        assertThrows(AccessDeniedException.class,
                () -> service.request(actor(), requestFrom("192.0.2.1")));

        Usuario inactiveActor = actor();
        inactiveActor.setActivo(false);
        assertThrows(AccessDeniedException.class,
                () -> service.request(inactiveActor, requestFrom("192.0.2.1")));
    }

    @Test
    void shouldRequireConfiguredFirebaseAndSmtpBeforeSending() {
        CustomEmailService email = mock(CustomEmailService.class);
        when(email.isConfigured()).thenReturn(false);
        EmailVerificationService service = service(
                mock(FirebaseAuth.class), email, mock(RateLimitService.class));

        assertThrows(EmailDeliveryException.class,
                () -> service.request(actor(), requestFrom("192.0.2.1")));
        verify(email).isConfigured();
        verify(email, never()).sendEmailVerification(anyString(), any(), anyString());
    }

    private static EmailVerificationService service(
            FirebaseAuth auth,
            CustomEmailService email,
            RateLimitService rateLimitService) {
        ObjectProvider<FirebaseAuth> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(auth);
        return new EmailVerificationService(provider, email, rateLimitService, 3);
    }

    private static CustomEmailService configuredEmail() {
        CustomEmailService email = mock(CustomEmailService.class);
        when(email.isConfigured()).thenReturn(true);
        return email;
    }

    private static UserRecord user(boolean verified, boolean disabled) {
        UserRecord user = mock(UserRecord.class);
        doReturn("owner@example.com").when(user).getEmail();
        doReturn("Account Owner").when(user).getDisplayName();
        doReturn(verified).when(user).isEmailVerified();
        doReturn(disabled).when(user).isDisabled();
        return user;
    }

    private static Usuario actor() {
        Usuario actor = new Usuario();
        actor.setFirebaseUid("firebase-uid");
        actor.setActivo(true);
        return actor;
    }

    private static MockHttpServletRequest requestFrom(String address) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(address);
        return request;
    }
}
