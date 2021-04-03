package moe.yuuta.dn42peering.admin.asn;

import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.RequestPredicate;
import io.vertx.ext.web.validation.ValidationHandler;
import io.vertx.ext.web.validation.builder.Bodies;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import moe.yuuta.dn42peering.asn.IASNService;
import moe.yuuta.dn42peering.portal.ISubRouter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;

public class ASNHandler implements ISubRouter {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    @Nonnull
    @Override
    public Router mount(@Nonnull Vertx vertx) {
        final IASNService asnService = IASNService.createProxy(vertx, IASNService.ADDRESS);
        final TemplateEngine engine = FreeMarkerTemplateEngine.create(vertx, "ftlh");
        final SchemaParser parser = SchemaParser.createDraft7SchemaParser(
                SchemaRouter.create(vertx, new SchemaRouterOptions()));

        final Router router = Router.router(vertx);
        router.post().handler(BodyHandler.create().setBodyLimit(100 * 1024));

        router.get("/")
                .produces("text/html")
                .handler(ctx -> ASNAdminUI.renderIndex(
                        ctx.user().principal().getString("username"),
                        engine,
                        asnService,
                        ctx));
        router.get("/change-password")
                .produces("text/html")
                .handler(ctx -> {
                    final List<String> predefinedASN =
                            ctx.queryParam("asn");
                    ASNAdminUI.renderChangePassword(
                            ctx.user().principal().getString("username"),
                            predefinedASN == null || predefinedASN.isEmpty() ? null :
                                    predefinedASN.get(0),
                            null,
                            engine,
                            ctx);
                });
        router.post("/change-password")
                .produces("text/html")
                .handler(BodyHandler.create().setBodyLimit(100 * 1024))
                .handler(ValidationHandler
                        .builder(parser)
                        .body(Bodies.formUrlEncoded(objectSchema()
                                .property("asn", stringSchema())
                                .property("passwd", stringSchema())
                                .property("confirm", stringSchema())
                                .allowAdditionalProperties(false)))
                        .predicate(RequestPredicate.BODY_REQUIRED)
                        .build())
                .handler(ctx -> {
                    final String asn = ctx.user().principal().getString("username");
                    final JsonObject parameters = ctx.<RequestParameters>get(ValidationHandler.REQUEST_CONTEXT_KEY)
                            .body().getJsonObject();
                    final String targetASN = parameters.getString("asn");
                    final String passwd = parameters.getString("passwd");
                    final String confirm = parameters.getString("confirm");
                    if(targetASN == null || passwd == null || confirm == null ||
                    targetASN.isEmpty() || passwd.isEmpty() || confirm.isEmpty()) {
                        ASNAdminUI.renderChangePassword(asn,
                                targetASN,
                                Collections.singletonList("Some fields are not supplied."),
                                engine,
                                ctx);
                        return;
                    }
                    if(!passwd.equals(confirm)) {
                        ASNAdminUI.renderChangePassword(asn,
                                targetASN,
                                Collections.singletonList("Passwords mismatch."),
                                engine,
                                ctx);
                        return;
                    }
                    asnService.changePassword(targetASN, passwd, ar -> {
                        if(ar.succeeded()) {
                            // TODO: Destroy sessions?
                            ctx.response()
                                    .setStatusCode(303)
                                    .putHeader("Location", "/admin/asn")
                                    .end();
                        } else {
                            logger.error("Cannot change password for " + targetASN, ar.cause());
                        }
                    });
                });
        return router;
    }
}
