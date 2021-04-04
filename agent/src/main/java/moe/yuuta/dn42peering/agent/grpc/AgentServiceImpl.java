package moe.yuuta.dn42peering.agent.grpc;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import moe.yuuta.dn42peering.agent.Deploy;
import moe.yuuta.dn42peering.agent.Persistent;
import moe.yuuta.dn42peering.agent.proto.DeployResult;
import moe.yuuta.dn42peering.agent.proto.NodeConfig;
import moe.yuuta.dn42peering.agent.proto.VertxAgentGrpc;

import javax.annotation.Nonnull;

class AgentServiceImpl extends VertxAgentGrpc.AgentVertxImplBase {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private final Vertx vertx;

    AgentServiceImpl(@Nonnull Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public Future<DeployResult> deploy(NodeConfig config) {
        return Deploy.deploy(vertx, config)
                .compose(_v -> Future.future(f -> {
                    Persistent.persistent(vertx, config)
                            .onComplete(res -> f.complete(_v)); // Ignore errors
                }));
    }
}
