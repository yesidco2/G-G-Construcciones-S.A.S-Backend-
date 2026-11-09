package com.gyg.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyg.backend.model.Rol;
import com.gyg.backend.repository.AdministradorAutorizadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Map;

@Service
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    private static final String FIREBASE_PASSWORD_RESET_URL =
            "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode";

    private final AdministradorAutorizadoRepository authorizationRepository;
    private final RestOperations restOperations;
    private final ObjectMapper objectMapper;
    private final String firebaseWebApiKey;

    public PasswordResetService(
            AdministradorAutorizadoRepository authorizationRepository,
            RestOperations restOperations,
            ObjectMapper objectMapper,
            @Value("${app.firebase.web-api-key:}") String firebaseWebApiKey) {
        this.authorizationRepository = authorizationRepository;
        this.restOperations = restOperations;
        this.objectMapper = objectMapper;
        this.firebaseWebApiKey = firebaseWebApiKey;
    }

    public void request(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        boolean authorized = authorizationRepository.findByEmail(normalizedEmail)
                .filter(authorization -> authorization.isActivo()
                        && authorization.getRol() != Rol.USUARIO)
                .isPresent();
        if (!authorized) {
            return;
        }
        if (firebaseWebApiKey.isBlank()) {
            logger.error("Firebase password reset is unavailable: FIREBASE_WEB_API_KEY is not configured.");
            throw new PasswordResetUnavailableException();
        }

        URI uri = UriComponentsBuilder.fromUriString(FIREBASE_PASSWORD_RESET_URL)
                .queryParam("key", firebaseWebApiKey)
                .build()
                .encode()
                .toUri();
        try {
            ResponseEntity<String> response = restOperations.postForEntity(uri, Map.of(
                    "requestType", "PASSWORD_RESET",
                    "email", normalizedEmail), String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new PasswordResetUnavailableException();
            }
        } catch (HttpClientErrorException exception) {
            if (isNonActionableAccountError(exception)) {
                return;
            }
            logger.warn("Firebase password reset failed with HTTP status {} and error code {}.",
                    exception.getStatusCode().value(), firebaseErrorCode(exception));
            throw new PasswordResetUnavailableException();
        } catch (RestClientException exception) {
            logger.warn("Firebase password reset request failed due to {}.", exception.getClass().getSimpleName());
            throw new PasswordResetUnavailableException();
        }
    }

    private boolean isNonActionableAccountError(HttpClientErrorException exception) {
        String code = firebaseErrorCode(exception);
        return code.startsWith("EMAIL_NOT_FOUND")
                || code.startsWith("USER_DISABLED");
    }

    private String firebaseErrorCode(HttpClientErrorException exception) {
        if (exception.getStatusCode() != HttpStatus.BAD_REQUEST) {
            return "HTTP_" + exception.getStatusCode().value();
        }
        try {
            JsonNode error = objectMapper.readTree(exception.getResponseBodyAsByteArray()).path("error");
            String code = error.path("message").asText();
            return code.isBlank() ? "UNKNOWN" : code;
        } catch (IOException exceptionDuringParsing) {
            return "UNPARSEABLE_RESPONSE";
        }
    }
}
