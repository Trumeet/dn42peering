package moe.yuuta.dn42peering.agent;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import moe.yuuta.dn42peering.agent.proto.DeployResult;
import moe.yuuta.dn42peering.agent.proto.NodeConfig;

import javax.annotation.Nonnull;
import java.io.*;

public class Persistent {
    private static final Logger logger = LoggerFactory.getLogger(Persistent.class.getSimpleName());

    public static boolean enabled(@Nonnull Vertx vertx) {
        return vertx.getOrCreateContext().config().getBoolean("persistent", false);
    }

    @Nonnull
    public static String getPath(@Nonnull Vertx vertx) {
        return vertx.getOrCreateContext().config().getString("persistent_path",
                "/var/lib/dn42peering/agent/config");
    }

    public static Future<DeployResult> recover(@Nonnull Vertx vertx) {
        if(!enabled(vertx)) {
            logger.info("Persistent disabled.");
            return Future.succeededFuture(null);
        }
        if(!new File(getPath(vertx)).exists()) {
            logger.info("Persistent file is not found.");
            return Future.succeededFuture(null);
        }
        return vertx.<NodeConfig>executeBlocking(f -> {
            logger.info("Recovering from persistent state...");
            try(final InputStream in = new FileInputStream(getPath(vertx))) {
                final NodeConfig config = NodeConfig.parseDelimitedFrom(in);
                f.complete(config);
            } catch (IOException e) {
                f.fail(e);
            }
        }).compose(config -> Deploy.deploy(vertx, config))
                .onSuccess(res ->
                        logger.info("Recovered from persistent state."));
    }

    @Nonnull
    public static Future<Void> persistent(@Nonnull Vertx vertx, @Nonnull NodeConfig config) {
        if (!enabled(vertx)) return Future.succeededFuture();
        return vertx.fileSystem()
                .open(getPath(vertx),
                        new OpenOptions()
                                .setWrite(true)
                                .setCreate(true))
                .<AsyncFile>compose(file -> {
                    return vertx.executeBlocking(f -> {
                        try {
                            final ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            config.writeDelimitedTo(stream);
                            file.write(Buffer.buffer(stream.toByteArray()));
                            stream.close();
                            f.complete(file);
                        } catch (IOException e) {
                            f.fail(e);
                        }
                    });
                })
                .compose(AsyncFile::close)
                .compose(file ->
                        vertx.fileSystem().chmod(getPath(vertx), "rw-------"))
                .onFailure(err -> logger.error("Cannot persistent node configuration", err));
    }
}
