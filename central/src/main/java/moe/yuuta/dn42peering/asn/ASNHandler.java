package moe.yuuta.dn42peering.asn;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailResult;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.service.RouteToEBServiceHandler;
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
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import moe.yuuta.dn42peering.jaba.Pair;
import moe.yuuta.dn42peering.portal.FormException;
import moe.yuuta.dn42peering.portal.HTTPException;
import moe.yuuta.dn42peering.portal.ISubRouter;
import moe.yuuta.dn42peering.whois.IWhoisService;
import moe.yuuta.dn42peering.whois.WhoisObject;
import org.apache.commons.text.RandomStringGenerator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;

public class ASNHandler implements ISubRouter {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    @Nonnull
    @Override
    public Router mount(@Nonnull Vertx vertx) {
        final Router router = Router.router(vertx);
        router.get("/")
                .produces("text/html")
                .handler(RouteToEBServiceHandler.build(vertx.eventBus(), IASNHttpService.ADDRESS, "index"));

        final ObjectSchemaBuilder registerSchema = objectSchema()
                .allowAdditionalProperties(false)
                .requiredProperty("asn", stringSchema());
        final SchemaParser parser = SchemaParser.createDraft7SchemaParser(
                SchemaRouter.create(vertx, new SchemaRouterOptions()));
        router.post().handler(BodyHandler.create().setBodyLimit(100 * 1024));
        router.post("/")
                .produces("text/html")
                .handler(ValidationHandler
                        .builder(parser)
                        .body(Bodies.formUrlEncoded(registerSchema))
                        .predicate(RequestPredicate.BODY_REQUIRED)
                        .build())
                .handler(RouteToEBServiceHandler.build(vertx.eventBus(), IASNHttpService.ADDRESS, "register"));

        return router;
    }
}
