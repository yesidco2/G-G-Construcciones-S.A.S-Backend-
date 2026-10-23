package com.gyg.backend.config;

import com.gyg.backend.model.AdministradorAutorizado;
import com.gyg.backend.model.Rol;
import com.gyg.backend.repository.AdministradorAutorizadoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Arrays;
import java.util.Locale;

@Configuration
public class AdministradorAutorizadoSeedConfig {

    @Bean
    CommandLineRunner seedAdministradoresAutorizados(
            AdministradorAutorizadoRepository repository,
            @Value("${app.auth.admin-emails:}") String configuredEmails) {
        return args -> Arrays.stream(configuredEmails.split(","))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .map(email -> email.toLowerCase(Locale.ROOT))
                .distinct()
                .forEach(email -> {
                    if (repository.findByEmail(email).isEmpty()) {
                        AdministradorAutorizado autorizado = new AdministradorAutorizado();
                        autorizado.setEmail(email);
                        autorizado.setRol(Rol.ADMIN);
                        autorizado.setActivo(true);
                        autorizado.setCreadoPor("sistema");
                        try {
                            repository.saveAndFlush(autorizado);
                        } catch (DataIntegrityViolationException exception) {
                            if (repository.findByEmail(email).isEmpty()) {
                                throw exception;
                            }
                        }
                    }
                });
    }
}
