package moe.yuuta.dn42peering.node;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.Pool;
import moe.yuuta.dn42peering.database.DatabaseUtils;

public class NodeVerticle extends AbstractVerticle {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private MessageConsumer<JsonObject> consumer;
    private Pool pool;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        pool = DatabaseUtils.getPool(vertx);

        consumer = new ServiceBinder(vertx)
                .setAddress(INodeService.ADDRESS)
                .setIncludeDebugInfo(true)
                .register(INodeService.class, new NodeServiceImpl(vertx, pool));
        consumer.completionHandler(ar -> {
            if(ar.succeeded()) {
                startPromise.complete();
            } else {
                startPromise.fail(ar.cause());
            }
        });
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        CompositeFuture.all(
                Future.future(f -> consumer.unregister(ar -> {
                    if(ar.succeeded()) f.complete();
                    else f.fail(ar.cause());
                })),
                Future.future(f -> pool.close(ar -> {
                    if(ar.succeeded()) f.complete();
                    else f.fail(ar.cause());
                }))
        ).onComplete(ar -> {
            if(ar.succeeded()) stopPromise.complete();
            else stopPromise.fail(ar.cause());
        });
    }
}
