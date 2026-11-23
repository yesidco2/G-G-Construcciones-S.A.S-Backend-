package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V18__replace_auth_provider_identity extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        var connection = context.getConnection();
        String sessions = connection.getMetaData().getDatabaseProductName().equals("PostgreSQL")
                ? "seguridad.verified_auth_sessions" : "public.verified_auth_sessions";

        try (var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE public.administradores_autorizados RENAME COLUMN firebase_uid TO supabase_uid");
            statement.executeUpdate("UPDATE public.administradores_autorizados SET supabase_uid = NULL WHERE supabase_uid IS NOT NULL");
            statement.executeUpdate("DELETE FROM " + sessions);
            statement.execute("ALTER TABLE " + sessions + " RENAME COLUMN firebase_uid TO supabase_uid");
            statement.execute("ALTER TABLE " + sessions + " DROP COLUMN auth_time");
            statement.execute("ALTER TABLE " + sessions + " ADD COLUMN session_id VARCHAR(36) NOT NULL");
        }
    }
}
