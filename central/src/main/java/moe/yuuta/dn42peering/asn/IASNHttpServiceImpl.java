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
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
import moe.yuuta.dn42peering.jaba.Pair;
import moe.yuuta.dn42peering.portal.FormException;
import moe.yuuta.dn42peering.portal.HTTPException;
import moe.yuuta.dn42peering.portal.RenderingUtils;
import moe.yuuta.dn42peering.whois.IWhoisService;
import moe.yuuta.dn42peering.whois.WhoisObject;
import org.apache.commons.text.RandomStringGenerator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class IASNHttpServiceImpl implements IASNHttpService {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private final IASNService asnService;
    private final IWhoisService whoisService;
    private final MailClient mailClient;
    private final JsonObject mailConfig;
    private final TemplateEngine templateEngine;

    // Testing API
    public IASNHttpServiceImpl(@Nonnull IASNService asnService,
                               @Nonnull IWhoisService whoisService,
                               @Nonnull MailClient mailClient,
                               @Nonnull JsonObject mailConfig,
                               @Nonnull TemplateEngine templateEngine) {
        this.asnService = asnService;
        this.whoisService = whoisService;
        this.mailClient = mailClient;
        this.mailConfig = mailConfig;
        this.templateEngine = templateEngine;
    }

    public IASNHttpServiceImpl(@Nonnull Vertx vertx) {
        this.asnService = IASNService.createProxy(vertx, IASNService.ADDRESS);
        this.whoisService = IWhoisService.createProxy(vertx, IWhoisService.ADDRESS);
        mailConfig = vertx.getOrCreateContext().config().getJsonObject("mail");
        mailClient = MailClient.create(vertx, new MailConfig(mailConfig));
        templateEngine = FreeMarkerTemplateEngine.create(vertx, "ftlh");
    }

    @Override
    public void index(@Nonnull ServiceRequest context, @Nonnull Handler<AsyncResult<ServiceResponse>> handler) {
        renderIndex(null, null, handler);
    }

    @Override
    public void register(@Nonnull JsonObject parameters,
                         @Nonnull ServiceRequest context,
                         @Nonnull Handler<AsyncResult<ServiceResponse>> handler) {
        final String upperASN = parameters.getString("asn").toUpperCase();
        // Start: Check if the ASN exists.
        Future.<Void>future(f -> asnService.exists(upperASN, true, true, ar -> {
            if (ar.succeeded()) {
                if (ar.result()) {
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
                    if (asnLookup == null) {
                        return Future.failedFuture(new FormException("The ASN is not found in the DN42 registry."));
                    } else {
                        return Future.future(f -> asnService.lookupEmails(asnLookup, ar -> {
                            if (ar.succeeded()) {
                                if (ar.result().isEmpty()) {
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
                        if (ar.succeeded()) {
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
                .onSuccess(res -> {
                    // Get MailResult's out of the future.
                    final List<MailResult> sendRes = res.list();
                    renderSuccess(sendRes, handler);
                })
                .onFailure(err -> {
                    if (err instanceof HTTPException) {
                        handler.handle(Future.succeededFuture(new ServiceResponse(((HTTPException) err).code, null, null, null)));
                    } else if (err instanceof FormException) {
                        renderIndex(Arrays.asList(((FormException) err).errors.clone()),
                                upperASN,
                                handler);
                    } else {
                        logger.error(String.format("Cannot register ASN %s.", upperASN), err);
                        handler.handle(Future.failedFuture(err));
                    }
                });
    }

    private void renderIndex(@Nullable List<String> errors,
                             @Nullable String asn,
                             @Nonnull Handler<AsyncResult<ServiceResponse>> handler) {
        final Map<String, Object> root = new HashMap<>();
        root.put("input_asn", asn == null ? "" : asn);
        root.put("errors", errors);
        templateEngine.render(root, "asn/index.ftlh", RenderingUtils.getGeneralRenderingHandler(handler));
    }

    private void renderSuccess(@Nonnull List<MailResult> sendRes,
                               @Nonnull Handler<AsyncResult<ServiceResponse>> handler) {
        final Map<String, Object> root = new HashMap<>();
        root.put("emails", sendRes.stream()
                .filter(Objects::nonNull) // Nulls mean failures.
                .flatMap(res -> res.getRecipients().stream())
                .collect(Collectors.toList()));
        templateEngine.render(root, "asn/success.ftlh", RenderingUtils.getGeneralRenderingHandler(handler));
    }
}
