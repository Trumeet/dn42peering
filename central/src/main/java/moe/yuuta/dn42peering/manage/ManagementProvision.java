package moe.yuuta.dn42peering.manage;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import moe.yuuta.dn42peering.jaba.Pair;
import moe.yuuta.dn42peering.node.INodeService;
import moe.yuuta.dn42peering.node.Node;
import moe.yuuta.dn42peering.peer.IPeerService;
import moe.yuuta.dn42peering.peer.Peer;
import moe.yuuta.dn42peering.peer.ProvisionStatus;
import moe.yuuta.dn42peering.provision.BGPRequestCommon;
import moe.yuuta.dn42peering.provision.IProvisionRemoteService;
import moe.yuuta.dn42peering.provision.WGRequestCommon;

import javax.annotation.Nonnull;
import java.io.IOException;

class ManagementProvision {
    private static final Logger logger = LoggerFactory.getLogger(ManagementProvision.class.getSimpleName());

    @Nonnull
    public static Future<Void> reloadPeer(@Nonnull INodeService nodeService,
                                    @Nonnull IProvisionRemoteService provisionService,
                                    @Nonnull Peer existingPeer, @Nonnull Peer inPeer) {
        // Check if we can reload on the fly.
        // Otherwise, we can only deprovision and provision.
        // This will cause unnecessary wastes.
        boolean canReload = inPeer.getType() == existingPeer.getType() &&
                inPeer.getNode() == existingPeer.getNode();
        // wg-quick does not support switching IP addresses.
        // TODO: Move reload detection to agents.
        if(canReload && // Only check if no other factors prevent us from reloading.
                inPeer.getType() == Peer.VPNType.WIREGUARD &&
                existingPeer.getType() == Peer.VPNType.WIREGUARD) {
            if(!inPeer.getIpv4().equals(existingPeer.getIpv6())) {
                canReload = false;
            }
            if(inPeer.getIpv6() != null && !inPeer.getIpv6().equals(existingPeer.getIpv6())) {
                try {
                    // LL addrs does not have anything to do with ifconfig.
                    if(inPeer.isIPv6LinkLocal() && existingPeer.isIPv6LinkLocal())
                        canReload = true;
                    else
                        canReload = false;
                } catch (IOException ignored) {}
            }
        }
        // wg-quick will also not clear EndPoint setting if we just reload it.
        if(canReload && // Only check if no other factors prevent us from reloading.
                inPeer.getType() == Peer.VPNType.WIREGUARD &&
                existingPeer.getType() == Peer.VPNType.WIREGUARD) {
            if(inPeer.getWgEndpoint() == null &&
                    existingPeer.getWgEndpoint() != null) {
                canReload = false;
            }
        }
        Future<Void> future;
        if (canReload) {
            future = Future.<Node>future(f -> nodeService.getNode(inPeer.getNode(), f))
                    .compose(node -> {
                        if(node == null || !node.getSupportedVPNTypes().contains(inPeer.getType())) {
                            return Future.failedFuture("The node does not exist");
                        }
                        return Future.succeededFuture(node);
                    })
                    .compose(node -> {
                        switch (existingPeer.getType()) {
                            case WIREGUARD:
                                final WGRequestCommon wgReq = inPeer.toWGRequest();
                                wgReq.setNode(node.toRPCNode());
                                return Future.<String>future(f -> provisionService.reloadWG(
                                        node.toRPCNode(),
                                        wgReq,
                                        f)
                                ).compose(device -> Future.succeededFuture(new Pair<>(node, device)));
                            default:
                                throw new UnsupportedOperationException("Bug: Unknown type.");
                        }
                    })
                    .compose(pair -> {
                        final BGPRequestCommon bgpReq = inPeer.toBGPRequest();
                        bgpReq.setNode(pair.a.toRPCNode());
                        bgpReq.setDevice(pair.b);
                        return Future.future(f -> provisionService.reloadBGP(
                                pair.a.toRPCNode(),
                                bgpReq,
                                f));
                    });
        } else {
            future = unprovisionPeer(nodeService, provisionService, existingPeer)
                    .compose(f -> provisionPeer(nodeService, provisionService, inPeer));
        }
        return future;
    }

