package com.gyg.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import com.gyg.backend.service.CaptchaFailureCode;
import com.gyg.backend.service.CaptchaVerificationResult;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestOperations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CaptchaProviderClientTest {

    @Test
    void shouldFailClosedWhenSiteverifyCannotBeReached() {
        RestOperations transport = mock(RestOperations.class);
        when(transport.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenThrow(new ResourceAccessException("timeout"));
        CaptchaProviderClient client = client(transport, "app.example.com");

        assertEquals(
                CaptchaFailureCode.PROVIDER_TIMEOUT,
                client.verify("captcha-response", "192.0.2.1", "contacto").failureCode());
    }

    @Test
    void shouldSendSiteverifyFieldsAndAcceptExpectedActionAndHostname() {
        RestOperations transport = mock(RestOperations.class);
        when(transport.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of(
                        "success", true, "action", "contacto", "hostname", "app.example.com")));
        CaptchaProviderClient client = client(transport, "app.example.com,admin.example.com");

        assertTrue(client.verify("captcha-response", "192.0.2.1", "contacto").successful());

        var entityCaptor = forClass(org.springframework.http.HttpEntity.class);
        verify(transport).exchange(
                eq("https://challenges.cloudflare.com/turnstile/v0/siteverify"),
                eq(HttpMethod.POST), entityCaptor.capture(), eq(Map.class));
        MultiValueMap<String, String> form = (MultiValueMap<String, String>) entityCaptor.getValue().getBody();
        assertEquals("secret", form.getFirst("secret"));
        assertEquals("captcha-response", form.getFirst("response"));
        assertEquals("192.0.2.1", form.getFirst("remoteip"));
    }

    @Test
    void shouldRejectWrongActionAndUntrustedHostname() {
        RestOperations transport = mock(RestOperations.class);
        CaptchaProviderClient client = client(transport, "app.example.com");
        when(transport.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of(
                        "success", true, "action", "newsletter", "hostname", "app.example.com")))
                .thenReturn(ResponseEntity.ok(Map.of(
                        "success", true, "action", "contacto", "hostname", "evil.example.com")));

        assertEquals(
                CaptchaFailureCode.ACTION_MISMATCH,
                client.verify("token-1", "192.0.2.1", "contacto").failureCode());
        assertEquals(
                CaptchaFailureCode.HOSTNAME_MISMATCH,
                client.verify("token-2", "192.0.2.1", "contacto").failureCode());
    }

    @Test
    void shouldClassifyCloudflareErrorCodesWithoutExposingProviderDetails() {
        RestOperations transport = mock(RestOperations.class);
        CaptchaProviderClient client = client(transport, "app.example.com");
        when(transport.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of(
                        "success", false, "error-codes", java.util.List.of("timeout-or-duplicate"))))
                .thenReturn(ResponseEntity.ok(Map.of(
                        "success", false, "error-codes", java.util.List.of("invalid-input-secret"))))
                .thenReturn(ResponseEntity.ok(Map.of(
                        "success", false, "error-codes", java.util.List.of("internal-error"))));

        assertEquals(CaptchaFailureCode.TOKEN_EXPIRED_OR_DUPLICATE,
                client.verify("expired", "192.0.2.1", "contacto").failureCode());
        assertEquals(CaptchaFailureCode.CONFIGURATION_ERROR,
                client.verify("token", "192.0.2.1", "contacto").failureCode());
        assertEquals(CaptchaFailureCode.PROVIDER_UNAVAILABLE,
                client.verify("token", "192.0.2.1", "contacto").failureCode());
    }

    @Test
    void shouldRejectMissingActionAndHostnameEvenWhenProviderReportsSuccess() {
        RestOperations transport = mock(RestOperations.class);
        when(transport.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("success", true)));

        CaptchaVerificationResult result = client(transport, "app.example.com")
                .verify("token", "192.0.2.1", "contacto");

        assertFalse(result.successful());
        assertEquals(CaptchaFailureCode.ACTION_MISMATCH, result.failureCode());
    }

    private static CaptchaProviderClient client(RestOperations transport, String hostnames) {
        return new CaptchaProviderClient(transport, "turnstile", "secret", hostnames);
    }
}
