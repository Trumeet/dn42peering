package moe.yuuta.dn42peering.agent.provision;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class Change {
    @Nonnull
    public final String id;
    @Nullable
    public final String to;
    @Nonnull
    public final String action;

    public Change(@Nonnull String id,
                  @Nullable String to,
                  @Nonnull String action) {
        this.id = id;
        this.to = to;
        this.action = action;
    }

    @Nonnull
    public abstract Future<Void> execute(@Nonnull Vertx vertx);
}
