package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V13__separate_technical_schema extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        if (!context.getConnection().getMetaData().getDatabaseProductName().equals("PostgreSQL")) {
            return;
        }
        try (var statement = context.getConnection().createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS seguridad");
            for (String table : new String[]{"api_rate_limit_locks", "api_rate_limit_windows",
                    "captcha_session_tickets", "captcha_used_tokens", "verified_auth_sessions"}) {
                statement.execute("ALTER TABLE public." + table + " SET SCHEMA seguridad");
            }
        }
    }
}
