package com.gyg.backend.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.CoreErrorCode;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

@Configuration
@ConditionalOnExpression("'${spring.datasource.url:}'.startsWith('jdbc:postgresql:')")
public class TechnicalSchemaConfiguration {

    @Bean
    public FlywayConfigurationCustomizer technicalFlywaySchema(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) {
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
                        } catch (SQLException exception) {
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
    public FlywayMigrationStrategy relocateFlywayHistory(
            @Value("${app.flyway.allow-applied-profile-v20:${APP_FLYWAY_ALLOW_APPLIED_PROFILE_V20:false}}")
            boolean allowAppliedProfileV20) {
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
                    if (oldHistory && newHistory && !hasRedundantBaseline(statement)) {
                        throw new IllegalStateException("Existen dos historiales Flyway. Se requiere revisar su procedencia.");
                    }
                    if (oldHistory && !newHistory) {
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

            if (allowAppliedProfileV20) {
                migrateWithAppliedProfileV20(flyway);
            } else {
                flyway.migrate();
            }
        };
    }

    private boolean hasRedundantBaseline(Statement statement) throws SQLException {
        try (var result = statement.executeQuery("""
                SELECT count(*) = 1 AND count(*) FILTER (
                    WHERE p.type = 'BASELINE' AND p.success AND EXISTS (
                        SELECT 1 FROM seguridad.flyway_schema_history s
                        WHERE s.type = 'BASELINE' AND s.success AND s.version = p.version
                    )
                ) = 1 FROM public.flyway_schema_history p
                """)) {
            result.next();
            return result.getBoolean(1);
        }
    }

    private void migrateWithAppliedProfileV20(Flyway flyway) {
        var strict = Flyway.configure().configuration(flyway.getConfiguration())
                .ignoreMigrationPatterns("*:pending").load();
        var validation = strict.validateWithResult();
        if (validation.validationSuccessful) {
            flyway.migrate();
            return;
        }
        var invalid = validation.invalidMigrations;
        boolean onlyProfileV20 = invalid != null && invalid.size() == 1
                && "20".equals(invalid.get(0).version)
                && invalid.get(0).errorDetails.errorCode == CoreErrorCode.APPLIED_VERSIONED_MIGRATION_NOT_RESOLVED
                && Arrays.stream(strict.info().all()).anyMatch(migration ->
                        migration.getVersion() != null && "20".equals(migration.getVersion().getVersion())
                                && "V20__perfil_y_documentos.sql".equals(migration.getScript())
                                && migration.getState() == MigrationState.MISSING_SUCCESS);
        if (!onlyProfileV20) {
            strict.validate();
            return;
        }

        Flyway.configure().configuration(flyway.getConfiguration())
                .ignoreMigrationPatterns("versioned:missing").load().migrate();
    }
}
