package com.gyg.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("G&G Construcciones API")
                        .version("1.0.0")
                        .description("API REST para el portal de G&G Construcciones - Ingenier\u00EDa Civil y Consultor\u00EDa SST"));
    }
}
