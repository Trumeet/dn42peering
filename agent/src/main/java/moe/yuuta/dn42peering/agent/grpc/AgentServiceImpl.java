package moe.yuuta.dn42peering.agent.grpc;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import moe.yuuta.dn42peering.agent.proto.DeployResult;
import moe.yuuta.dn42peering.agent.proto.NodeConfig;
import moe.yuuta.dn42peering.agent.proto.VertxAgentGrpc;
import moe.yuuta.dn42peering.agent.provision.BGPProvisioner;
import moe.yuuta.dn42peering.agent.provision.Change;
import moe.yuuta.dn42peering.agent.provision.WireGuardCleanupProvisioner;
import moe.yuuta.dn42peering.agent.provision.WireGuardProvisioner;

import javax.annotation.Nonnull;
import java.util.List;

class AgentServiceImpl extends VertxAgentGrpc.AgentVertxImplBase {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private final Vertx vertx;

    AgentServiceImpl(@Nonnull Vertx vertx) {
        this.vertx = vertx;
    }

    private Future<Void> chainChanges(@Nonnull List<Change> changes) {
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

    @Override
    public Future<DeployResult> deploy(NodeConfig config) {
        logger.info("Deployment started");
        final BGPProvisioner bgpProvisioner = new BGPProvisioner(vertx);
        final WireGuardProvisioner wireGuardProvisioner = new WireGuardProvisioner(vertx);
        final WireGuardCleanupProvisioner wireGuardCleanupProvisioner = new WireGuardCleanupProvisioner(vertx);

        // TODO: Currently all provisioning operations are non-fault-tolering. This means that
        // TODO: if one operation fails, the following will fail. This may be changed in later.
        // Changes in each provisioners are executed in sequence.
        // Two provisioners are executed in sequence.
        return wireGuardProvisioner.calculateChanges(config.getNode(), config.getWgsList())
                    .compose(this::chainChanges)
                .compose(_v -> bgpProvisioner.calculateChanges(config.getNode(), config.getBgpsList())
                    .compose(this::chainChanges))
                .compose(_v -> wireGuardCleanupProvisioner.calculateChanges(config.getNode(), config.getWgsList())
                        .compose(this::chainChanges))
                .onSuccess(res -> logger.info("Deployment finished. Detailed log can be traced above."))
                .onFailure(err -> logger.error("Deployment failed. Detailed log can be traced above.", err))
                .compose(compositeFuture -> Future.succeededFuture(null));
    }
}
