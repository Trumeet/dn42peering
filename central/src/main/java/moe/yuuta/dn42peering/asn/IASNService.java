package moe.yuuta.dn42peering.asn;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import moe.yuuta.dn42peering.whois.WhoisObject;

import javax.annotation.Nonnull;
import java.util.List;

@ProxyGen
public interface IASNService {
    String ADDRESS = IASNService.class.getName();

    @Nonnull
    static IASNService createProxy(@Nonnull Vertx vertx, @Nonnull String address) {
        return new IASNServiceVertxEBProxy(vertx, address);
    }

    @Fluent
    @Nonnull
    IASNService exists(@Nonnull String asn, boolean requireActivationStatus, boolean activated,
                @Nonnull Handler<AsyncResult<Boolean>> handler);

    @Fluent
    @Nonnull
    IASNService markAsActivated(@Nonnull String asn, @Nonnull Handler<AsyncResult<Void>> handler);

    @Fluent
    @Nonnull
    IASNService changePassword(@Nonnull String asn, @Nonnull String newPassword,
                        @Nonnull Handler<AsyncResult<Void>> handler);

    @Fluent
    @Nonnull
    IASNService registerOrChangePassword(@Nonnull String asn, @Nonnull String newPassword,
                               @Nonnull Handler<AsyncResult<Void>> handler);

    @Fluent
    @Nonnull
    IASNService auth(@Nonnull String asn, @Nonnull String password, @Nonnull Handler<AsyncResult<Boolean>> handler);

    @Fluent
    @Nonnull
    IASNService delete(@Nonnull String asn, @Nonnull Handler<AsyncResult<Void>> handler);

    @Fluent
    @Nonnull
    IASNService lookupEmails(@Nonnull WhoisObject asn, @Nonnull Handler<AsyncResult<List<String>>> handler);

    @Fluent
    @Nonnull
    IASNService count(@Nonnull Handler<AsyncResult<Integer>> handler);

    @Fluent
    @Nonnull
    IASNService list(@Nonnull Handler<AsyncResult<List<ASN>>> handler);
}
