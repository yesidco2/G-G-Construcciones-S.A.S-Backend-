package com.gyg.backend.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CaptchaConfigurationTest {

    @Test
    void shouldFailStartupWhenCaptchaIsEnabledWithoutSecret() {
        assertThrows(IllegalStateException.class, () -> new CaptchaConfiguration()
                .validateCaptchaSettings(true, "turnstile", "")
                .afterPropertiesSet());
    }

    @Test
    void shouldAllowDisabledCaptchaWithoutSecret() {
        assertDoesNotThrow(() -> new CaptchaConfiguration()
                .validateCaptchaSettings(false, "turnstile", "")
                .afterPropertiesSet());
    }
}
