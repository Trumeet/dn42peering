package moe.yuuta.dn42peering.agent.grpc;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;
import moe.yuuta.dn42peering.RPC;
import moe.yuuta.dn42peering.agent.Persistent;

public class RPCVerticle extends AbstractVerticle {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private VertxServer server;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        Persistent.recover(vertx)
                .onComplete(ar -> {
                    if(ar.succeeded()) {
                        server = VertxServerBuilder
                                .forAddress(vertx, vertx.getOrCreateContext().config().getString("internal_ip"),
                                        RPC.AGENT_PORT)
                                .addService(new AgentServiceImpl(vertx))
                                .build()
                                .start(startPromise);
                    } else {
                        logger.error("Cannot recover from persistent state, aborting.", ar.cause());
                        startPromise.fail("Recover failed.");
                    }
                });
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        if(server != null)
            server.shutdown(stopPromise);
    }
}
