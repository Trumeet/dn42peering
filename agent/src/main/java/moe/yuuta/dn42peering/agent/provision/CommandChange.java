package moe.yuuta.dn42peering.agent.provision;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Arrays;

public class CommandChange extends Change {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    @Nonnull
    private final String[] commands;

    public CommandChange(@Nonnull String[] commands) {
        super(Arrays.toString(commands), null, "exec");
        this.commands = commands;
    }

    @Nonnull
    @Override
    public Future<Void> execute(@Nonnull Vertx vertx) {
        logger.info("Executing " + Arrays.toString(commands));
        return AsyncShell.execSucc(vertx, commands);
    }
}
