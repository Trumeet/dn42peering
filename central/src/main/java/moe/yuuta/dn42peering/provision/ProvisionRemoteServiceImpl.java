package moe.yuuta.dn42peering.provision;

import io.grpc.ManagedChannel;
import io.vertx.core.*;
import io.vertx.grpc.VertxChannelBuilder;
import moe.yuuta.dn42peering.agent.proto.VertxAgentGrpc;
import moe.yuuta.dn42peering.node.Node;

import javax.annotation.Nonnull;

class ProvisionRemoteServiceImpl implements IProvisionRemoteService {
    private final Vertx vertx;

    ProvisionRemoteServiceImpl(@Nonnull Vertx vertx) {
        this.vertx = vertx;
    }

    private @Nonnull ManagedChannel toChannel(@Nonnull NodeCommon node) {
        return VertxChannelBuilder.forAddress(vertx, node.getInternalIp(), node.getInternalPort())
                .usePlaintext()
                .build();
    }
    
    @Nonnull
    @Override
    public IProvisionRemoteService provisionBGP(@Nonnull NodeCommon node,
                                                @Nonnull BGPRequestCommon request,
                                                @Nonnull Handler<AsyncResult<Void>> handler) {
        final ManagedChannel channel = toChannel(node);
        final VertxAgentGrpc.AgentVertxStub stub = VertxAgentGrpc.newVertxStub(channel);
        stub.provisionBGP(request.toGRPC())
                .<Void>compose(reply -> Future.succeededFuture(null))
                .onComplete(res -> channel.shutdown())
                .onComplete(handler);
        return this;
    }

    @Nonnull
    @Override
    public IProvisionRemoteService reloadBGP(@Nonnull NodeCommon node,
                                             @Nonnull BGPRequestCommon request,
                                             @Nonnull Handler<AsyncResult<Void>> handler) {
        final ManagedChannel channel = toChannel(node);
        final VertxAgentGrpc.AgentVertxStub stub = VertxAgentGrpc.newVertxStub(channel);
        stub.reloadBGP(request.toGRPC())
                .<Void>compose(reply -> Future.succeededFuture(null))
                .onComplete(res -> channel.shutdown())
                .onComplete(handler);
        return this;
    }

    @Nonnull
    @Override
    public IProvisionRemoteService deleteBGP(@Nonnull NodeCommon node,
                                                  @Nonnull BGPRequestCommon request,
                                                  @Nonnull Handler<AsyncResult<Void>> handler) {
        final ManagedChannel channel = toChannel(node);
        final VertxAgentGrpc.AgentVertxStub stub = VertxAgentGrpc.newVertxStub(channel);
        stub.deleteBGP(request.toGRPC())
                .<Void>compose(reply -> Future.succeededFuture(null))
                .onComplete(res -> channel.shutdown())
                .onComplete(handler);
        return this;
    }

    @Nonnull
    @Override
    public IProvisionRemoteService provisionWG(@Nonnull NodeCommon node,
                                                         @Nonnull WGRequestCommon request,
                                                         @Nonnull Handler<AsyncResult<String>> handler) {
        final ManagedChannel channel = toChannel(node);
        final VertxAgentGrpc.AgentVertxStub stub = VertxAgentGrpc.newVertxStub(channel);
        stub.provisionWG(request.toGRPC())
                .compose(wgReply -> Future.succeededFuture(wgReply.getDevice()))
                .onComplete(res -> channel.shutdown())
                .onComplete(handler);
        return this;
    }

    @Nonnull
    @Override
    public IProvisionRemoteService reloadWG(@Nonnull NodeCommon node,
                                                      @Nonnull WGRequestCommon request,
                                                      @Nonnull Handler<AsyncResult<String>> handler) {
        final ManagedChannel channel = toChannel(node);
        final VertxAgentGrpc.AgentVertxStub stub = VertxAgentGrpc.newVertxStub(channel);
        stub.reloadWG(request.toGRPC())
                .compose(wgReply -> Future.succeededFuture(wgReply.getDevice()))
                .onComplete(res -> channel.shutdown())
                .onComplete(handler);
        return this;
    }

    @Nonnull
    @Override
    public IProvisionRemoteService deleteWG(@Nonnull NodeCommon node,
                                                           @Nonnull WGRequestCommon request,
                                                           @Nonnull Handler<AsyncResult<Void>> handler) {
        final ManagedChannel channel = toChannel(node);
        final VertxAgentGrpc.AgentVertxStub stub = VertxAgentGrpc.newVertxStub(channel);
        stub.deleteWG(request.toGRPC())
                .<Void>compose(wgReply -> Future.succeededFuture(null))
                .onComplete(res -> channel.shutdown())
                .onComplete(handler);
        return this;
    }
}
