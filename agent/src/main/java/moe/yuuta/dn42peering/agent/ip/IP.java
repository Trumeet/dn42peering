package moe.yuuta.dn42peering.agent.ip;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import moe.yuuta.dn42peering.agent.IOUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class IP {
    private static Logger logger = LoggerFactory.getLogger(IP.class.getSimpleName());

    public static Future<String> batch(@Nonnull Vertx vertx,
                                       @Nonnull IPOptions options,
                                       @Nonnull List<String> commands) {
        logger.info("Running batch ip commands:\n" +
                String.join("\n", commands));
        return vertx.executeBlocking(f -> {
            final List<String> cmds = new ArrayList<>();
            cmds.add("ip");
            cmds.addAll(options.toCommand());
            cmds.add("-b");
            cmds.add("/dev/stdin");
            logger.info("Executing " + cmds);
            final ProcessBuilder builder = new ProcessBuilder()
                    .command(cmds.toArray(new String[]{}));
            builder.environment().put("LANG", "C");

            try {
                final Process process = builder.start();
                final OutputStream stdin = process.getOutputStream();
                final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
                writer.write(String.join("\n", commands));
                writer.close();
                final InputStream stdout = process.getInputStream();
                final InputStream stderr = process.getErrorStream();
                final int res = process.waitFor();
                switch (res) {
                    case 0:
                        f.complete(IOUtils.read(stdout));
                        break;
                    case 1:
                        f.fail(new IPSyntaxException(IOUtils.read(stderr)));
                        break;
                    case 2:
                        f.fail(new IPKernelException(IOUtils.read(stderr)));
                        break;
                    default:
                        f.fail(new IPException(res, IOUtils.read(stderr)));
                        break;
                }
            } catch (IOException | InterruptedException e) {
                f.fail(e);
            }
        });
    }

    @Nonnull
    private static List<String> generateScript(@Nonnull String object,
                                               @Nonnull String action,
                                               @Nullable String... arguments) {
        final List<String> cmds = new ArrayList<>();
        cmds.add(object);
        cmds.add(action);
        if (arguments != null) cmds.addAll(Arrays.asList(arguments));
        return cmds;
    }

    @Nonnull
    public static Future<String> ip(@Nonnull Vertx vertx,
                                    @Nonnull IPOptions options,
                                     @Nonnull List<String> cmd) {
        return vertx.executeBlocking(f -> {
            final List<String> cmds = new ArrayList<>();
            cmds.add("ip");
            cmds.addAll(options.toCommand());
            cmds.addAll(cmd);
            logger.info("Executing '" + cmds + "'.");
            final ProcessBuilder builder = new ProcessBuilder()
                    .command(cmds.toArray(new String[]{}));
            builder.environment().put("LANG", "C");

            try {
                final Process process = builder.start();
                final InputStream stdout = process.getInputStream();
                final InputStream stderr = process.getErrorStream();
                final int res = process.waitFor();
                switch (res) {
                    case 0:
                        f.complete(IOUtils.read(stdout));
                        break;
                    case 1:
                        f.fail(new IPSyntaxException(IOUtils.read(stderr)));
                        break;
                    case 2:
                        f.fail(new IPKernelException(IOUtils.read(stderr)));
                        break;
                    default:
                        f.fail(new IPException(res, IOUtils.read(stderr)));
                        break;
                }
            } catch (IOException | InterruptedException e) {
                f.fail(e);
            }
        });
    }

    @Nonnull
    public static Future<String> ip(@Nonnull Vertx vertx,
                                     @Nonnull IPOptions options,
                                     @Nonnull String object,
                                     @Nonnull String action,
                                     @Nullable String... arguments) {
        return ip(vertx, options, generateScript(object, action, arguments));
    }

    public static class Link {
        private static final String OBJECT = "link";

        public static Future<List<moe.yuuta.dn42peering.agent.ip.Link>> handler(@Nonnull String rawJson) {
            final JsonArray arr = new JsonArray(rawJson);
            return Future.succeededFuture(
                    arr.stream()
                            .map(obj -> ((JsonObject) obj).mapTo(moe.yuuta.dn42peering.agent.ip.Link.class))
                            .collect(Collectors.toList()));
        }

        @Nonnull
        public static List<String> show(@Nullable String device) {
            return generateScript(OBJECT,
                    "show",
                    device);
        }


        @Nonnull
        public static List<String> add(@Nonnull String device,
                                       @Nonnull String type) {
            return generateScript(OBJECT,
                    "add",
                    "dev", device,
                    "type", type);
        }

        @Nonnull
        public static List<String> set(@Nonnull String device,
                                       @Nonnull String... statements) {
            final String[] arguments = new String[statements.length + 1];
            arguments[0] = device;
            System.arraycopy(statements, 0, arguments, 1, statements.length);
            return generateScript(OBJECT,
                    "set",
                    arguments);
        }

        @Nonnull
        public static List<String> del(@Nonnull String device) {
            return generateScript(OBJECT,
                    "del",
                    "dev", device);
        }
    }

    public static class Addr {
        private static final String OBJECT = "addr";

        @Nonnull
        public static Future<List<Address>> handler(@Nonnull String rawJson) {
            final JsonArray arr = new JsonArray(rawJson);
            return Future.succeededFuture(
                    arr.stream()
                            .map(obj -> ((JsonObject) obj).mapTo(Address.class))
                            .collect(Collectors.toList()));
        }

        @Nonnull
        public static List<String> show(@Nullable String device) {
            return generateScript(OBJECT,
                    "show",
                    device == null ? null : new String[] { device });
        }

        @Nonnull
        public static List<String> add(@Nonnull String ifaddr,
                                       @Nonnull String device,
                                       @Nullable String peer) {
            String[] args = new String[peer == null ? 3 : 5];
            args[0] = ifaddr;
            args[1] = "dev";
            args[2] = device;
            if (peer != null) {
                args[3] = "peer";
                args[4] = peer;
            }
            return generateScript(OBJECT,
                    "add",
                    args);
        }

        @Nonnull
        public static List<String> flush(@Nullable String device) {
            return generateScript(OBJECT,
                    "flush",
                    device);
        }
    }
}
