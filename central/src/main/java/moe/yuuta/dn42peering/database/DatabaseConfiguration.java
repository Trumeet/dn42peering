package moe.yuuta.dn42peering.database;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import javax.annotation.Nonnull;

@DataObject
public class DatabaseConfiguration {
    public final String host;
    public final int port;
    public final String database;
    public final String user;
    public final String password;

    public DatabaseConfiguration(@Nonnull JsonObject json) {
        this.host = json.getString("host", "localhost");
        this.port = json.getInteger("port", 3306);
        this.database = json.getString("database", "dn42peering");
        this.user = json.getString("user", "root");
        this.password = json.getString("password", "");
    }

    @Nonnull
    public JsonObject toJson() {
        return new JsonObject()
                .put("host", host)
                .put("port", port)
                .put("database", database)
                .put("user", user)
                .put("password", password);
    }
}
