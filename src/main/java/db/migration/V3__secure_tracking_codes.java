package db.migration;

import com.gyg.backend.service.TrackingCodeService;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class V3__secure_tracking_codes extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        java.util.List<LegacyCode> legacyCodes = new java.util.ArrayList<>();
        try (Statement statement = context.getConnection().createStatement();
             ResultSet rows = statement.executeQuery("SELECT id, codigo_seguimiento FROM obras")) {
            while (rows.next()) {
                legacyCodes.add(new LegacyCode(rows.getLong("id"), rows.getString("codigo_seguimiento")));
            }
        }

        String pepper = System.getenv("APP_TRACKING_CODE_PEPPER");
        if (pepper == null || pepper.getBytes(StandardCharsets.UTF_8).length < 32) {
            pepper = System.getProperty("APP_TRACKING_CODE_PEPPER");
        }
        if (!legacyCodes.isEmpty()
                && (pepper == null || pepper.getBytes(StandardCharsets.UTF_8).length < 32)) {
            throw new IllegalStateException(
                    "APP_TRACKING_CODE_PEPPER debe configurarse con al menos 32 bytes para migrar los códigos.");
        }

        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("ALTER TABLE obras ADD COLUMN codigo_hash VARCHAR(64)");
            try (PreparedStatement update = context.getConnection().prepareStatement(
                    "UPDATE obras SET codigo_hash = ? WHERE id = ?")) {
                for (LegacyCode legacyCode : legacyCodes) {
                    update.setString(1,
                            TrackingCodeService.hmacSha256(legacyCode.code(), pepper == null ? "" : pepper));
                    update.setLong(2, legacyCode.id());
                    update.addBatch();
                }
                update.executeBatch();
            }
            statement.execute("ALTER TABLE obras ALTER COLUMN codigo_hash SET NOT NULL");
            statement.execute("ALTER TABLE obras DROP COLUMN codigo_seguimiento");
            statement.execute("ALTER TABLE obras ADD CONSTRAINT uk_obras_codigo_hash UNIQUE (codigo_hash)");
        }
    }

    private record LegacyCode(long id, String code) {
    }
}
