package moe.yuuta.dn42peering.agent.provision;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import moe.yuuta.dn42peering.agent.IOUtils;

import javax.annotation.Nonnull;
import java.io.*;

public class WireGuardSyncConfChange extends Change {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private final String device;
    private final String config;
    public WireGuardSyncConfChange(@Nonnull String device,
                                   @Nonnull String config) {
        super(device, config, "syncconf");
        this.device = device;
        this.config = config;
    }

    @Nonnull
    @Override
    public Future<Void> execute(@Nonnull Vertx vertx) {
        logger.info("Syncing config with " + device);
        return vertx.executeBlocking(f -> {
            try {
                final Process process = new ProcessBuilder()
                        .command("wg", "syncconf", device, "/dev/stdin")
                        .start();
                final OutputStream stdin = process.getOutputStream();
                final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
                writer.write(config);
                writer.close();
                final InputStream stderr = process.getErrorStream();
                final int res = process.waitFor();
                if(res != 0) {
                    f.fail("Unexpected 'wg syncconf " + device + " /dev/stdin' response: " + res + ".\n" +
                            "stderr = \n" +
                            IOUtils.read(stderr));
                    return;
                }
                f.complete();
            } catch (IOException | InterruptedException e) {
                f.fail(e);
            }
        });
    }
}
