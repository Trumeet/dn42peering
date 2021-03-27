package moe.yuuta.dn42peering.provision;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;

public class ProvisionVerticle extends AbstractVerticle {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private MessageConsumer<JsonObject> consumer;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        consumer = new ServiceBinder(vertx)
                .setAddress(IProvisionRemoteService.ADDRESS)
                .register(IProvisionRemoteService.class, new ProvisionRemoteServiceImpl(vertx));
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
        Future.future(f -> consumer.unregister(ar -> {
            if(ar.succeeded()) f.complete();
            else f.fail(ar.cause());
        })).onComplete(ar -> {
            if(ar.succeeded()) stopPromise.complete();
            else stopPromise.fail(ar.cause());
        });
    }
}
