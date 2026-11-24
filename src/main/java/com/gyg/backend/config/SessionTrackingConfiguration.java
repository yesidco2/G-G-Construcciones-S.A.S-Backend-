package com.gyg.backend.config;

import jakarta.servlet.ServletContext;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class SessionTrackingConfiguration {

    @Bean
    ServletContextInitializer disableSessionTracking() {
        return (ServletContext servletContext) ->
                servletContext.setSessionTrackingModes(Collections.emptySet());
    }
}
