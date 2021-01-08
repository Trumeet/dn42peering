package moe.yuuta.dn42peering.whois;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import javax.annotation.Nonnull;

@ProxyGen
public interface IWhoisService {
    String ADDRESS = IWhoisService.class.getName();

    static IWhoisService create(@Nonnull Vertx vertx) {
        return new WhoisServiceImpl(vertx);
    }

    @Nonnull
    static IWhoisService createProxy(@Nonnull Vertx vertx, @Nonnull String address) {
        return new IWhoisServiceVertxEBProxy(vertx, address);
    }

    @Fluent
    @Nonnull
    IWhoisService query(@Nonnull String handle, @Nonnull Handler<AsyncResult<WhoisObject>> handler);
}
