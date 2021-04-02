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
import java.util.stream.Collectors;

class AgentServiceImpl extends VertxAgentGrpc.AgentVertxImplBase {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private final Vertx vertx;

    AgentServiceImpl(@Nonnull Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public Future<DeployResult> deploy(NodeConfig config) {
        logger.info("Deployment started");
        final BGPProvisioner bgpProvisioner = new BGPProvisioner(vertx);
        final WireGuardProvisioner wireGuardProvisioner = new WireGuardProvisioner(vertx);
        final List<Future> calcFutures = new ArrayList<>();
        calcFutures.add(bgpProvisioner.calculateChanges(config.getNode(), config.getBgpsList()));
        calcFutures.add(wireGuardProvisioner.calculateChanges(config.getNode(), config.getWgsList()));

        return CompositeFuture.all(calcFutures)
                .compose(compositeFuture -> {
                    final List<Future> changes = new ArrayList<>(calcFutures.size());
                    for (int i = 0; i < calcFutures.size(); i ++) {
                        final List<Change> list = compositeFuture.resultAt(i);
                        changes.addAll(list.stream().map(change -> change.execute(vertx)).collect(Collectors.toList()));
                    }
                    return CompositeFuture.all(changes);
                })
                .onComplete(res -> logger.info("Deployment finished. Detailed log can be traced above."))
                .compose(compositeFuture -> Future.succeededFuture(null));
    }
}
