package moe.yuuta.dn42peering.agent.grpc;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import moe.yuuta.dn42peering.agent.proto.DeployResult;
import moe.yuuta.dn42peering.agent.proto.NodeConfig;
import moe.yuuta.dn42peering.agent.proto.VertxAgentGrpc;
import moe.yuuta.dn42peering.agent.provision.BGPProvisioner;
import moe.yuuta.dn42peering.agent.provision.Change;
import moe.yuuta.dn42peering.agent.provision.WireGuardProvisioner;

import javax.annotation.Nonnull;
import java.util.ArrayList;
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

        final List<Future> execFutures = new ArrayList<>(2);
        execFutures.add(bgpProvisioner.calculateChanges(config.getNode(), config.getBgpsList())
                .compose(this::chainChanges));
        execFutures.add(wireGuardProvisioner.calculateChanges(config.getNode(), config.getWgsList())
                .compose(this::chainChanges));

        // Changes in each provisioners are executed in sequence.
        // Two provisioners are executed in parallel.
        // We must wait all calculations done.
        return CompositeFuture.join(execFutures)
                .onSuccess(res -> logger.info("Deployment finished. Detailed log can be traced above."))
                .onFailure(err -> logger.error("Deployment failed. Detailed log can be traced above.", err))
                .compose(compositeFuture -> Future.succeededFuture(null));
    }
}
