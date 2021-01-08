package moe.yuuta.dn42peering.whois;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;

public class WhoisVerticle extends AbstractVerticle {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private MessageConsumer<JsonObject> consumer;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        consumer = new ServiceBinder(vertx)
                .setAddress(IWhoisService.ADDRESS)
                .register(IWhoisService.class, IWhoisService.create(vertx));
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
        consumer.unregister(stopPromise);
    }
}

