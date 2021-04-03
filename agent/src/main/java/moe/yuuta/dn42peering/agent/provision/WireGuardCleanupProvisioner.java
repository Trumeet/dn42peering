package moe.yuuta.dn42peering.agent.provision;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import moe.yuuta.dn42peering.agent.ip.Address;
import moe.yuuta.dn42peering.agent.ip.IP;
import moe.yuuta.dn42peering.agent.ip.IPOptions;
import moe.yuuta.dn42peering.agent.proto.Node;
import moe.yuuta.dn42peering.agent.proto.WireGuardConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class WireGuardCleanupProvisioner implements IProvisioner<WireGuardConfig> {
    private final Vertx vertx;

    public WireGuardCleanupProvisioner(@Nonnull Vertx vertx) {
        this.vertx = vertx;
    }

    @Nullable
    private WireGuardConfig searchDesiredConfig(@Nonnull List<WireGuardConfig> configs,
                                                @Nonnull String device) {
        // TODO: Optimize algorithm
        for (final WireGuardConfig config : configs) {
            if(config.getInterface().equals(device))
                return config;
        }
        return null;
    }

    @Nonnull
    @Override
    public Future<List<Change>> calculateChanges(@Nonnull Node node, @Nonnull List<WireGuardConfig> allDesired) {
        return IP.ip(vertx, new IPOptions(), IP.Addr.show(null))
                .compose(IP.Addr::handler)
                .compose(addrs -> {
                    final List<String> ipCommands = new ArrayList<>();
                    // Detect interfaces to delete
                    for (final Address address : addrs) {
                        if(!address.getLinkType().equals("none") ||
                                !address.getIfname().matches("wg_.*")) {
                            continue;
                        }
                        if(searchDesiredConfig(allDesired, address.getIfname()) == null)
                            ipCommands.add(String.join(" ", IP.Link.del(address.getIfname())));
                    }
                    final List<Change> changes = new ArrayList<>();
                    if(!ipCommands.isEmpty()) {
                        changes.add(new IPChange(true, ipCommands));
                    }
                    return Future.succeededFuture(changes);
                });
    }
}
