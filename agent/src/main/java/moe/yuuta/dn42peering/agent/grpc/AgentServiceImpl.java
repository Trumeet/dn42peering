package moe.yuuta.dn42peering.agent.grpc;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import moe.yuuta.dn42peering.agent.proto.*;
import moe.yuuta.dn42peering.agent.provision.IProvisionService;

import javax.annotation.Nonnull;

class AgentServiceImpl extends VertxAgentGrpc.AgentVertxImplBase {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private final Vertx vertx;
    private final IProvisionService provisionService;

    AgentServiceImpl(@Nonnull Vertx vertx) {
        this.vertx = vertx;
        this.provisionService = IProvisionService.create(vertx);
    }

    @Override
    public Future<BGPReply> provisionBGP(BGPRequest request) {
        return Future.<Void>future(f -> provisionService.provisionBGP(
                request.getNode().getIpv4(),
                request.getNode().getIpv6(),
                (int)request.getId(),
                request.getIpv4(),
                request.getIpv6().isEmpty() ? null : request.getIpv6(),
                request.getDevice(),
                request.getMpbgp(),
                request.getAsn(),
                f))
                .compose(_v -> Future.succeededFuture(BGPReply.newBuilder().build()))
                .onFailure(err -> logger.error(String.format("Cannot provision BGP for %d", request.getId()),
                        err));
    }

    @Override
    public Future<BGPReply> reloadBGP(BGPRequest request) {
        return Future.<Void>future(f -> provisionService.reloadBGP(
                request.getNode().getIpv4(),
                request.getNode().getIpv6(),
                (int)request.getId(),
                request.getIpv4(),
                request.getIpv6().isEmpty() ? null : request.getIpv6(),
                request.getDevice(),
                request.getMpbgp(),
                request.getAsn(),
                f))
                .compose(_v -> Future.succeededFuture(BGPReply.newBuilder().build()))
                .onFailure(err -> logger.error(String.format("Cannot reload BGP for %d", request.getId()),
                        err));
    }

    @Override
    public Future<BGPReply> deleteBGP(BGPRequest request) {
        return Future.<Void>future(f -> provisionService.unprovisionBGP((int)request.getId(), f))
                .compose(_v -> Future.succeededFuture(BGPReply.newBuilder().build()))
                .onFailure(err -> logger.error(String.format("Cannot delete BGP for %d", request.getId()),
                        err));
    }

    @Override
    public Future<WGReply> provisionWG(WGRequest request) {
        return Future.<String>future(f -> provisionService.provisionVPNWireGuard(
                request.getNode().getIpv4(),
                request.getNode().getIpv6(),
                (int)request.getId(),
                request.getListenPort(),
                request.getEndpoint().isEmpty() ? "" : request.getEndpoint(),
                request.getPeerPubKey(),
                request.getSelfPrivKey(),
                request.getSelfPresharedSecret(),
                request.getPeerIPv4(),
                request.getPeerIPv6().isEmpty() ? null : request.getPeerIPv6(),
                f))
                .compose(dev -> Future.succeededFuture(WGReply.newBuilder()
                .setDevice(dev).build()))
                .onFailure(err -> logger.error(String.format("Cannot provision WireGuard for %d", request.getId()),
                        err));
    }

    @Override
    public Future<WGReply> reloadWG(WGRequest request) {
        return Future.<String>future(f -> provisionService.reloadVPNWireGuard(
                request.getNode().getIpv4(),
                request.getNode().getIpv6(),
                (int)request.getId(),
                request.getListenPort(),
                request.getEndpoint().isEmpty() ? "" : request.getEndpoint(),
                request.getPeerPubKey(),
                request.getSelfPrivKey(),
                request.getSelfPresharedSecret(),
                request.getPeerIPv4(),
                request.getPeerIPv6().isEmpty() ? null : request.getPeerIPv6(),
                f))
                .compose(dev -> Future.succeededFuture(WGReply.newBuilder()
                        .setDevice(dev).build()))
                .onFailure(err -> logger.error(String.format("Cannot reload WireGuard for %d", request.getId()),
                        err));
    }

    @Override
    public Future<WGReply> deleteWG(WGRequest request) {
        return Future.<Void>future(f -> provisionService.unprovisionVPNWireGuard((int)request.getId(), f))
                .compose(_v -> Future.succeededFuture(WGReply.newBuilder().build()))
                .onFailure(err -> logger.error(String.format("Cannot delete WireGuard for %d", request.getId()),
                        err));
    }
}
