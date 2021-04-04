package moe.yuuta.dn42peering.agent.provision;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import moe.yuuta.dn42peering.agent.proto.Node;
import moe.yuuta.dn42peering.agent.proto.WireGuardConfig;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WireGuardLegacyCleanupProvisioner implements IProvisioner<WireGuardConfig> {
    private final Vertx vertx;

    public WireGuardLegacyCleanupProvisioner(@Nonnull Vertx vertx) {
        this.vertx = vertx;
    }

    @Nonnull
    @Override
    public Future<List<Change>> calculateChanges(@Nonnull Node node, @Nonnull List<WireGuardConfig> allDesired) {
        final String[] actualNamesRaw = new File("/etc/wireguard/").list((dir, name) -> name.matches("wg_.*\\.conf"));
        final List<String> actualNames = Arrays.stream(actualNamesRaw == null ? new String[]{} : actualNamesRaw)
                .sorted()
                .collect(Collectors.toList());
        return Future.succeededFuture(actualNames.stream()
                .flatMap(string -> {
                    return Arrays.stream(new Change[]{
                            new CommandChange(new String[]{"systemctl", "disable", "--now", "-q", "wg-quick@" + string.replace(".conf", ".service")}),
                            new FileChange("/etc/wireguard/" + string, null, FileChange.Action.DELETE.toString())
                    });
                })
                .collect(Collectors.toList()));
    }
}
