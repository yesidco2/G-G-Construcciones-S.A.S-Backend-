package com.gyg.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestOperations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

        assertFalse(client.verify("captcha-response", "192.0.2.1", "contacto"));
    }

    @Test
    void shouldSendSiteverifyFieldsAndAcceptExpectedActionAndHostname() {
        RestOperations transport = mock(RestOperations.class);
        when(transport.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of(
                        "success", true, "action", "contacto", "hostname", "app.example.com")));
        CaptchaProviderClient client = client(transport, "app.example.com,admin.example.com");

        assertTrue(client.verify("captcha-response", "192.0.2.1", "contacto"));

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

        assertFalse(client.verify("token-1", "192.0.2.1", "contacto"));
        assertFalse(client.verify("token-2", "192.0.2.1", "contacto"));
    }

    private static CaptchaProviderClient client(RestOperations transport, String hostnames) {
        return new CaptchaProviderClient(transport, "turnstile", "secret", hostnames);
    }
}
