package moe.yuuta.dn42peering.provision;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import javax.annotation.Nonnull;

@ProxyGen
public interface IProvisionRemoteService {
    String ADDRESS = IProvisionRemoteService.class.getName();

    @Nonnull
    static IProvisionRemoteService create(@Nonnull Vertx vertx) {
        return new IProvisionRemoteServiceVertxEBProxy(vertx, ADDRESS);
    }

    @Fluent
    @Nonnull
    IProvisionRemoteService deploy(long nodeId,
                                         @Nonnull Handler<AsyncResult<Void>> handler);
}
