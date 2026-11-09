package com.gyg.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyg.backend.model.AdministradorAutorizado;
import com.gyg.backend.model.Rol;
import com.gyg.backend.repository.AdministradorAutorizadoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestOperations;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetServiceTest {

    private static final String EMAIL = "engineer@example.com";

    @Test
    void shouldSendFirebaseResetEmailToAuthorizedAccount() {
        AdministradorAutorizadoRepository repository = mock(AdministradorAutorizadoRepository.class);
        RestOperations restOperations = mock(RestOperations.class);
        AdministradorAutorizado authorization = authorization();
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(authorization));
        when(restOperations.postForEntity(any(java.net.URI.class), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"email\":\"" + EMAIL + "\"}"));

        service(repository, restOperations, "public-api-key").request(" Engineer@Example.com ");

        verify(restOperations).postForEntity(any(java.net.URI.class), eq(Map.of(
                "requestType", "PASSWORD_RESET",
                "email", EMAIL)), eq(String.class));
    }

    @Test
    void shouldNotCallFirebaseForUnauthorizedAddress() {
        AdministradorAutorizadoRepository repository = mock(AdministradorAutorizadoRepository.class);
        RestOperations restOperations = mock(RestOperations.class);
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service(repository, restOperations, "public-api-key").request(EMAIL));
        verify(restOperations, never()).postForEntity(any(java.net.URI.class), any(), eq(String.class));
    }

    @Test
    void shouldNotRevealWhenFirebaseHasNoMatchingAccount() {
        AdministradorAutorizadoRepository repository = mock(AdministradorAutorizadoRepository.class);
        RestOperations restOperations = mock(RestOperations.class);
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(authorization()));
        when(restOperations.postForEntity(any(java.net.URI.class), any(), eq(String.class)))
                .thenThrow(firebaseError("EMAIL_NOT_FOUND"));

        assertDoesNotThrow(() -> service(repository, restOperations, "public-api-key").request(EMAIL));
    }

    @Test
    void shouldFailExplicitlyWhenFirebaseConfigurationIsMissing() {
        AdministradorAutorizadoRepository repository = mock(AdministradorAutorizadoRepository.class);
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(authorization()));

        assertThrows(PasswordResetUnavailableException.class,
                () -> service(repository, mock(RestOperations.class), "").request(EMAIL));
    }

    @Test
    void shouldFailClosedWhenFirebaseCannotSendTheResetEmail() {
        AdministradorAutorizadoRepository repository = mock(AdministradorAutorizadoRepository.class);
        RestOperations restOperations = mock(RestOperations.class);
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(authorization()));
        when(restOperations.postForEntity(any(java.net.URI.class), any(), eq(String.class)))
                .thenThrow(new ResourceAccessException("Firebase unavailable"));

        assertThrows(PasswordResetUnavailableException.class,
                () -> service(repository, restOperations, "public-api-key").request(EMAIL));
    }

    private static PasswordResetService service(
            AdministradorAutorizadoRepository repository,
            RestOperations restOperations,
            String apiKey) {
        return new PasswordResetService(repository, restOperations, new ObjectMapper(), apiKey);
    }

    private static AdministradorAutorizado authorization() {
        AdministradorAutorizado authorization = new AdministradorAutorizado();
        authorization.setEmail(EMAIL);
        authorization.setRol(Rol.ADMIN);
        authorization.setActivo(true);
        return authorization;
    }

    private static HttpClientErrorException firebaseError(String errorCode) {
        byte[] response = ("{\"error\":{\"message\":\"" + errorCode + "\"}}")
                .getBytes(StandardCharsets.UTF_8);
        return HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY, response, StandardCharsets.UTF_8);
    }
}