    public static Future<Void> unprovisionPeer(@Nonnull INodeService nodeService,
                                         @Nonnull IProvisionRemoteService provisionService,
                                         @Nonnull Peer existingPeer) {
        return Future.<Node>future(f -> nodeService.getNode(existingPeer.getNode(), f))
                .compose(node -> {
                    if(node == null) {
                        return Future.failedFuture("The node does not exist");
                    }
                    return Future.succeededFuture(node);
                })
                .compose(node -> {
                    switch (existingPeer.getType()) {
                        case WIREGUARD:
                            return Future.<Void>future(f -> provisionService.deleteWG(
                                    node.toRPCNode(),
                                    new WGRequestCommon(null,
                                            (long)existingPeer.getId(),
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null),
                                    f))
                                    .compose(res -> Future.succeededFuture(node));
                        default:
                            throw new UnsupportedOperationException("Bug: Unknown type.");
                    }
                })
                .compose(node -> {
                    return Future.future(f -> provisionService.deleteBGP(
                            node.toRPCNode(),
                            new BGPRequestCommon(null,
                                    (long)existingPeer.getId(),
                                    null,
                                    null,
                                    null,
                                    null,
                                    null),
                            f));
                })
                ;
    }

    @Nonnull
    public static Future<Void> provisionPeer(@Nonnull INodeService nodeService,
                                       @Nonnull IProvisionRemoteService provisionService,
                                       @Nonnull Peer inPeer) {
        return Future.<Node>future(f -> nodeService.getNode(inPeer.getNode(), f))
                .compose(node -> {
                    if(node == null || !node.getSupportedVPNTypes().contains(inPeer.getType())) {
                        return Future.failedFuture("The node does not exist");
                    }
                    return Future.succeededFuture(node);
                })
                .compose(node -> {
                    switch (inPeer.getType()) {
                        case WIREGUARD:
                            final WGRequestCommon wgReq = inPeer.toWGRequest();
                            wgReq.setNode(node.toRPCNode());
                            return Future.<String>future(f -> provisionService.provisionWG(
                                    node.toRPCNode(),
                                    wgReq,
                                    f)
                            ).compose(device -> Future.succeededFuture(new Pair<>(node, device)));
                        default:
                            throw new UnsupportedOperationException("Bug: Unknown type.");
                    }
                })
                .compose(pair -> {
                    final BGPRequestCommon bgpReq = inPeer.toBGPRequest();
                    bgpReq.setNode(pair.a.toRPCNode());
                    bgpReq.setDevice(pair.b);
                    return Future.future(f -> provisionService.provisionBGP(
                            pair.a.toRPCNode(),
                            bgpReq,
                            f));
                });
    }

    public static void handleProvisionResult(@Nonnull IPeerService peerService,
                                       @Nonnull Peer inPeer,
                                       @Nonnull AsyncResult<Void> res) {
        if(res.succeeded()) {
            peerService.changeProvisionStatus(inPeer.getId(),
                    ProvisionStatus.PROVISIONED, ar -> {
                        if (ar.failed()) {
                            logger.error(String.format("Cannot update %d to provisioned.", inPeer.getId()), ar.cause());
                        }
                    });
        } else {
            logger.error(String.format("Cannot provision %d.", inPeer.getId()), res.cause());
            peerService.changeProvisionStatus(inPeer.getId(),
                    ProvisionStatus.FAIL, ar -> {
                        if (ar.failed()) {
                            logger.error(String.format("Cannot update %d to failed.", inPeer.getId()), ar.cause());
                        }
                    });
        }
    }
}
