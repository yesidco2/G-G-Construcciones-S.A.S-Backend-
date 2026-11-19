package com.gyg.backend.config;

import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

@Configuration
@ConditionalOnExpression("'${spring.datasource.url:}'.startsWith('jdbc:postgresql:')")
public class TechnicalSchemaConfiguration {
    @Bean
    public FlywayConfigurationCustomizer technicalFlywaySchema(
            @org.springframework.beans.factory.annotation.Value("${spring.datasource.url}") String url,
            @org.springframework.beans.factory.annotation.Value("${spring.datasource.username}") String username,
            @org.springframework.beans.factory.annotation.Value("${spring.datasource.password}") String password) {
        return configuration -> configuration.dataSource(url, username, password).defaultSchema("seguridad")
                .schemas("seguridad", "public")
                .callbacks(new Callback() {
                    @Override
                    public boolean supports(Event event, Context context) {
                        return event == Event.BEFORE_EACH_MIGRATE;
                    }

                    @Override
                    public boolean canHandleInTransaction(Event event, Context context) {
                        return true;
                    }

                    @Override
                    public void handle(Event event, Context context) {
                        try {
                            context.getConnection().setSchema("public");
                        } catch (java.sql.SQLException exception) {
                            throw new IllegalStateException("No se pudo seleccionar el esquema de negocio.", exception);
                        }
                    }

                    @Override
                    public String getCallbackName() {
                        return "business-schema";
                    }
                });
    }

    @Bean
    public FlywayMigrationStrategy relocateFlywayHistory() {
        return flyway -> {
            try (var connection = flyway.getConfiguration().getDataSource().getConnection()) {
                connection.setAutoCommit(false);
                try (var statement = connection.createStatement()) {
                    statement.execute("SELECT pg_advisory_xact_lock(20261119)");
                    statement.execute("CREATE SCHEMA IF NOT EXISTS seguridad");
                    boolean oldHistory;
                    boolean newHistory;
                    try (var result = statement.executeQuery("SELECT to_regclass('public.flyway_schema_history') IS NOT NULL, "
                            + "to_regclass('seguridad.flyway_schema_history') IS NOT NULL")) {
                        result.next();
                        oldHistory = result.getBoolean(1);
                        newHistory = result.getBoolean(2);
                    }
                    if (oldHistory && newHistory) {
                        throw new IllegalStateException("Existen dos historiales Flyway. Se requiere revisar su procedencia.");
                    }
                    if (oldHistory) {
                        statement.execute("ALTER TABLE public.flyway_schema_history SET SCHEMA seguridad");
                    }
                    connection.commit();
                } catch (Exception exception) {
                    connection.rollback();
                    throw exception;
                }
            } catch (Exception exception) {
                throw new IllegalStateException("No se pudo trasladar el historial Flyway.", exception);
            }
            flyway.migrate();
        };
    }
}
