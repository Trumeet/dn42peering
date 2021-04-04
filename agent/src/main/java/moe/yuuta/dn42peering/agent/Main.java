package moe.yuuta.dn42peering.agent;

import io.vertx.core.*;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import moe.yuuta.dn42peering.agent.grpc.RPCVerticle;

import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;

public class Main {
    public static void main(@Nonnull String... args) throws Throwable {
        if(args.length != 1) {
            System.err.println("Usage: agent <path/to/config.json>");
            System.exit(64);
            return;
        }

        System.setProperty("vertx.logger-delegate-factory-class-name",
                "io.vertx.core.logging.JULLogDelegateFactory");

        final InputStream in = new FileInputStream(args[0]);
        final JsonObject config = new JsonObject(IOUtils.read(in));
        in.close();

        final Vertx vertx = Vertx.vertx(new VertxOptions());
        final DeploymentOptions options = new DeploymentOptions()
                .setConfig(config)
                .setInstances(1);
        Logger logger = LoggerFactory.getLogger("Main");
        CompositeFuture.all(Arrays.asList(
                Future.<String>future(f -> vertx.deployVerticle(RPCVerticle.class.getName(), options, f))
        )).onComplete(res -> {
            if (res.succeeded()) {
                logger.info("The server started.");
            } else {
                logger.error("Cannot deploy the server.", res.cause());
            }
        });
    }
}
