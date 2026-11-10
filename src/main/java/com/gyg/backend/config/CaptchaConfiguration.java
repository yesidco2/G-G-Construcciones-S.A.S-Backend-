package com.gyg.backend.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestOperations;

import java.time.Duration;

@Configuration
public class CaptchaConfiguration {

    @Bean
    RestOperations captchaRestOperations(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10))
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
                if (enabled && provider.equals("turnstile") && expectedHostnames.isBlank()) {
                throw new IllegalStateException(
                    "app.captcha.expected-hostnames es obligatorio cuando Turnstile está habilitado.");
                }
                boolean production = java.util.Arrays.stream(profiles.split(","))
                    .map(String::trim)
                    .anyMatch("prod"::equalsIgnoreCase);
                boolean containsLocalHostname = java.util.Arrays.stream(expectedHostnames.split(","))
                    .map(String::trim)
                    .map(hostname -> hostname.toLowerCase(java.util.Locale.ROOT))
                    .anyMatch(hostname -> hostname.equals("localhost")
                        || hostname.equals("127.0.0.1"));
                if (production && containsLocalHostname) {
                throw new IllegalStateException(
                    "app.captcha.expected-hostnames no puede incluir localhost ni 127.0.0.1 en producción.");
                }
        };
    }
}
