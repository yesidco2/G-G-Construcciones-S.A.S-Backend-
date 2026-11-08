package com.gyg.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityCorsConfigurationTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void shouldNotAddLocalhostOriginsOutsideDevProfile() {
        CorsConfiguration cors = securityConfig.corsConfigurationSource(
                        new MockEnvironment(), "https://frontend.example", "http://localhost:*,http://127.0.0.1:*")
                .getCorsConfiguration(new MockHttpServletRequest());

        assertTrue(cors.getAllowedOriginPatterns().contains("https://frontend.example"));
        assertFalse(cors.getAllowedOriginPatterns().contains("http://localhost:*"));
        assertFalse(cors.getAllowedOriginPatterns().contains("http://127.0.0.1:*"));
    }

    @Test
    void shouldAddLocalhostOriginsOnlyInDevProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        CorsConfiguration cors = securityConfig.corsConfigurationSource(
                        environment, "https://frontend.example", "http://localhost:*,http://127.0.0.1:*")
                .getCorsConfiguration(new MockHttpServletRequest());

        assertTrue(cors.getAllowedOriginPatterns().containsAll(
                List.of("https://frontend.example", "http://localhost:*", "http://127.0.0.1:*")));
    }

    @Test
    void shouldLoadDemoDataOnlyForDevProfile() {
        Profile profile = DataLoader.class.getAnnotation(Profile.class);

        assertTrue(profile != null && List.of(profile.value()).equals(List.of("dev")));
    }
}
