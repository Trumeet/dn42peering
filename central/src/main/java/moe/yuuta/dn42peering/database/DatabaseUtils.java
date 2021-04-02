package moe.yuuta.dn42peering.database;

import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.PropertyKind;

import javax.annotation.Nonnull;

public final class DatabaseUtils {
    public static PropertyKind<Long> LAST_INSERTED_ID = PropertyKind.create("last-inserted-id", Long.class);

    private static final Logger logger =
            LoggerFactory.getLogger(DatabaseUtils.class.getSimpleName());

    @Nonnull
    public static Pool getPool(@Nonnull Vertx vertx) {
        final JsonObject json = vertx.getOrCreateContext().config().getJsonObject("database");
        final MySQLConnectOptions opt = new MySQLConnectOptions(json);
        return MySQLPool.pool(vertx, opt, new PoolOptions().setMaxSize(5));
    }
}
