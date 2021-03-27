package moe.yuuta.dn42peering;

import io.vertx.core.*;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import moe.yuuta.dn42peering.asn.ASNVerticle;
import moe.yuuta.dn42peering.node.NodeVerticle;
import moe.yuuta.dn42peering.peer.PeerVerticle;
import moe.yuuta.dn42peering.portal.HTTPPortalVerticle;
import moe.yuuta.dn42peering.provision.ProvisionVerticle;
import moe.yuuta.dn42peering.whois.WhoisVerticle;

import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.io.InputStream;

public class Main {
    public static void main(@Nonnull String... args) throws Throwable {
        if(args.length != 1) {
            System.err.println("Usage: central <path/to/config.json>");
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
                .setInstances(Runtime.getRuntime().availableProcessors() * 2);
        Logger logger = LoggerFactory.getLogger("Main");
        CompositeFuture.all(
                Future.<String>future(f -> vertx.deployVerticle(PeerVerticle.class.getName(), options, f)),
                Future.<String>future(f -> vertx.deployVerticle(WhoisVerticle.class.getName(), options, f)),
                Future.<String>future(f -> vertx.deployVerticle(ASNVerticle.class.getName(), options, f)),
                Future.<String>future(f -> vertx.deployVerticle(NodeVerticle.class.getName(), options, f)),
                Future.<String>future(f -> vertx.deployVerticle(ProvisionVerticle.class.getName(), options, f)),
                Future.<String>future(f -> vertx.deployVerticle(HTTPPortalVerticle.class.getName(), options, f))
        ).onComplete(res -> {
            if (res.succeeded()) {
                logger.info("The server started.");
            } else {
                logger.error("Cannot deploy the server.", res.cause());
            }
        });
    }
}
