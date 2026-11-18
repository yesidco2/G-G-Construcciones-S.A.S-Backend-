package com.gyg.backend.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.gyg.backend.model.AdministradorAutorizado;
import com.gyg.backend.model.Rol;
import com.gyg.backend.repository.AdministradorAutorizadoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PasswordResetServiceTest {

    private static final String EMAIL = "engineer@example.com";
    private static final String ACTION_LINK = "https://example.firebaseapp.com/__/auth/action?mode=reset";

    @Test
    void shouldGenerateOfficialResetLinkAndSendCustomEmailForAuthorizedAccount() throws Exception {
        AdministradorAutorizadoRepository repository = mock(AdministradorAutorizadoRepository.class);
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        CustomEmailService emailService = configuredEmailService();
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(authorization()));
        UserRecord user = user(false);
        when(firebaseAuth.getUserByEmail(EMAIL)).thenReturn(user);
        when(firebaseAuth.generatePasswordResetLink(EMAIL)).thenReturn(ACTION_LINK);

        service(repository, firebaseAuth, emailService).request(" Engineer@Example.com ");

        verify(firebaseAuth).generatePasswordResetLink(EMAIL);
        verify(firebaseAuth).getUserByEmail(EMAIL);
        verify(emailService).sendPasswordReset(EMAIL, "Engineer", ACTION_LINK);
    }

    @Test
    void shouldNotCallFirebaseForUnauthorizedAddress() {
        AdministradorAutorizadoRepository repository = mock(AdministradorAutorizadoRepository.class);
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertDoesNotThrow(() ->
                service(repository, firebaseAuth, configuredEmailService()).request(EMAIL));
        verifyNoInteractions(firebaseAuth);
    }

    @Test
    void shouldKeepFirebaseAccountErrorsGeneric() throws Exception {
        AdministradorAutorizadoRepository repository = mock(AdministradorAutorizadoRepository.class);
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        CustomEmailService emailService = configuredEmailService();
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(authorization()));
        when(firebaseAuth.getUserByEmail(EMAIL))
                .thenThrow(mock(FirebaseAuthException.class));

        assertDoesNotThrow(() -> service(repository, firebaseAuth, emailService).request(EMAIL));
        verify(emailService).isConfigured();
        verify(emailService, never()).sendPasswordReset(anyString(), any(), anyString());
    }

    @Test
    void shouldKeepSmtpFailuresGenericToPreventAccountEnumeration() throws Exception {
        AdministradorAutorizadoRepository repository = mock(AdministradorAutorizadoRepository.class);
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        CustomEmailService emailService = configuredEmailService();
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(authorization()));
        UserRecord user = user(false);
        when(firebaseAuth.getUserByEmail(EMAIL)).thenReturn(user);
        when(firebaseAuth.generatePasswordResetLink(EMAIL)).thenReturn(ACTION_LINK);
        doThrow(new EmailDeliveryException()).when(emailService)
                .sendPasswordReset(EMAIL, null, ACTION_LINK);

        assertDoesNotThrow(() -> service(repository, firebaseAuth, emailService).request(EMAIL));
    }

    @Test
    void shouldNotSendResetLinkToDisabledFirebaseAccount() throws Exception {
        AdministradorAutorizadoRepository repository = mock(AdministradorAutorizadoRepository.class);
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        CustomEmailService emailService = configuredEmailService();
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(authorization()));
        UserRecord user = user(true);
        when(firebaseAuth.getUserByEmail(EMAIL)).thenReturn(user);

        assertDoesNotThrow(() -> service(repository, firebaseAuth, emailService).request(EMAIL));
        verify(firebaseAuth, never()).generatePasswordResetLink(anyString());
        verify(emailService, never()).sendPasswordReset(anyString(), any(), anyString());
    }

    @Test
    void shouldFailExplicitlyWhenFirebaseOrSmtpConfigurationIsMissing() {
        AdministradorAutorizadoRepository repository = mock(AdministradorAutorizadoRepository.class);
        ObjectProvider<FirebaseAuth> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);

        PasswordResetService service = new PasswordResetService(
                repository, provider, mock(CustomEmailService.class));

        assertThrows(PasswordResetUnavailableException.class, () -> service.request(EMAIL));
        verifyNoInteractions(repository);
    }

    private static PasswordResetService service(
            AdministradorAutorizadoRepository repository,
            FirebaseAuth firebaseAuth,
            CustomEmailService emailService) {
        ObjectProvider<FirebaseAuth> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(firebaseAuth);
        return new PasswordResetService(repository, provider, emailService);
    }

    private static CustomEmailService configuredEmailService() {
        CustomEmailService emailService = mock(CustomEmailService.class);
        when(emailService.isConfigured()).thenReturn(true);
        return emailService;
    }

    private static UserRecord user(boolean disabled) {
        UserRecord user = mock(UserRecord.class);
        doReturn(EMAIL).when(user).getEmail();
        doReturn("Engineer").when(user).getDisplayName();
        doReturn(disabled).when(user).isDisabled();
        return user;
    }

    private static AdministradorAutorizado authorization() {
        AdministradorAutorizado authorization = new AdministradorAutorizado();
        authorization.setEmail(EMAIL);
        authorization.setRol(Rol.ADMIN);
        authorization.setActivo(true);
        return authorization;
    }
}
