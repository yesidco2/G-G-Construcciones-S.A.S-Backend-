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
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Bean
    InitializingBean validateCaptchaSettings(
            @Value("${app.captcha.enabled:true}") boolean enabled,
            @Value("${app.captcha.provider:turnstile}") String provider,
            @Value("${app.captcha.secret:}") String secret) {
        return () -> {
            if (!provider.equals("turnstile") && !provider.equals("recaptcha")) {
                throw new IllegalStateException("app.captcha.provider debe ser turnstile o recaptcha.");
            }
            if (enabled && secret.isBlank()) {
                throw new IllegalStateException(
                        "APP_CAPTCHA_SECRET es obligatorio cuando CAPTCHA está habilitado.");
            }
        };
    }
}
