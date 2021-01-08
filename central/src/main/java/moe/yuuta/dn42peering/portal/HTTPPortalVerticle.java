package moe.yuuta.dn42peering.portal;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
import moe.yuuta.dn42peering.asn.ASNHandler;
import moe.yuuta.dn42peering.manage.ManageHandler;

public class HTTPPortalVerticle extends AbstractVerticle {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        final TemplateEngine engine = FreeMarkerTemplateEngine.create(vertx, "ftlh");

        final Router router = Router.router(vertx);
        router.get("/")
                .produces("text/html")
                .handler(ctx -> {
                    final JsonObject data = new JsonObject()
                            .put("name", config().getJsonObject("http").getValue("name"));
                    engine.render(data, "index.ftlh", res -> {
                        if(res.succeeded()) {
                            ctx.response().end(res.result());
                        } else {
                            ctx.fail(res.cause());
                            logger.error("Cannot render index.", res.cause());
                        }
                    });
                });
        router.mountSubRouter("/asn", new ASNHandler().mount(vertx));
        router.mountSubRouter("/manage", new ManageHandler().mount(vertx));
        router.errorHandler(500, ctx -> {
            logger.error("Generic Error", ctx.failure());
        });
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080, res -> {
                    if(res.succeeded()) {
                        startPromise.complete();
                    } else {
                        startPromise.fail(res.cause());
                    }
                });
    }
}
