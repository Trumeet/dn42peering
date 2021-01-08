package moe.yuuta.dn42peering.agent.provision;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@ProxyGen
public interface IProvisionService {
    String ADDRESS = IProvisionService.class.getName();

    @Nonnull
    static IProvisionService create(@Nonnull Vertx vertx) {
        return new IProvisionServiceVertxEBProxy(vertx, ADDRESS);
    }

    @Fluent
    @Nonnull
    IProvisionService provisionBGP(@Nonnull String localIP4,
                                   @Nonnull String localIP6,
                                   int id,
                                   @Nonnull String ipv4,
                                   @Nullable String ipv6,
                                   @Nullable String device,
                                   boolean mpbgp,
                                   @Nonnull String asn,
                                   @Nonnull Handler<AsyncResult<Void>> handler);

    @Fluent
    @Nonnull
    IProvisionService reloadBGP(@Nonnull String localIP4,
                                @Nonnull String localIP6,
                                int id,
                                @Nonnull String ipv4,
                                @Nullable String ipv6,
                                @Nullable String device,
                                boolean mpbgp,
                                @Nonnull String asn,
                                @Nonnull Handler<AsyncResult<Void>> handler);

    @Fluent
    @Nonnull
    IProvisionService unprovisionBGP(int id, @Nonnull Handler<AsyncResult<Void>> handler);

    @Fluent
    @Nonnull
    IProvisionService provisionVPNWireGuard(@Nonnull String localIP4,
                                            @Nonnull String localIP6,
                                            int id,
                                            int listenPort,
                                            @Nullable String endpointWithPort,
                                            @Nonnull String peerPubKey,
                                            @Nonnull String selfPrivKey,
                                            @Nonnull String selfPresharedSecret,
                                            @Nonnull String peerIPv4,
                                            @Nullable String peerIPv6,
                                            @Nonnull Handler<AsyncResult<String>> handler);

    @Fluent
    @Nonnull
    IProvisionService reloadVPNWireGuard(@Nonnull String localIP4,
                                         @Nonnull String localIP6,
                                         int id,
                                         int listenPort,
                                         @Nullable String endpointWithPort,
                                         @Nonnull String peerPubKey,
                                         @Nonnull String selfPrivKey,
                                         @Nonnull String selfPresharedSecret,
                                         @Nonnull String peerIPv4,
                                         @Nullable String peerIPv6,
                                         @Nonnull Handler<AsyncResult<String>> handler);

    @Fluent
    @Nonnull
    IProvisionService unprovisionVPNWireGuard(int id, @Nonnull Handler<AsyncResult<Void>> handler);
}
