package moe.yuuta.dn42peering.manage;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
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
import moe.yuuta.dn42peering.admin.SudoUtils;
import moe.yuuta.dn42peering.asn.IASNService;
import moe.yuuta.dn42peering.jaba.Pair;
import moe.yuuta.dn42peering.node.INodeService;
import moe.yuuta.dn42peering.peer.IPeerService;
import moe.yuuta.dn42peering.peer.Peer;
import moe.yuuta.dn42peering.portal.FormException;
import moe.yuuta.dn42peering.portal.HTTPException;
import moe.yuuta.dn42peering.portal.ISubRouter;
import moe.yuuta.dn42peering.provision.IProvisionRemoteService;
import moe.yuuta.dn42peering.whois.IWhoisService;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static io.vertx.ext.web.validation.builder.Parameters.param;
import static io.vertx.json.schema.common.dsl.Schemas.*;
import static moe.yuuta.dn42peering.manage.ManagementUI.*;

public class ManageHandler implements ISubRouter {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    @Nonnull
    @Override
    public Router mount(@Nonnull Vertx vertx) {
        final IASNService asnService = IASNService.createProxy(vertx, IASNService.ADDRESS);
        final IWhoisService whoisService = IWhoisService.createProxy(vertx, IWhoisService.ADDRESS);
        final IPeerService peerService = IPeerService.createProxy(vertx);
        final INodeService nodeService = INodeService.createProxy(vertx);
        final IProvisionRemoteService provisionService = IProvisionRemoteService.create(vertx);
        final TemplateEngine engine = FreeMarkerTemplateEngine.create(vertx, "ftlh");

        final Router router = Router.router(vertx);
        router.post().handler(BodyHandler.create().setBodyLimit(100 * 1024));
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
        router.route().handler(BasicAuthHandler.create(new ASNAuthProvider(asnService), "manage portal"));
        router.route().handler(ctx -> {
            // Mark as activated.
            asnService.markAsActivated(getActualASN(ctx, false), ar -> {
                if (ar.succeeded()) {
                    ctx.next();
                } else {
                    ctx.fail(ar.cause());
                }
            });
        });

        router.get("/")
                .produces("text/html")
                .handler(ctx -> {
                    final String asn = getActualASN(ctx, true);
                    Future.<List<moe.yuuta.dn42peering.peer.Peer>>future(f ->
                            peerService.listUnderASN(asn, f))
                            .onSuccess(peers -> renderIndex(engine, asn, peers, ctx))
                            .onFailure(ctx::fail);
                });

        router.get("/new")
                .produces("text/html")
                .handler(ctx -> {
                    final String asn = getActualASN(ctx, true);
                    renderForm(engine, nodeService, true, asn, null, null, ctx);
                });

        final ObjectSchemaBuilder registerSchema = objectSchema()
                .allowAdditionalProperties(false)
                .property("ipv4", stringSchema())
                .property("ipv6", stringSchema())
                .property("mpbgp", stringSchema())
                .property("vpn", enumSchema("wg"))
                .property("wg_endpoint", stringSchema())
                .property("wg_endpoint_port", stringSchema())
                .property("wg_pubkey", stringSchema())
                .property("node", stringSchema());
        final SchemaParser parser = SchemaParser.createDraft7SchemaParser(
                SchemaRouter.create(vertx, new SchemaRouterOptions()));

        router.post("/new")
                .handler(BodyHandler.create().setBodyLimit(100 * 1024))
                .handler(ValidationHandler
                        .builder(parser)
                        .body(Bodies.formUrlEncoded(registerSchema))
                        .predicate(RequestPredicate.BODY_REQUIRED)
                        .build())
                .handler(ctx -> {
                    final String asn = getActualASN(ctx, true);
                    final JsonObject parameters = ctx.<RequestParameters>get(ValidationHandler.REQUEST_CONTEXT_KEY)
                            .body().getJsonObject();
                    // Parse peer
                    parseForm(nodeService, parameters)
                            .compose(peer -> {
                                // Keys are generated during parsing.
                                peer.setAsn(asn);
                                return Future.succeededFuture(peer);
                            }).compose(peer ->
                            ManagementVerify.verifyAllOrThrow(whoisService, peer, asn)
                    ).<Peer>compose(peer -> Future.future(f -> {
                        boolean needCheckIP6 = true;
                        try {
                            if(peer.isIPv6LinkLocal())
                                needCheckIP6 = false;
                        } catch (IOException ignored) {}
                        peerService.isIPConflict(peer.getType(),
                                peer.getIpv4(),
                                needCheckIP6 ? peer.getIpv6() : null,
                                ar -> {
                                    if (ar.succeeded()) {
                                        if (ar.result()) {
                                            f.fail(new FormException(peer,
                                                    "The IPv4 or IPv6 you specified conflicts with an existing peering with the same type."));
                                        } else {
                                            f.complete(peer);
                                        }
                                    } else {
                                        f.fail(ar.cause());
                                    }
                                });
                            })
                    ).<Peer>compose(peer -> Future.future(f -> peerService.addNew(peer, ar -> {
                        if (ar.succeeded()) {
                            peer.setId((int) (long) ar.result());
                            f.complete(peer);
                        } else f.fail(ar.cause());
                    })))
                            .onSuccess(peer -> {
                                ctx.response()
                                        .setStatusCode(303)
                                        .putHeader("Location", "/manage")
                                        .end();
                                provisionService.deploy(peer.getNode(), ar -> {});
                            })
                            .onFailure(err -> {
                                if (err instanceof FormException) {
                                    renderForm(engine, nodeService,
                                            true, asn,
                                            ((Peer) ((FormException) err).data),
                                            Arrays.asList(((FormException) err).errors),
                                            ctx);
                                } else {
                                    if(!(err instanceof HTTPException)) logger.error("Cannot add peer.", err);
                                    ctx.fail(err);
                                }
                            });
                });

        router.get("/edit")
                .produces("text/html")
                .handler(ValidationHandler
                        .builder(parser)
                        .queryParameter(param("id", stringSchema()))
                        .build())
                .handler(ctx -> {
                    final String asn = getActualASN(ctx, true);
                    final String id = ctx.<RequestParameters>get(ValidationHandler.REQUEST_CONTEXT_KEY)
                            .queryParameter("id").getString();
                    Future.<Peer>future(f -> peerService.getSingle(asn, id, f))
                            .compose(peer -> {
                                if (peer == null) {
                                    return Future.failedFuture(new HTTPException(404));
                                }
                                return Future.succeededFuture(peer);
                            })
                            .onSuccess(peer -> Future.future(f -> renderForm(engine, nodeService, false,
                                    asn, peer, null, ctx)))
                            .onFailure(ctx::fail);
                });

        router.post("/edit")
                .handler(BodyHandler.create().setBodyLimit(100 * 1024))
                .handler(ValidationHandler
                        .builder(parser)
                        .body(Bodies.formUrlEncoded(registerSchema))
                        .queryParameter(param("id", stringSchema()))
                        .predicate(RequestPredicate.BODY_REQUIRED)
                        .build())
                .handler(ctx -> {
                    final String asn = getActualASN(ctx, true);
                    final JsonObject parameters = ctx.<RequestParameters>get(ValidationHandler.REQUEST_CONTEXT_KEY)
                            .body().getJsonObject();
                    final String id = ctx.<RequestParameters>get(ValidationHandler.REQUEST_CONTEXT_KEY)
                            .queryParameter("id").getString();
                    Future.<Peer>future(f -> peerService.getSingle(asn, id, f))
                            .compose(peer -> {
                                if (peer == null) {
                                    return Future.failedFuture(new HTTPException(404));
                                }
                                return Future.succeededFuture(peer);
                            }).compose(existingPeer ->
                            parseForm(nodeService, parameters)
                                    .compose(inPeer -> {
                                        // Preserve keys
                                        inPeer.setWgSelfPrivKey(existingPeer.getWgSelfPrivKey());
                                        inPeer.setWgSelfPubkey(existingPeer.getWgSelfPubkey());
                                        inPeer.setWgPresharedSecret(existingPeer.getWgPresharedSecret());
                                        inPeer.setAsn(asn);
                                        inPeer.setId(existingPeer.getId());
                                        return Future.succeededFuture(new Pair<>(existingPeer, inPeer));
                                    })
                    ).compose(peer -> {
                                final Peer inPeer = peer.b;
                                return ManagementVerify.verifyAllOrThrow(whoisService, inPeer, asn)
                                        .compose(_peer -> Future.succeededFuture(peer));
                            }
                    ).<Pair<Peer /* Existing */, Peer /* Input */>>compose(peer -> {
                                final Peer existingPeer = peer.a;
                                final Peer inPeer = peer.b;
                                final boolean needCheckIPv4Conflict = ManagementVerify.determineIfNeedCheckIPv4(existingPeer, inPeer);
                                final boolean needCheckIPv6Conflict = ManagementVerify.determineIfNeedCheckIPv6(existingPeer, inPeer);
                                return Future.future(f -> peerService.isIPConflict(inPeer.getType(),
                                        needCheckIPv4Conflict ? inPeer.getIpv4() : null,
                                        needCheckIPv6Conflict ? inPeer.getIpv6() : null,
                                        ar -> {
                                            if (ar.succeeded()) {
                                                if (ar.result()) {
                                                    f.fail(new FormException(inPeer,
                                                            "The IPv4 or IPv6 you specified conflicts with an existing peering with the same type."));
                                                } else {
                                                    f.complete(peer);
                                                }
                                            } else {
                                                f.fail(ar.cause());
                                            }
                                        }));
                            }
                    ).<Pair<Peer /* Existing */, Peer /* Input */>>compose(peer ->
                            Future.future(f -> peerService.updateTo(peer.b /* New Peer */, ar -> {
                                if (ar.succeeded()) f.complete(peer);
                                else f.fail(ar.cause());
                            })))
                            .onSuccess(pair -> {
                                ctx.response()
                                        .setStatusCode(303)
                                        .putHeader("Location", "/manage")
                                        .end();
                                final Peer existingPeer = pair.a;
                                final Peer inPeer = pair.b;
                                provisionService.deploy(existingPeer.getNode(), ar -> {});
                                provisionService.deploy(inPeer.getNode(), ar -> {});
                            })
                            .onFailure(err -> {
                                if (err instanceof FormException) {
                                    final Peer peer = (Peer) ((FormException) err).data;
                                    if(peer != null) {
                                        // The exception may be generated from parseForm
                                        // In this case, the peer contains default data (like ID, keys, asn)
                                        // ID is the most important one since it determines the action of the form
                                        // so we need to manually ensure it here.
                                        peer.setId(Integer.parseInt(id)); // It must work.
                                    }
                                    renderForm(engine,
                                            nodeService,
                                            false,
                                            asn,
                                            peer,
                                            Arrays.asList(((FormException) err).errors),
                                            ctx);
                                } else {
                                    if(!(err instanceof HTTPException)) logger.error("Cannot edit peer.", err);
                                    ctx.fail(err);
                                }
                            });
                });

        router.get("/delete")
                .handler(ValidationHandler
                        .builder(parser)
                        .queryParameter(param("id", stringSchema()))
                        .build())
                .handler(ctx -> {
                    final String asn = getActualASN(ctx, true);
                    final String id = ctx.<RequestParameters>get(ValidationHandler.REQUEST_CONTEXT_KEY)
                            .queryParameter("id").getString();
                    Future.<Peer>future(f -> peerService.getSingle(asn, id, f))
                            .compose(peer -> {
                                if (peer == null) {
                                    return Future.failedFuture(new HTTPException(404));
                                }
                                return Future.succeededFuture(peer);
                            })
                            .compose(peer -> Future.<Void>future(f -> peerService.deletePeer(asn, id, f))
                            .compose(_v1 -> Future.succeededFuture(peer)))
                            .onSuccess(peer -> {
                                ctx.response()
                                        .setStatusCode(303)
                                        .putHeader("Location", "/manage")
                                        .end();
                                provisionService.deploy(peer.getNode(), ar -> {});
                            })
                            .onFailure(err -> {
                                if(!(err instanceof HTTPException)) logger.error("Cannot delete peer.", err);
                                ctx.fail(err);
                            });
                });

        router.get("/change-password")
                .produces("text/html")
                .handler(ctx -> {
                    final String asn = getActualASN(ctx, true);
                    renderChangepw(engine, asn, null, ctx);
                });

        router.post("/change-password")
                .handler(BodyHandler.create().setBodyLimit(100 * 1024))
                .handler(ValidationHandler
                        .builder(parser)
                        .body(Bodies.formUrlEncoded(objectSchema()
                                .property("passwd", stringSchema())
                                .property("confirm", stringSchema())
                                .allowAdditionalProperties(false)))
                        .predicate(RequestPredicate.BODY_REQUIRED)
                        .build())
                .handler(ctx -> {
                    final String asn = getActualASN(ctx, true);
                    final JsonObject parameters = ctx.<RequestParameters>get(ValidationHandler.REQUEST_CONTEXT_KEY)
                            .body().getJsonObject();
                    final String passwd = parameters.getString("passwd");
                    final String confirm = parameters.getString("confirm");
                    Future.<Void>future(f -> {
                        if (passwd == null || confirm == null) {
                            f.fail(new FormException("Some fields are not supplied."));
                        } else {
                            if (!passwd.equals(confirm)) {
                                f.fail(new FormException("Two passwords do not match."));
                            } else {
                                f.complete();
                            }
                        }
                    }).<Void>compose(v -> Future.future(f -> asnService.changePassword(asn, passwd, f)))
                            .onSuccess(_void -> {
                                ctx.session().destroy();
                                ctx.response()
                                        .setStatusCode(303)
                                        .putHeader("Location", "/manage")
                                        .end();
                            })
                            .onFailure(err -> {
                                if (err instanceof FormException) {
                                    renderChangepw(engine, asn,
                                            Arrays.asList(((FormException) err).errors),
                                            ctx);
                                } else {
                                    if(!(err instanceof HTTPException)) logger.error("Cannot change password.", err);
                                    ctx.fail(err);
                                }
                            });
                });

        router.get("/delete-account")
                .produces("text/html")
                .handler(ctx -> {
                    final String asn = getActualASN(ctx, true);
                    renderDA(engine, asn, null, ctx);
                });

        router.post("/delete-account")
                .produces("text/html")
                .handler(ctx -> {
                    final String asn = getActualASN(ctx, true);
                    Future.<Void>future(f -> peerService.existsUnderASN(asn, ar -> {
                        if (ar.succeeded()) {
                            if (ar.result()) {
                                f.fail(new FormException("There are still active peers. Delete them before deleting the account."));
                            } else {
                                f.complete(null);
                            }
                        } else {
                            f.fail(ar.cause());
                        }
                    })).<Void>compose(v -> Future.future(f -> asnService.delete(asn, f)))
                            .onSuccess(_void -> {
                                ctx.session().destroy();
                                ctx.response()
                                        .setStatusCode(303)
                                        .putHeader("Location", "/")
                                        .end();
                            })
                            .onFailure(err -> {
                                if (err instanceof FormException) {
                                    renderDA(engine, asn,
                                            Arrays.asList(((FormException) err).errors),
                                            ctx);
                                } else {
                                    logger.error("Cannot delete account.", err);
                                    ctx.fail(err);
                                }
                            });
                });

        router.get("/show-configuration")
                .produces("text/html")
                .handler(ValidationHandler
                        .builder(parser)
                        .queryParameter(param("id", stringSchema()))
                        .build())
                .handler(ctx -> {
                    final String asn = getActualASN(ctx, true);
                    final String id = ctx.<RequestParameters>get(ValidationHandler.REQUEST_CONTEXT_KEY)
                            .queryParameter("id").getString();
                    Future.<Peer>future(f -> peerService.getSingle(asn, id, f))
                            .compose(peer -> {
                                if (peer == null) {
                                    return Future.failedFuture(new HTTPException(404));
                                }
                                return Future.succeededFuture(peer);
                            })
                            .onSuccess(peer -> renderShowConfig(nodeService, engine, peer, ctx))
                            .onFailure(ctx::fail);
                });

        return router;
    }

    @Nonnull
    private String getActualASN(@Nonnull RoutingContext ctx, boolean acceptSudo) {
        final String authASN = ctx.user().principal().getString("username");
        if(!acceptSudo) {
            return authASN;
        }
        if(ctx.getCookie(SudoUtils.SUDO_COOKIE) == null) {
            return authASN;
        }
        final Cookie sudoCookie = ctx.getCookie(SudoUtils.SUDO_COOKIE);
        if(ctx.user().attributes().getBoolean("admin") == null) {
            // Unauthorized
            logger.warn("Unauthorized sudo attempt by " + authASN + ", target ASN: " + SudoUtils.getTargetASN(sudoCookie));
            return authASN;
        }
        return SudoUtils.getTargetASN(sudoCookie);
    }
}
