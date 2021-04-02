package moe.yuuta.dn42peering.provision;

import io.grpc.ManagedChannel;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.grpc.VertxChannelBuilder;
import moe.yuuta.dn42peering.agent.proto.NodeConfig;
import moe.yuuta.dn42peering.agent.proto.VertxAgentGrpc;
import moe.yuuta.dn42peering.jaba.Pair;
import moe.yuuta.dn42peering.node.INodeService;
import moe.yuuta.dn42peering.node.Node;
import moe.yuuta.dn42peering.peer.IPeerService;
import moe.yuuta.dn42peering.peer.Peer;

import javax.annotation.Nonnull;
import java.util.List;

class ProvisionRemoteServiceImpl implements IProvisionRemoteService {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private final Vertx vertx;
    private final INodeService nodeService;
    private final IPeerService peerService;

    ProvisionRemoteServiceImpl(@Nonnull Vertx vertx) {
        this.vertx = vertx;
        this.nodeService = INodeService.createProxy(vertx);
        this.peerService = IPeerService.createProxy(vertx);
    }

    private @Nonnull ManagedChannel toChannel(@Nonnull Node node) {
        return VertxChannelBuilder.forAddress(vertx, node.getInternalIp(), node.getInternalPort())
                .usePlaintext()
                .build();
    }

    @Nonnull
    @Override
    public IProvisionRemoteService deploy(long nodeId,
                                          @Nonnull Handler<AsyncResult<Void>> handler) {
        logger.info("Deploying to " + nodeId);
        vertx.sharedData().getLockWithTimeout("deploy_" + nodeId, 30 * 1000)
                .<Void>compose(lock -> {
                    return Future.<moe.yuuta.dn42peering.node.Node>future(f -> nodeService.getNode((int)nodeId, f))
                            .compose(node -> {
                                if (node == null) {
                                    return Future.failedFuture("Invalid node");
                                } else {
                                    return Future.succeededFuture(node);
                                }
                            })
                            .compose(node -> {
                                final NodeConfig.Builder builder = NodeConfig.newBuilder();
                                builder.setNode(node.toRPCNode().build());
                                return Future.succeededFuture(new Pair<>(node, builder));
                            })
                            .compose(pair -> {
                                final Node node = pair.a;
                                final NodeConfig.Builder builder = pair.b;
                                return Future.<List<Peer>>future(f -> peerService.listUnderNode(node.getId(), f))
                                        .compose(peers -> {
                                            peers.forEach(peer -> {
                                                builder.addBgps(peer.toBGPConfig());
                                                switch (peer.getType()) {
                                                    case WIREGUARD:
                                                        builder.addWgs(peer.toWireGuardConfig());
                                                        break;
                                                    default:
                                                        throw new IllegalArgumentException("Bug: Unsupported VPN type");
                                                }
                                            });
                                            return Future.succeededFuture(pair);
                                        });
                            })
                            .compose(pair -> {
                                final ManagedChannel channel = toChannel(pair.a);
                                final VertxAgentGrpc.AgentVertxStub stub = VertxAgentGrpc.newVertxStub(channel);
                                return stub.deploy(pair.b.build())
                                        .<Void>compose(reply -> Future.succeededFuture(null))
                                        .onComplete(res -> channel.shutdown());
                            })
                            .onComplete(_v -> {
                                lock.release();
                            });
                })
                .onFailure(err -> logger.error("Cannot deploy to " + nodeId, err))
                .onSuccess(res -> logger.info("Deploy to " + nodeId + " succeed."))
                .onComplete(handler);
        return this;
    }
}
