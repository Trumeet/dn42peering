package moe.yuuta.dn42peering.agent;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import moe.yuuta.dn42peering.agent.proto.DeployResult;
import moe.yuuta.dn42peering.agent.proto.NodeConfig;
import moe.yuuta.dn42peering.agent.provision.*;

import javax.annotation.Nonnull;
import java.util.List;

public class Deploy {
    private static final Logger logger = LoggerFactory.getLogger(Deploy.class.getSimpleName());
    private static Future<Void> chainChanges(@Nonnull Vertx vertx, @Nonnull List<Change> changes) {
        if(changes.isEmpty()) {
            return Future.succeededFuture();
        }
        Future<Void> last = changes.get(0).execute(vertx);
        for (int i = 1; i < changes.size(); i ++) {
            final Change current = changes.get(i);
            last = last.compose(_v -> current.execute(vertx));
        }
        return last;
    }

    public static Future<DeployResult> deploy(@Nonnull Vertx vertx, @Nonnull NodeConfig config) {
        logger.info("Deployment started");
        final BGPProvisioner bgpProvisioner = new BGPProvisioner(vertx);
        final WireGuardLegacyCleanupProvisioner wireGuardLegacyCleanupProvisioner = new WireGuardLegacyCleanupProvisioner(vertx);
        final WireGuardProvisioner wireGuardProvisioner = new WireGuardProvisioner(vertx);
        final WireGuardCleanupProvisioner wireGuardCleanupProvisioner = new WireGuardCleanupProvisioner(vertx);

        // TODO: Currently all provisioning operations are non-fault-tolering. This means that
        // TODO: if one operation fails, the following will fail. This may be changed in later.
        // Changes in each provisioners are executed in sequence.
        // Two provisioners are executed in sequence.
        return wireGuardLegacyCleanupProvisioner.calculateChanges(config.getNode(), config.getWgsList())
                    .compose(changes -> chainChanges(vertx, changes))
                .compose(_v -> wireGuardProvisioner.calculateChanges(config.getNode(), config.getWgsList())
                        .compose(changes -> chainChanges(vertx, changes)))
                .compose(_v -> bgpProvisioner.calculateChanges(config.getNode(), config.getBgpsList())
                        .compose(changes -> chainChanges(vertx, changes)))
                .compose(_v -> wireGuardCleanupProvisioner.calculateChanges(config.getNode(), config.getWgsList())
                        .compose(changes -> chainChanges(vertx, changes)))
                .onSuccess(res -> logger.info("Deployment finished. Detailed log can be traced above."))
                .onFailure(err -> logger.error("Deployment failed. Detailed log can be traced above.", err))
                .compose(compositeFuture -> Future.succeededFuture(null));
    }
}
