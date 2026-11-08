package com.gyg.backend.config;

import com.gyg.backend.service.CaptchaProviderUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestOperations;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CaptchaProviderClientTest {

    @Test
    void shouldFailClosedWhenSiteverifyCannotBeReached() {
        RestOperations transport = mock(RestOperations.class);
        when(transport.postForObject(anyString(), any(), eq(java.util.Map.class)))
                .thenThrow(new ResourceAccessException("timeout"));
        CaptchaProviderClient client = new CaptchaProviderClient(transport, "turnstile", "secret");

        assertThrows(CaptchaProviderUnavailableException.class,
                () -> client.verify("captcha-response", "192.0.2.1"));
    }
}
