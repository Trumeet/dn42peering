package moe.yuuta.dn42peering.portal;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

import javax.annotation.Nonnull;

public interface ISubRouter {
    @Nonnull Router mount(@Nonnull Vertx vertx);
}
