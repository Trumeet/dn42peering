package moe.yuuta.dn42peering.agent.provision;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import moe.yuuta.dn42peering.agent.ip.IP;
import moe.yuuta.dn42peering.agent.ip.IPOptions;

import javax.annotation.Nonnull;
import java.util.List;

public class IPChange extends Change {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private final boolean force;
    private final List<String> commands;

    public IPChange(boolean force, @Nonnull List<String> commands) {
        super("ip", null, "exec");
        this.force = force;
        this.commands = commands;
    }

    @Nonnull
    @Override
    public Future<Void> execute(@Nonnull Vertx vertx) {
        return IP.batch(vertx, new IPOptions().setForce(force), commands)
                .compose(stdout -> Future.succeededFuture());
    }
}
