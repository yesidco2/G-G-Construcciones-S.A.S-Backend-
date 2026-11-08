package com.gyg.backend.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
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
            Environment environment,
            @Value("${app.captcha.enabled:true}") boolean enabled,
            @Value("${app.captcha.provider:turnstile}") String provider,
            @Value("${app.captcha.secret:}") String secret) {
        return () -> {
            if (!provider.equals("turnstile") && !provider.equals("recaptcha")) {
                throw new IllegalStateException("app.captcha.provider debe ser turnstile o recaptcha.");
            }
            boolean production = environment.acceptsProfiles(Profiles.of("prod"))
                    || environment.getActiveProfiles().length == 0;
            if (enabled && production && secret.isBlank()) {
                throw new IllegalStateException(
                        "app.captcha.secret es obligatorio cuando CAPTCHA está habilitado en producción.");
            }
        };
    }
}
