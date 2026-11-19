package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class V12__unify_accounts_and_authorizations extends BaseJavaMigration {
    private record Authorization(long id, String email, String role, boolean active,
                                 String creator, Timestamp created) {}

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        List<Authorization> authorizations = new ArrayList<>();
        try (Statement statement = connection.createStatement()) {
            try (ResultSet duplicates = statement.executeQuery(
                    "SELECT LOWER(TRIM(email)) FROM usuarios WHERE email IS NOT NULL "
                    + "GROUP BY LOWER(TRIM(email)) HAVING COUNT(*) > 1")) {
                if (duplicates.next()) {
                    throw new IllegalStateException("Hay correos duplicados en usuarios. Revisarlos antes de unificar.");
                }
            }
            try (ResultSet rows = statement.executeQuery("SELECT * FROM administradores_autorizados ORDER BY id")) {
                while (rows.next()) {
                    authorizations.add(new Authorization(rows.getLong("id"),
                            rows.getString("email").trim().toLowerCase(java.util.Locale.ROOT),
                            rows.getString("rol"), rows.getBoolean("activo"),
                            rows.getString("creado_por"), rows.getTimestamp("creado_en")));
                }
            }
            statement.execute("ALTER TABLE usuarios ALTER COLUMN firebase_uid DROP NOT NULL");
            statement.execute("ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_rol_check");
            statement.execute("ALTER TABLE usuarios ADD CONSTRAINT cuentas_rol_check "
                    + "CHECK (rol IN ('USUARIO', 'CLIENTE', 'INGENIERO', 'ADMIN'))");
            statement.execute("ALTER TABLE usuarios ADD COLUMN autorizacion_email VARCHAR(255) UNIQUE");
            statement.execute("ALTER TABLE usuarios ADD COLUMN rol_autorizado VARCHAR(32)");
            statement.execute("ALTER TABLE usuarios ADD COLUMN autorizacion_activa BOOLEAN NOT NULL DEFAULT FALSE");
            statement.execute("ALTER TABLE usuarios ADD COLUMN autorizado_por VARCHAR(255)");
            statement.execute("ALTER TABLE usuarios ADD COLUMN autorizado_en TIMESTAMP WITHOUT TIME ZONE");
            statement.execute("ALTER TABLE usuarios ADD COLUMN autorizacion_id_anterior BIGINT UNIQUE");
            statement.execute("ALTER TABLE usuarios ADD CONSTRAINT cuentas_autorizacion_check "
                    + "CHECK ((autorizacion_email IS NULL AND rol_autorizado IS NULL AND autorizacion_activa = FALSE) "
                    + "OR (autorizacion_email IS NOT NULL AND rol_autorizado IS NOT NULL "
                    + "AND rol_autorizado IN ('ADMIN', 'INGENIERO'))) ");
        }
        for (Authorization authorization : authorizations) {
            Long userId = null;
            try (PreparedStatement find = connection.prepareStatement(
                    "SELECT id FROM usuarios WHERE LOWER(TRIM(email)) = ?")) {
                find.setString(1, authorization.email());
                try (ResultSet result = find.executeQuery()) {
                    if (result.next()) {
                        userId = result.getLong(1);
                    }
                }
            }
            if (userId == null) {
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO usuarios (email, rol, activo, creado_en) VALUES (?, 'USUARIO', TRUE, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    insert.setString(1, authorization.email());
                    insert.setTimestamp(2, authorization.created());
                    insert.executeUpdate();
                    try (ResultSet keys = insert.getGeneratedKeys()) {
                        keys.next();
                        userId = keys.getLong(1);
                    }
                }
            }
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE usuarios SET autorizacion_email = ?, rol_autorizado = ?, autorizacion_activa = ?, "
                    + "autorizado_por = ?, autorizado_en = ?, autorizacion_id_anterior = ? WHERE id = ?")) {
                update.setString(1, authorization.email());
                update.setString(2, authorization.role());
                update.setBoolean(3, authorization.active());
                update.setString(4, authorization.creator());
                update.setTimestamp(5, authorization.created());
                update.setLong(6, authorization.id());
                update.setLong(7, userId);
                update.executeUpdate();
            }
        }
        try (Statement statement = connection.createStatement()) {
            try (ResultSet count = statement.executeQuery(
                    "SELECT COUNT(*) FROM usuarios WHERE autorizacion_id_anterior IS NOT NULL")) {
                count.next();
                if (count.getLong(1) != authorizations.size()) {
                    throw new IllegalStateException("La copia de autorizaciones no coincide con el origen.");
                }
            }
            statement.execute("DROP TABLE administradores_autorizados");
            statement.execute("ALTER TABLE usuarios RENAME TO administradores_autorizados");
        }
    }
}
