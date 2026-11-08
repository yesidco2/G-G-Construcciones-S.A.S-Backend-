package com.gyg.backend.db.migration;

import com.gyg.backend.service.TrackingCodeService;
import db.migration.V3__secure_tracking_codes;
import org.flywaydb.core.api.migration.Context;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecureTrackingCodesMigrationTest {

    @Test
    void shouldHashLegacyCodesAndRemovePlaintextColumn() throws Exception {
        String oldPepper = System.getProperty("APP_TRACKING_CODE_PEPPER");
        String pepper = "test-pepper-for-hmac-must-be-at-least-32";
        System.setProperty("APP_TRACKING_CODE_PEPPER", pepper);
        try {
            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:tracking_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE obras (id BIGINT PRIMARY KEY, codigo_seguimiento "
                        + "VARCHAR(255) NOT NULL)");
                statement.execute("INSERT INTO obras VALUES (1, 'LEGACY-CODE-0001')");

                Context context = mock(Context.class);
                when(context.getConnection()).thenReturn(connection);
                new V3__secure_tracking_codes().migrate(context);

                try (ResultSet result = statement.executeQuery(
                        "SELECT codigo_hash FROM obras WHERE id = 1")) {
                    result.next();
                    assertEquals(TrackingCodeService.hmacSha256("LEGACY-CODE-0001", pepper),
                            result.getString(1));
                }
                assertFalse(connection.getMetaData().getColumns(null, null, "OBRAS", "CODIGO_SEGUIMIENTO")
                        .next());
            }
        } finally {
            if (oldPepper == null) {
                System.clearProperty("APP_TRACKING_CODE_PEPPER");
            } else {
                System.setProperty("APP_TRACKING_CODE_PEPPER", oldPepper);
            }
        }
    }
}
