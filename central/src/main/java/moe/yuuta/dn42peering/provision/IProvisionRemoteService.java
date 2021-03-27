package moe.yuuta.dn42peering.provision;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import moe.yuuta.dn42peering.node.Node;

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
    IProvisionRemoteService provisionBGP(@Nonnull NodeCommon node,
                                         @Nonnull BGPRequestCommon request,
                                         @Nonnull Handler<AsyncResult<Void>> handler);

    @Fluent
    @Nonnull
    IProvisionRemoteService reloadBGP(@Nonnull NodeCommon node,
                                      @Nonnull BGPRequestCommon request,
                                      @Nonnull Handler<AsyncResult<Void>> handler);

    @Fluent
    @Nonnull
    IProvisionRemoteService deleteBGP(@Nonnull NodeCommon node,
                                      @Nonnull BGPRequestCommon request,
                                      @Nonnull Handler<AsyncResult<Void>> handler);

    @Fluent
    @Nonnull
    IProvisionRemoteService provisionWG(@Nonnull NodeCommon node,
                                        @Nonnull WGRequestCommon request,
                                        @Nonnull Handler<AsyncResult<String>> handler);

    @Fluent
    @Nonnull
    IProvisionRemoteService reloadWG(@Nonnull NodeCommon node,
                                     @Nonnull WGRequestCommon request,
                                     @Nonnull Handler<AsyncResult<String>> handler);

    @Fluent
    @Nonnull
    IProvisionRemoteService deleteWG(@Nonnull NodeCommon node,
                                     @Nonnull WGRequestCommon request,
                                     @Nonnull Handler<AsyncResult<Void>> handler);
}
