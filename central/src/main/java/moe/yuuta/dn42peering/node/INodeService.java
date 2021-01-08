package moe.yuuta.dn42peering.node;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import javax.annotation.Nonnull;
import java.util.List;

@ProxyGen
public interface INodeService {
    String ADDRESS = INodeService.class.getName();

    @Nonnull
    static INodeService createProxy(@Nonnull Vertx vertx) {
        return new INodeServiceVertxEBProxy(vertx, ADDRESS);
    }

    @Fluent
    @Nonnull
    INodeService listNodes(@Nonnull Handler<AsyncResult<List<Node>>> handler);

    @Fluent
    @Nonnull
    INodeService getNode(int id, @Nonnull Handler<AsyncResult<Node>> handler);
}
