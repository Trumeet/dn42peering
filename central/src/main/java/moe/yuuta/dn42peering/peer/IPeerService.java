package moe.yuuta.dn42peering.peer;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@ProxyGen
public interface IPeerService {
    String ADDRESS = IPeerService.class.getName();

    @Nonnull
    static IPeerService createProxy(@Nonnull Vertx vertx) {
        return new IPeerServiceVertxEBProxy(vertx, ADDRESS);
    }

    @Fluent
    @Nonnull
    IPeerService listUnderASN(@Nonnull String asn, @Nonnull Handler<AsyncResult<List<Peer>>> handler);

    @Fluent
    @Nonnull
    IPeerService getSingle(@Nonnull String asn, String id, @Nonnull Handler<AsyncResult<Peer>> handler);

    @Fluent
    @Nonnull
    IPeerService deletePeer(@Nonnull String asn, String id, @Nonnull Handler<AsyncResult<Void>> handler);

    @Fluent
    @Nonnull
    IPeerService updateTo(@Nonnull Peer peer, @Nonnull Handler<AsyncResult<Void>> handler);

    @Fluent
    @Nonnull
    IPeerService addNew(@Nonnull Peer peer, @Nonnull Handler<AsyncResult<Long>> handler);

    @Fluent
    @Nonnull
    IPeerService existsUnderASN(@Nonnull String asn, @Nonnull Handler<AsyncResult<Boolean>> handler);

    @Fluent
    @Nonnull
    IPeerService isIPConflict(@Nonnull Peer.VPNType type, @Nullable String ipv4, @Nullable String ipv6, @Nonnull Handler<AsyncResult<Boolean>> handler);

    @Fluent
    @Nonnull
    IPeerService changeProvisionStatus(int id, @Nonnull ProvisionStatus provisionStatus, @Nonnull Handler<AsyncResult<Void>> handler);
}
