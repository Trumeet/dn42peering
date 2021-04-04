package moe.yuuta.dn42peering.agent.provision;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
import moe.yuuta.dn42peering.agent.ip.AddrInfoItem;
import moe.yuuta.dn42peering.agent.ip.Address;
import moe.yuuta.dn42peering.agent.ip.IP;
import moe.yuuta.dn42peering.agent.ip.IPOptions;
import moe.yuuta.dn42peering.agent.proto.Node;
import moe.yuuta.dn42peering.agent.proto.WireGuardConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.Inet6Address;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private Future<Buffer> renderConfig(@Nonnull WireGuardConfig config) {
        final Map<String, Object> params = new HashMap<>(5);
        params.put("listen_port", config.getListenPort());
        params.put("self_priv_key", config.getSelfPrivKey());
        params.put("preshared_key", config.getSelfPresharedSecret());
        if (!config.getEndpoint().equals("")) {
            params.put("endpoint", config.getEndpoint());
        }
        params.put("peer_pub_key", config.getPeerPubKey());

        return engine.render(params, "wg.conf.ftlh");
    }

    @Nullable
    private Address searchActualAddress(@Nonnull List<Address> addresses,
                                        @Nonnull String device) {
        // TODO: Optimize algorithm
        for (final Address address : addresses) {
            if(address.getIfname().equals(device))
                return address;
        }
        return null;
    }

    @Nonnull
    private List<String> calculateSingleNetlinkChanges(@Nonnull Node node,
                                                       @Nonnull WireGuardConfig desired,
                                                       @Nullable Address actual) throws IOException {
        final boolean linkLocal =
                !desired.getPeerIPv6().isEmpty() &&
                        Inet6Address.getByName(desired.getPeerIPv6()).isLinkLocalAddress();

        final boolean desireIP6 = !desired.getPeerIPv6().isEmpty();
        final boolean needCreateInterface = actual == null;
        final boolean needCreateAddrs;
        final boolean needUp;

        if(actual == null) {
            needCreateAddrs = true;
            needUp = true;
        } else {
            needUp = !actual.getOperstate().equals("UP") &&
            !actual.getOperstate().equals("UNKNOWN");
            AddrInfoItem actualIP4 = null;
            AddrInfoItem actualIP6 = null;
            boolean excessiveIPs = false;
            for (final AddrInfoItem item : actual.getAddrInfo()) {
                switch (item.getFamily()) {
                    case "inet":
                        if(actualIP4 != null) {
                            excessiveIPs = true;
                            break;
                        } else {
                            actualIP4 = item;
                        }
                        break;
                    case "inet6":
                        if(actualIP6 != null) {
                            excessiveIPs = true;
                            break;
                        } else {
                            actualIP6 = item;
                        }
                        break;
                    default:
                        excessiveIPs = true;
                        break;
                }
            }
            if(excessiveIPs || actualIP4 == null || (desireIP6 && actualIP6 == null) ||
                    (!desireIP6 && actualIP6 != null)) {
                logger.info("Recreating addresses for " + desired.getId() + " since there are extra addresses or necessary addresses cannot be found.");
                needCreateAddrs = true;
            } else {
                boolean needCreateIP4Addr =
                        actualIP4.getPrefixlen() != 32 ||
                                !node.getIpv4().equals(actualIP4.getLocal()) ||
                                !desired.getPeerIPv4().equals(actualIP4.getAddress());
                boolean needCreateIP6Addr = false;
                if(desireIP6) {
                    needCreateIP6Addr =
                            actualIP6.getPrefixlen() != (linkLocal ? 64 : 128) ||
                                    !(linkLocal ? node.getIpv6() : node.getIpv6NonLL()).equals(actualIP6.getLocal()) ||
                                    (linkLocal ? (actualIP6.getAddress() != null) :
                                            !desired.getPeerIPv6().equals(actualIP6.getAddress()));
                    if(needCreateIP6Addr) {
                        logger.info("IPv6 addresses for " + desired.getId() + " is outdated.\n" +
                                "Prefixes match: " + (actualIP6.getPrefixlen() == (linkLocal ? 64 : 128)) + "\n" +
                                "Local addresses match: " + ((linkLocal ? node.getIpv6() : node.getIpv6NonLL()).equals(actualIP6.getLocal())) + "\n" +
                                "Peer addresses match: " + (linkLocal ? (actualIP6.getAddress() == null) :
                                desired.getPeerIPv6().equals(actualIP6.getAddress())));
                    }
                }
                needCreateAddrs = needCreateIP4Addr || needCreateIP6Addr;
                if(needCreateAddrs)
                    logger.info("Recreating addresses for " + desired.getId() +
                            " since IPv4 or IPv6 information is updated: " + needCreateIP4Addr + ", " + needCreateIP6Addr + ".");
            }
        }

        final List<List<String>> changes = new ArrayList<>();
        if(needCreateInterface)
            changes.add(IP.Link.add(desired.getInterface(), "wireguard"));
        if(needCreateAddrs) {
            changes.add(IP.Addr.flush(desired.getInterface()));
            changes.add(IP.Addr.add(node.getIpv4() + "/32",
                    desired.getInterface(),
                    desired.getPeerIPv4() + "/32"));
            if(!desired.getPeerIPv6().isEmpty()) {
                if(linkLocal)
                    changes.add(IP.Addr.add(node.getIpv6() + "/64",
                            desired.getInterface(),
                            null));
                else
                    changes.add(IP.Addr.add(node.getIpv6NonLL() + "/128",
                            desired.getInterface(),
                            desired.getPeerIPv6() + "/128"));
            }
        }
        if(needUp)
            changes.add(IP.Link.set(desired.getInterface(), "up"));
        return changes
                .stream().map(cmd -> String.join(" ", cmd))
                .collect(Collectors.toList());
    }

    @Nonnull
    private Future<List<Change>> calculateTotalNetlinkChanges(@Nonnull Node node,
                                           @Nonnull List<WireGuardConfig> allDesired) {
        return IP.ip(vertx, new IPOptions(), IP.Addr.show(null))
                .compose(IP.Addr::handler)
                .compose(addrs -> {
                    final List<String> ipCommands = new ArrayList<>();
                    for (final WireGuardConfig desired : allDesired) {
                        final Address actual = searchActualAddress(addrs, desired.getInterface());
                        try {
                            ipCommands.addAll(calculateSingleNetlinkChanges(node,
                                    desired,
                                    actual));
                        } catch (IOException e) {
                            return Future.failedFuture(e);
                        }
                    }
                    final List<Change> changes = new ArrayList<>();
                    if(!ipCommands.isEmpty()) {
                        changes.add(new IPChange(true, ipCommands));
                    }
                    return Future.succeededFuture(changes);
                });
    }

    @Nonnull
    private Future<List<Change>> calculateTotalWireGuardChanges(@Nonnull Node node,
                                                              @Nonnull List<WireGuardConfig> allDesired) {
        return CompositeFuture.join(allDesired.stream().map(desired -> {
            return renderConfig(desired)
                    .compose(desiredConf -> {
                        return Future.succeededFuture(new WireGuardSyncConfChange(desired.getInterface(),
                                desiredConf.toString()));
                    });
        }).collect(Collectors.toList()))
                .compose(compositeFuture -> {
                    final List<Change> changes = new ArrayList<>(allDesired.size());
                    for (int i = 0; i < allDesired.size(); i ++) {
                        final Change change = compositeFuture.resultAt(i);
                        if(change == null) continue;
                        changes.add(change);
                    }
                    return Future.succeededFuture(changes);
                });
    }

    @Nonnull
    @Override
    public Future<List<Change>> calculateChanges(@Nonnull Node node, @Nonnull List<WireGuardConfig> allDesired) {
        return calculateTotalNetlinkChanges(node, allDesired)
                    .compose(changes -> {
            return calculateTotalWireGuardChanges(node, allDesired)
                    .compose(wireguardChanges -> {
                        changes.addAll(wireguardChanges);
                        return Future.succeededFuture(changes);
                    });
        });
    }
}
