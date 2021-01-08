package moe.yuuta.dn42peering.agent.grpc;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;
import moe.yuuta.dn42peering.RPC;

public class RPCVerticle extends AbstractVerticle {
    private VertxServer server;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        server = VertxServerBuilder
                .forAddress(vertx, vertx.getOrCreateContext().config().getString("internal_ip"),
                        RPC.AGENT_PORT)
                .addService(new AgentServiceImpl(vertx))
                .build()
                .start(startPromise);
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        server.shutdown(stopPromise);
    }
}
