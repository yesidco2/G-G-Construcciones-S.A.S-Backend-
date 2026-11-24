package com.gyg.backend.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestOperations;

import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

@Configuration
public class CaptchaConfiguration {

    private static final Set<String> TURNSTILE_TEST_SECRETS = Set.of(
            "1x0000000000000000000000000000000AA",
            "2x0000000000000000000000000000000AA",
            "3x0000000000000000000000000000000AA");

    @Bean
    RestOperations captchaRestOperations(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Bean
    InitializingBean validateCaptchaSettings(
            @Value("${app.captcha.enabled:true}") boolean enabled,
            @Value("${app.captcha.provider:turnstile}") String provider,
            @Value("${app.captcha.secret:}") String secret,
            @Value("${app.captcha.expected-hostnames:}") String expectedHostnames,
            @Value("${spring.profiles.active:${spring.profiles.default:prod}}") String profiles) {
        return () -> {
            if (!provider.equals("turnstile") && !provider.equals("recaptcha")) {
                throw new IllegalStateException("app.captcha.provider debe ser turnstile o recaptcha.");
            }
            if (enabled && secret.isBlank()) {
                throw new IllegalStateException(
                        "APP_CAPTCHA_SECRET es obligatorio cuando CAPTCHA está habilitado.");
            }
            Set<String> hostnames = Arrays.stream(expectedHostnames.split(","))
                    .map(String::trim)
                    .filter(hostname -> !hostname.isEmpty())
                    .map(hostname -> hostname.toLowerCase(Locale.ROOT))
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
            if (enabled && provider.equals("turnstile") && hostnames.isEmpty()) {
                throw new IllegalStateException(
                        "app.captcha.expected-hostnames es obligatorio cuando Turnstile está habilitado.");
            }
            boolean production = Arrays.stream(profiles.split(","))
                    .map(String::trim)
                    .anyMatch("prod"::equalsIgnoreCase);
            boolean containsLocalHostname = hostnames.stream()
                    .anyMatch(hostname -> hostname.equals("localhost")
                            || hostname.equals("127.0.0.1"));
            if (production && containsLocalHostname) {
                throw new IllegalStateException(
                        "app.captcha.expected-hostnames no puede incluir localhost ni 127.0.0.1 en producción.");
            }
            if (enabled && production && provider.equals("turnstile")
                    && TURNSTILE_TEST_SECRETS.contains(secret.trim())) {
                throw new IllegalStateException("No se permiten claves de prueba de Turnstile en producción.");
            }
        };
    }
}
