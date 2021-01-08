package moe.yuuta.dn42peering.asn;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailResult;
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
        final IASNService asnService = IASNService.createProxy(vertx, IASNService.ADDRESS);
        final IWhoisService whoisService = IWhoisService.createProxy(vertx, IWhoisService.ADDRESS);
        // Preserve. We need to get email address out of it later.
        final JsonObject mailConfig = vertx.getOrCreateContext().config().getJsonObject("mail");
        final MailClient mailClient = MailClient.create(vertx, new MailConfig(mailConfig));

        final TemplateEngine engine = FreeMarkerTemplateEngine.create(vertx, "ftlh");
        final Router router = Router.router(vertx);
        router.get("/")
                .produces("text/html")
                .handler(ctx -> {
                    renderIndex(engine, null, null, res -> {
                        if(res.succeeded()) {
                            ctx.response().end(res.result());
                        } else {
                            ctx.fail(res.cause());
                            logger.error("Cannot render /asn.", res.cause());
                        }
                    });
                });

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
                .handler(ctx -> {
                    final JsonObject parameters = ctx.<RequestParameters>get(ValidationHandler.REQUEST_CONTEXT_KEY)
                            .body().getJsonObject();
                    final String upperASN = parameters.getString("asn").toUpperCase();
                    // Start: Check if the ASN exists.
                    Future.<Void>future(f -> asnService.exists(upperASN, true, true, ar -> {
                        if(ar.succeeded()) {
                            if(ar.result()) {
                                f.fail(new FormException("This ASN exists in our records. Please login instead of registering."));
                            } else {
                                f.complete();
                            }
                        } else {
                            f.fail(ar.cause());
                        }
                    }))
                    // Lookup ASN
                    .<WhoisObject>compose(exists ->
                            Future.future(f -> whoisService.query(upperASN, f)))
                    // Lookup emails
                    .<List<String>>compose(asnLookup -> {
                        if(asnLookup == null) {
                            return Future.failedFuture(new FormException("The ASN is not found in the DN42 registry."));
                        } else {
                            return Future.future(f -> asnService.lookupEmails(asnLookup, ar -> {
                                if(ar.succeeded()) {
                                    if(ar.result().isEmpty()) {
                                        f.fail(new FormException("The tech-c contact for this ASN does not have emails."));
                                    } else {
                                        f.complete(ar.result());
                                    }
                                } else {
                                    f.fail(ar.cause());
                                }
                            }));
                        }
                    })
                    // Generate random password and register.
                    .<Pair<String /* Random password */, List<String> /* Emails */>>compose(emails -> Future.future(f -> {
                        final String randomPassword = new RandomStringGenerator.Builder()
                                .withinRange('a', 'z')
                                .build()
                                .generate(15);
                        asnService.registerOrChangePassword(upperASN, randomPassword, ar -> {
                            if(ar.succeeded()) {
                                f.complete(new Pair<>(randomPassword, emails));
                            } else {
                                f.fail(ar.cause());
                            }
                        });
                    }))
                    // Send mails.
                    .compose(pair -> CompositeFuture.any(Stream.of(pair.b)
                            .map(mail -> new MailMessage()
                                    .setFrom(mailConfig.getString("from"))
                                    .setTo(mail)
                                    .setText(String.format("Hi %s! Welcome to dn42 peering! Your peering initial password is %s. Make sure to change it.",
                                            upperASN,
                                            pair.a))
                                    .setSubject("Peering initial password"))
                            .map(message -> Future.<MailResult>future(f -> mailClient.sendMail(message, f)))
                            .collect(Collectors.toList())))
                    // Render HTML or report errors
                    .onComplete(ar -> {
                        if(ar.succeeded()) {
                            // Get MailResult's out of the future.
                            final List<MailResult> sendRes = ar.result().list();
                            renderSuccess(engine, sendRes, res -> {
                                if(res.succeeded()) {
                                    ctx.response().end(res.result());
                                } else {
                                    ctx.fail(res.cause());
                                    logger.error("Cannot render /asn (success).", res.cause());
                                }
                            });
                        } else {
                            if(ar.cause() instanceof HTTPException) {
                                ctx.response().setStatusCode(((HTTPException) ar.cause()).code).end();
                            } else if(ar.cause() instanceof FormException) {
                                renderIndex(engine,
                                        Arrays.asList(((FormException) ar.cause()).errors.clone()),
                                        upperASN,
                                        res -> {
                                    if(res.succeeded()) {
                                        ctx.response().end(res.result());
                                    } else {
                                        ctx.fail(res.cause());
                                        logger.error("Cannot render /asn (with errors).", res.cause());
                                    }
                                });
                            } else {
                                logger.error(String.format("Cannot register ASN %s.", upperASN), ar.cause());
                                ctx.fail(ar.cause());
                            }
                        }
                    });
                });

        return router;
    }

    private void renderIndex(@Nonnull TemplateEngine engine,
                             @Nullable List<String> errors,
                             @Nullable String asn,
                             @Nonnull Handler<AsyncResult<Buffer>> handler) {
        final Map<String, Object> root = new HashMap<>();
        root.put("input_asn", asn == null ? "" : asn);
        root.put("errors", errors);
        engine.render(root, "asn/index.ftlh", handler);
    }

    private void renderSuccess(@Nonnull TemplateEngine engine,
                                 @Nonnull List<MailResult> sendRes,
                                 @Nonnull Handler<AsyncResult<Buffer>> handler) {
        final Map<String, Object> root = new HashMap<>();
        root.put("emails", sendRes.stream()
                .filter(Objects::nonNull) // Nulls mean failures.
                .flatMap(res -> res.getRecipients().stream())
                .collect(Collectors.toList()));
        engine.render(root, "asn/success.ftlh", handler);
    }
}
