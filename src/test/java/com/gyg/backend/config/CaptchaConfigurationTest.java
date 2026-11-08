package com.gyg.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CaptchaConfigurationTest {

    @Test
    void shouldFailStartupForEnabledProductionCaptchaWithoutSecret() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThrows(IllegalStateException.class, () -> new CaptchaConfiguration()
                .validateCaptchaSettings(environment, true, "turnstile", "")
                .afterPropertiesSet());
    }
}
