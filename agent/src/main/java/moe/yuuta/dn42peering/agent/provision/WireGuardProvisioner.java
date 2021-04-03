package moe.yuuta.dn42peering.agent.provision;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystemException;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
import moe.yuuta.dn42peering.agent.proto.Node;
import moe.yuuta.dn42peering.agent.proto.WireGuardConfig;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.Inet6Address;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.stream.Collectors;

public class WireGuardProvisioner implements IProvisioner<WireGuardConfig> {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private final TemplateEngine engine;
    private final Vertx vertx;

    public WireGuardProvisioner(@Nonnull Vertx vertx) {
        this(FreeMarkerTemplateEngine.create(vertx, "ftlh"), vertx);
    }

    public WireGuardProvisioner(@Nonnull TemplateEngine engine,
                          @Nonnull Vertx vertx) {
        this.engine = engine;
        this.vertx = vertx;
    }

    @Nonnull
    private Future<List<Change>> calculateDeleteChanges(@Nonnull List<WireGuardConfig> allDesired) {
        final String[] actualNamesRaw = new File("/etc/wireguard/").list((dir, name) -> name.matches("wg_.*\\.conf"));
        final List<String> actualNames = Arrays.stream(actualNamesRaw == null ? new String[]{} : actualNamesRaw)
                .sorted()
                .collect(Collectors.toList());
        final String[] desiredNames = allDesired
                .stream()
                .map(desired -> generateWGPath(desired.getInterface()))
                .sorted()
                .collect(Collectors.toList())
                .toArray(new String[]{});
        final List<Integer> toRemove = new ArrayList<>(actualNames.size());
        for (int i = 0; i < desiredNames.length; i ++) {
            toRemove.clear();
            for(int j = 0; j < actualNames.size(); j ++) {
                if(("/etc/wireguard/" + actualNames.get(j)).equals(desiredNames[i])) {
                    toRemove.add(j);
                }
            }
            for (int j = 0; j < toRemove.size(); j ++) {
                actualNames.remove(toRemove.get(j).intValue());
            }
        }
        return Future.succeededFuture(actualNames.stream()
                .flatMap(string -> {
                    return Arrays.stream(new Change[] {
                            new CommandChange(new String[]{ "systemctl", "disable", "--now", "-q", "wg-quick@" + string }),
                            new FileChange("/etc/wireguard/" + string, null, FileChange.Action.DELETE.toString())
                    });
                })
                .collect(Collectors.toList()));
    }

    @Nonnull
    private static String generateWGPath(@Nonnull String iif) {
        return String.format("/etc/wireguard/%s.conf", iif);
    }

    @Nonnull
    private Future<Buffer> readConfig(@Nonnull String iif) {
        return Future.future(f -> {
            vertx.fileSystem()
                    .readFile(generateWGPath(iif))
                    .onFailure(err -> {
                        if(err instanceof FileSystemException &&
                                err.getCause() instanceof NoSuchFileException) {
                            f.complete(null);
                        } else {
                            f.fail(err);
                        }
                    })
                    .onSuccess(f::complete);
        });
    }

    @Nonnull
    private Future<Buffer> renderConfig(@Nonnull Node node, @Nonnull WireGuardConfig config) {
        final Map<String, Object> params = new HashMap<>(9);
        params.put("listen_port", config.getListenPort());
        params.put("self_priv_key", config.getSelfPrivKey());
        params.put("dev", config.getInterface());
        params.put("self_ipv4", node.getIpv4());
        params.put("peer_ipv4", config.getPeerIPv4());
        if (!config.getPeerIPv6().equals("")) {
            params.put("peer_ipv6", config.getPeerIPv6());
            try {
                final boolean ll = Inet6Address.getByName(config.getPeerIPv6()).isLinkLocalAddress();
                params.put("peer_ipv6_ll", ll);
                if(ll)
                    params.put("self_ipv6", node.getIpv6());
                else
                    params.put("self_ipv6", node.getIpv6NonLL());
            } catch (IOException e) {
                return Future.failedFuture(e);
            }
        }
        params.put("preshared_key", config.getSelfPresharedSecret());
        if(!config.getEndpoint().equals("")) {
            params.put("endpoint", config.getEndpoint());
        }
        params.put("peer_pub_key", config.getPeerPubKey());

        return engine.render(params, "wg.conf.ftlh");
    }

    @Nonnull
    private Future<List<Change>> calculateSingleConfigChange(@Nonnull Node node,
                                                             @Nonnull WireGuardConfig desiredConfig) {
        return CompositeFuture.all(readConfig(desiredConfig.getInterface()), renderConfig(node, desiredConfig))
                .compose(future -> {
                    final Buffer actualBuff = future.resultAt(0);
                    final String actual = actualBuff == null ? null : actualBuff.toString();
                    final String desired = future.resultAt(1).toString();
                    final List<Change> changes = new ArrayList<>(1);
                    if(actual == null) {
                        changes.add(new FileChange(generateWGPath(desiredConfig.getInterface()),
                                desired,
                                FileChange.Action.CREATE_AND_WRITE.toString()));
                        changes.add(
                                new CommandChange(new String[] { "systemctl",
                                        "enable",
                                        "--now",
                                        "-q",
                                        "wg-quick@" + desiredConfig.getInterface() }));
                    } else if(!actual.equals(desired)) {
                        // TODO: Smart reloading / restarting
                        changes.add(new FileChange(generateWGPath(desiredConfig.getInterface()),
                                desired,
                                FileChange.Action.OVERWRITE.toString()));
                        changes.add(
                                new CommandChange(new String[] { "systemctl",
                                        "restart",
                                        "-q",
                                        "wg-quick@" + desiredConfig.getInterface() }));
                    }
                    return Future.succeededFuture(changes);
                });
    }

    @Nonnull
    private Future<List<Change>> calculateSingleServiceStatusChange(@Nonnull WireGuardConfig desiredConfig) {
        // Check if the service is not started or in wrong state.
        return AsyncShell.exec(vertx, "systemctl", "is-active", "wg-quick@" + desiredConfig.getInterface())
                .compose(res -> {
                    if(res == 0) {
                        return Future.succeededFuture(Collections.emptyList());
                    }
                    return Future.succeededFuture(Collections.singletonList(
                            new CommandChange(new String[] { "systemctl",
                                    "enable",
                                    "--now",
                                    "-q",
                                    "wg-quick@" + desiredConfig.getInterface() })
                    ));
                });
    }

    @Nonnull
    @Override
    public Future<List<Change>> calculateChanges(@Nonnull Node node, @Nonnull List<WireGuardConfig> allDesired) {
        final List<Future<List<Change>>> addOrModifyChanges =
                allDesired.stream().map(desired -> calculateSingleConfigChange(node, desired)).collect(Collectors.toList());
        final List<Future<List<Change>>> serviceChanges =
                allDesired.stream().map(this::calculateSingleServiceStatusChange).collect(Collectors.toList());
        final Future<List<Change>> deleteChanges =
                calculateDeleteChanges(allDesired);
        final List<Future> futures = new ArrayList<>(addOrModifyChanges.size() + 1);
        futures.addAll(addOrModifyChanges);
        futures.addAll(serviceChanges);
        futures.add(deleteChanges);
        return CompositeFuture.all(futures)
                .compose(compositeFuture -> {
                    final List<Change> changes = new ArrayList<>(futures.size());
                    for(int i = 0; i < futures.size(); i ++) {
                        changes.addAll(compositeFuture.resultAt(i));
                    }
                    return Future.succeededFuture(changes);
                });
    }
}
