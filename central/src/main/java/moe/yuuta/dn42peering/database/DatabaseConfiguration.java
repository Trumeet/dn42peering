package moe.yuuta.dn42peering.database;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import javax.annotation.Nonnull;
import java.util.Locale;

@DataObject
public class DatabaseConfiguration {
    public enum MigrateAction {
        AUTO, // Automatically migrate database (default action)
        AUTO_NO_BASELINE, // Automatically migrate database,
        // but fail if the database is not empty but had not been migrated using dn42peering.
        DISABLED, // Never migrate (use with caution)
        REPAIR // Repair mode. Only use if AUTO fails. Follow the instruction in exceptions and
        // choose this mode to run. The server will exit after successful repair.
    }

    public final String host;
    public final int port;
    public final String database;
    public final String user;
    public final String password;
    public final MigrateAction migrateAction;

    public DatabaseConfiguration(@Nonnull JsonObject json) {
        this.host = json.getString("host", "localhost");
        this.port = json.getInteger("port", 3306);
        this.database = json.getString("database", "dn42peering");
        this.user = json.getString("user", "root");
        this.password = json.getString("password", "");
        this.migrateAction = MigrateAction.valueOf(json.getString("migrate", "auto").toUpperCase(Locale.ROOT));
    }

    @Nonnull
    public JsonObject toJson() {
        return new JsonObject()
                .put("host", host)
                .put("port", port)
                .put("database", database)
                .put("user", user)
                .put("password", password)
                .put("migrate", migrateAction.toString());
    }
}
