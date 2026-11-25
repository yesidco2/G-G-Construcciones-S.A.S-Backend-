package com.gyg.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestOperations;

import java.time.Duration;

@Configuration
public class GeminiChatConfiguration {

    @Bean("geminiRestOperations")
    @ConditionalOnProperty(name = "app.chat.gemini.enabled", havingValue = "true")
    RestOperations geminiRestOperations(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(50))
                .build();
    }
}
