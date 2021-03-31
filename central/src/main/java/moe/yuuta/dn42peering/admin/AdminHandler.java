package moe.yuuta.dn42peering.admin;

import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.RequestPredicate;
import io.vertx.ext.web.validation.ValidationHandler;
import io.vertx.ext.web.validation.builder.Bodies;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import moe.yuuta.dn42peering.asn.IASNService;
import moe.yuuta.dn42peering.manage.AdminASNAuthProvider;
import moe.yuuta.dn42peering.portal.ISubRouter;

import javax.annotation.Nonnull;

import java.util.UUID;

import static io.vertx.json.schema.common.dsl.Schemas.*;

public class AdminHandler implements ISubRouter {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    @Nonnull
    @Override
    public Router mount(@Nonnull Vertx vertx) {
        final IASNService asnService = IASNService.createProxy(vertx, IASNService.ADDRESS);
        final TemplateEngine engine = FreeMarkerTemplateEngine.create(vertx, "ftlh");

        final Router router = Router.router(vertx);
        router.post().handler(BodyHandler.create().setBodyLimit(100 * 1024));
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
        router.route().handler(
                BasicAuthHandler.create(
                        new AdminASNAuthProvider(vertx.getOrCreateContext()
                                .config()
                                .getString("admin", UUID.randomUUID().toString()),
                                asnService), "admin portal"));

        router.get("/")
                .produces("text/html")
                .handler(ctx -> {
                    final String asn = ctx.user().principal().getString("username");
                    AdminUI.renderIndex(engine, asn, ctx);
                });

        router.get("/sudo")
                .produces("text/html")
                .handler(ctx -> {
                    final String asn = ctx.user().principal().getString("username");
                    final Cookie cookie = ctx.getCookie(SudoUtils.SUDO_COOKIE);
                    AdminUI.renderSudo(engine, asn, null,
                            cookie == null ? null : SudoUtils.getTargetASN(cookie), ctx);
                });

        final ObjectSchemaBuilder registerSchema = objectSchema()
                .allowAdditionalProperties(false)
                .property("asn", stringSchema());
        final SchemaParser parser = SchemaParser.createDraft7SchemaParser(
                SchemaRouter.create(vertx, new SchemaRouterOptions()));

        router.post("/sudo")
                .handler(BodyHandler.create().setBodyLimit(100 * 1024))
                .handler(ValidationHandler
                        .builder(parser)
                        .body(Bodies.formUrlEncoded(registerSchema))
                        .predicate(RequestPredicate.BODY_REQUIRED)
                        .build())
                .handler(ctx -> {
                    final String asn = ctx.user().principal().getString("username");
                    final JsonObject parameters = ctx.<RequestParameters>get(ValidationHandler.REQUEST_CONTEXT_KEY)
                            .body().getJsonObject();
                    final String targetASN = parameters.getString("asn");
                    if (asn == null || asn.equals("")) {
                        // Clear
                        ctx.removeCookie(SudoUtils.SUDO_COOKIE);
                        ctx.response()
                                .setStatusCode(303)
                                .putHeader("Location", "/admin")
                                .end();
                    } else {
                        ctx.addCookie(Cookie.cookie(SudoUtils.SUDO_COOKIE, targetASN).setPath("/"));
                        ctx.response()
                                .setStatusCode(303)
                                .putHeader("Location", "/manage")
                                .end();
                    }
                });

        return router;
    }
}
