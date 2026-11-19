package com.gyg.backend.db.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import static org.junit.jupiter.api.Assertions.*;

class UnifiedAccountsMigrationTest {
    @Test
    void copiesExistingAndPendingAuthorizationsAndPreservesUserIds() throws Exception {
        String url = "jdbc:h2:mem:unifiedMigration;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        Flyway.configure().dataSource(url, "sa", "").target("11").load().migrate();
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO usuarios (firebase_uid,email,rol,activo,creado_en) "
                    + "VALUES ('existing','existing@example.com','CLIENTE',FALSE,CURRENT_TIMESTAMP)");
            statement.executeUpdate("INSERT INTO administradores_autorizados (email,rol,activo,creado_por,creado_en) "
                    + "VALUES ('existing@example.com','ADMIN',TRUE,'original',CURRENT_TIMESTAMP), "
                    + "('pending@example.com','INGENIERO',FALSE,'original',CURRENT_TIMESTAMP)");
        }
        Flyway.configure().dataSource(url, "sa", "").load().migrate();
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            try (var row = statement.executeQuery("SELECT * FROM administradores_autorizados WHERE firebase_uid='existing'")) {
                assertTrue(row.next());
                assertEquals(1L, row.getLong("id"));
                assertEquals("CLIENTE", row.getString("rol"));
                assertEquals("ADMIN", row.getString("rol_autorizado"));
                assertFalse(row.getBoolean("activo"));
                assertTrue(row.getBoolean("autorizacion_activa"));
                assertEquals("original", row.getString("autorizado_por"));
            }
            try (var row = statement.executeQuery("SELECT * FROM administradores_autorizados WHERE autorizacion_email='pending@example.com'")) {
                assertTrue(row.next());
                assertNull(row.getString("firebase_uid"));
                assertEquals("INGENIERO", row.getString("rol_autorizado"));
                assertFalse(row.getBoolean("autorizacion_activa"));
            }
            try (var row = statement.executeQuery("SELECT COUNT(*) FROM administradores_autorizados")) {
                row.next();
                assertEquals(2, row.getInt(1));
            }
        }
    }
}
