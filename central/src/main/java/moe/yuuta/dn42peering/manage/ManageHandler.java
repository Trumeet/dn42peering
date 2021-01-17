package moe.yuuta.dn42peering.manage;

import com.wireguard.crypto.Key;
import com.wireguard.crypto.KeyFormatException;
import edazdarevic.commons.net.CIDRUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
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
import moe.yuuta.dn42peering.agent.proto.BGPRequest;
import moe.yuuta.dn42peering.agent.proto.VertxAgentGrpc;
import moe.yuuta.dn42peering.agent.proto.WGRequest;
import moe.yuuta.dn42peering.asn.IASNService;
import moe.yuuta.dn42peering.jaba.Pair;
import moe.yuuta.dn42peering.node.INodeService;
import moe.yuuta.dn42peering.node.Node;
import moe.yuuta.dn42peering.peer.IPeerService;
import moe.yuuta.dn42peering.peer.Peer;
import moe.yuuta.dn42peering.peer.ProvisionStatus;
import moe.yuuta.dn42peering.portal.FormException;
import moe.yuuta.dn42peering.portal.HTTPException;
import moe.yuuta.dn42peering.portal.ISubRouter;
import moe.yuuta.dn42peering.whois.IWhoisService;
import moe.yuuta.dn42peering.whois.WhoisObject;
import org.apache.commons.validator.routines.InetAddressValidator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.Inet6Address;
import java.util.*;
import java.util.stream.Collectors;

import static io.vertx.ext.web.validation.builder.Parameters.param;
import static io.vertx.json.schema.common.dsl.Schemas.*;

public class ManageHandler implements ISubRouter {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    @Nonnull
    @Override
    public Router mount(@Nonnull Vertx vertx) {
        final IASNService asnService = IASNService.createProxy(vertx, IASNService.ADDRESS);
        final IWhoisService whoisService = IWhoisService.createProxy(vertx, IWhoisService.ADDRESS);
        final IPeerService peerService = IPeerService.createProxy(vertx);
        final INodeService nodeService = INodeService.createProxy(vertx);
        final TemplateEngine engine = FreeMarkerTemplateEngine.create(vertx, "ftlh");

        final Router router = Router.router(vertx);
        router.post().handler(BodyHandler.create().setBodyLimit(100 * 1024));
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
        router.route().handler(BasicAuthHandler.create(new ASNAuthProvider(asnService)));
        router.route().handler(ctx -> {
            // Mark as activated.
            asnService.markAsActivated(ctx.user().principal().getString("username"), ar -> {
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
                    final String asn = ctx.user().principal().getString("username");
                    Future.<List<moe.yuuta.dn42peering.peer.Peer>>future(f ->
                            peerService.listUnderASN(asn, f))
                            .<Buffer>compose(peers -> Future.future(f -> renderIndex(engine, asn, peers, f)))
                            .onComplete(res -> {
                                if (res.succeeded()) {
                                    ctx.response().end(res.result());
                                } else {
                                    ctx.fail(res.cause());
                                }
                            });
                });

        router.get("/new")
                .produces("text/html")
                .handler(ctx -> {
                    final String asn = ctx.user().principal().getString("username");
                    renderForm(engine, nodeService, true, asn, null, null, res -> {
                        if (res.succeeded()) {
                            ctx.response().end(res.result());
                        } else {
                            res.cause().printStackTrace();
                            ctx.fail(res.cause());
                        }
                    });
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
                    final String asn = ctx.user().principal().getString("username");
                    final JsonObject parameters = ctx.<RequestParameters>get(ValidationHandler.REQUEST_CONTEXT_KEY)
                            .body().getJsonObject();
                    // Parse peer
                    parseForm(nodeService, parameters)
                            .compose(peer -> {
                                // Keys are generated during parsing.
                                peer.setAsn(asn);
                                return Future.succeededFuture(peer);
                            }).compose(peer ->
                            Future.<WhoisObject>future(f -> whoisService.query(peer.getIpv4(), f))
                                    .compose(whoisObject -> {
                                        if (whoisObject == null ||
                                                !whoisObject.containsKey("origin") ||
                                                !whoisObject.get("origin").contains(asn)) {
                                            return Future.failedFuture(new FormException(peer,
                                                    "The IPv4 address you specified does not have a route with your ASN."));
                                        }
                                        // Verify IPv6
                                        try {
                                            if (peer.getIpv6() != null && !peer.isIPv6LinkLocal()) {
                                                return Future.<WhoisObject>future(f -> whoisService.query(peer.getIpv6(), f))
                                                        .compose(ipv6Whois -> {
                                                            if (ipv6Whois == null ||
                                                                    !ipv6Whois.containsKey("origin") ||
                                                                    !ipv6Whois.get("origin").contains(asn)) {
                                                                return Future.failedFuture(new FormException(peer,
                                                                        "The IPv6 address you specified does not have a route with your ASN."));
                                                            } else {
                                                                return Future.succeededFuture(peer);
                                                            }
                                                        });
                                            } else {
                                                // Do not check for IPv6 address.
                                                return Future.succeededFuture(peer);
                                            }
                                        } catch (IOException e) {
                                            return Future.failedFuture(e);
                                        }
                                    })
                    ).<Peer>compose(peer -> Future.future(f -> peerService.isIPConflict(peer.getType(),
                            peer.getIpv4(),
                            peer.getIpv6(),
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
                            }))
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
                                provisionPeer(vertx, nodeService, peer).onComplete(ar ->
                                        this.handleProvisionResult(peerService, peer, ar));
                            })
                            .onFailure(err -> {
                                if (err instanceof HTTPException) {
                                    ctx.response().setStatusCode(((HTTPException) err).code).end();
                                } else if (err instanceof FormException) {
                                    renderForm(engine, nodeService,
                                            true, asn,
                                            ((Peer) ((FormException) err).data),
                                            Arrays.asList(((FormException) err).errors),
                                            res -> {
                                                if (res.succeeded()) {
                                                    ctx.response().end(res.result());
                                                } else {
                                                    ctx.fail(res.cause());
                                                }
                                            });
                                } else {
                                    logger.error("Cannot add peer.", err);
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
                    final String asn = ctx.user().principal().getString("username");
                    final String id = ctx.<RequestParameters>get(ValidationHandler.REQUEST_CONTEXT_KEY)
                            .queryParameter("id").getString();
                    Future.<Peer>future(f -> peerService.getSingle(asn, id, f))
                            .compose(peer -> {
                                if (peer == null) {
                                    return Future.failedFuture(new HTTPException(404));
                                }
                                return Future.succeededFuture(peer);
                            })
                            .<Buffer>compose(peer -> Future.future(f -> renderForm(engine, nodeService, false,
                                    asn, peer, null, f)))
                            .onComplete(res -> {
                                if (res.succeeded()) {
                                    ctx.response().end(res.result());
                                } else {
                                    res.cause().printStackTrace();
                                    ctx.fail(res.cause());
                                }
                            });
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
                    final String asn = ctx.user().principal().getString("username");
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
                                return Future.<WhoisObject>future(f -> whoisService.query(inPeer.getIpv4(), f))
                                        .compose(whoisObject -> {
                                            if (whoisObject == null ||
                                                    !whoisObject.containsKey("origin") ||
                                                    !whoisObject.get("origin").contains(asn)) {
                                                return Future.failedFuture(new FormException(inPeer,
                                                        "The IPv4 address you specified does not have a route with your ASN."));
                                            }
                                            // Verify IPv6
                                            try {
                                                if (inPeer.getIpv6() != null && !inPeer.isIPv6LinkLocal()) {
                                                    return Future.<WhoisObject>future(f -> whoisService.query(inPeer.getIpv6(), f))
                                                            .compose(ipv6Whois -> {
                                                                if (ipv6Whois == null ||
                                                                        !ipv6Whois.containsKey("origin") ||
                                                                        !ipv6Whois.get("origin").contains(asn)) {
                                                                    return Future.failedFuture(new FormException(inPeer,
                                                                            "The IPv6 address you specified does not have a route with your ASN."));
                                                                } else {
                                                                    return Future.succeededFuture(peer);
                                                                }
                                                            });
                                                } else {
                                                    // Do not check for IPv6 address.
                                                    return Future.succeededFuture(peer);
                                                }
                                            } catch (IOException e) {
                                                return Future.failedFuture(e);
                                            }
                                        });
                            }
                    ).<Pair<Peer /* Existing */, Peer /* Input */>>compose(peer -> {
                                final Peer existingPeer = peer.a;
                                final Peer inPeer = peer.b;
                                boolean needCheckIPv4Conflict;
                                boolean needCheckIPv6Conflict;

                                if (existingPeer.getType() != inPeer.getType()) {
                                    needCheckIPv4Conflict = true;
                                    needCheckIPv6Conflict = true;
                                } else {
                                    needCheckIPv4Conflict =
                                            !Objects.equals(existingPeer.getIpv4(), inPeer.getIpv4());
                                    needCheckIPv6Conflict =
                                            !Objects.equals(existingPeer.getIpv6(), inPeer.getIpv6());
                                    if (inPeer.getIpv6() == null) needCheckIPv6Conflict = false;
                                }
                                final boolean nc6 = needCheckIPv6Conflict;
                                return Future.future(f -> peerService.isIPConflict(inPeer.getType(),
                                        needCheckIPv4Conflict ? inPeer.getIpv4() : null,
                                        nc6 ? inPeer.getIpv6() : null,
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
                                reloadPeer(vertx, nodeService, existingPeer, inPeer).onComplete(ar ->
                                        this.handleProvisionResult(peerService, inPeer, ar));
                            })
                            .onFailure(err -> {
                                if (err instanceof HTTPException) {
                                    ctx.response().setStatusCode(((HTTPException) err).code).end();
                                } else if (err instanceof FormException) {
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
                                            res -> {
                                                if (res.succeeded()) {
                                                    ctx.response().end(res.result());
                                                } else {
                                                    ctx.fail(res.cause());
                                                }
                                            });
                                } else {
                                    logger.error("Cannot edit peer.", err);
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
                    final String asn = ctx.user().principal().getString("username");
                    final String id = ctx.<RequestParameters>get(ValidationHandler.REQUEST_CONTEXT_KEY)
                            .queryParameter("id").getString();
                    Future.<Peer>future(f -> peerService.getSingle(asn, id, f))
                            .compose(peer -> {
                                if (peer == null) {
                                    return Future.failedFuture(new HTTPException(404));
                                }
                                return Future.succeededFuture(peer);
                            })
                            .compose(peer -> unprovisionPeer(vertx, nodeService, peer))
                            .compose(_v -> Future.<Void>future(f -> peerService.deletePeer(asn, id, f)))
                            .onSuccess(_id -> ctx.response()
                                    .setStatusCode(303)
                                    .putHeader("Location", "/manage")
                                    .end())
                            .onFailure(err -> {
                                if (err instanceof HTTPException) {
                                    ctx.response().setStatusCode(((HTTPException) err).code).end();
                                } else {
                                    logger.error("Cannot delete peer.", err);
                                    ctx.fail(err);
                                }
                            });
                });

        router.get("/change-password")
                .produces("text/html")
                .handler(ctx -> {
                    final String asn = ctx.user().principal().getString("username");
                    renderChangepw(engine, asn, null, res -> {
                        if (res.succeeded()) {
                            ctx.response().end(res.result());
                        } else {
                            res.cause().printStackTrace();
                            ctx.fail(res.cause());
                        }
                    });
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
                    final String asn = ctx.user().principal().getString("username");
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
                                if (err instanceof HTTPException) {
                                    ctx.response().setStatusCode(((HTTPException) err).code).end();
                                } else if (err instanceof FormException) {
                                    renderChangepw(engine, asn,
                                            Arrays.asList(((FormException) err).errors),
                                            res -> {
                                                if (res.succeeded()) {
                                                    ctx.response().end(res.result());
                                                } else {
                                                    res.cause().printStackTrace();
                                                    ctx.fail(res.cause());
                                                }
                                            });
                                } else {
                                    logger.error("Cannot change password.", err);
                                    ctx.fail(err);
                                }
                            });
                });

        router.get("/delete-account")
                .produces("text/html")
                .handler(ctx -> {
                    final String asn = ctx.user().principal().getString("username");
                    renderDA(engine, asn, null, res -> {
                        if (res.succeeded()) {
                            ctx.response().end(res.result());
                        } else {
                            res.cause().printStackTrace();
                            ctx.fail(res.cause());
                        }
                    });
                });

        router.post("/delete-account")
                .produces("text/html")
                .handler(ctx -> {
                    final String asn = ctx.user().principal().getString("username");
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
                                if (err instanceof HTTPException) {
                                    ctx.response().setStatusCode(((HTTPException) err).code).end();
                                } else if (err instanceof FormException) {
                                    renderDA(engine, asn,
                                            Arrays.asList(((FormException) err).errors),
                                            res -> {
                                                if (res.succeeded()) {
                                                    ctx.response().end(res.result());
                                                } else {
                                                    res.cause().printStackTrace();
                                                    ctx.fail(res.cause());
                                                }
                                            });
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
                    final String asn = ctx.user().principal().getString("username");
                    final String id = ctx.<RequestParameters>get(ValidationHandler.REQUEST_CONTEXT_KEY)
                            .queryParameter("id").getString();
                    Future.<Peer>future(f -> peerService.getSingle(asn, id, f))
                            .compose(peer -> {
                                if (peer == null) {
                                    return Future.failedFuture(new HTTPException(404));
                                }
                                return Future.succeededFuture(peer);
                            })
                            .compose(peer -> renderShowConfig(nodeService, engine, peer))
                            .onComplete(res -> {
                                if (res.succeeded()) {
                                    ctx.response().end(res.result());
                                } else {
                                    res.cause().printStackTrace();
                                    ctx.fail(res.cause());
                                }
                            });
                });

        return router;
    }

    @Nonnull
    private Future<Peer> parseForm(@Nonnull INodeService nodeService,
                           @Nonnull JsonObject form) {
        // Parse form
        int nodeId = -1;
        if (form.containsKey("node")) {
            try {
                nodeId = Integer.parseInt(form.getString("node"));
            } catch (NumberFormatException ignored) {
            }
            if(nodeId == -1) {
                return Future.failedFuture(new FormException("The node selection is invalid."));
            }
        }

        int n = nodeId;
        return Future.<Node>future(f -> nodeService.getNode(n, f))
                .compose(node -> {
                    try {
                        final List<String> errors = new ArrayList<>(10);
                        if(node == null) {
                            errors.add("The node selection is invalid.");
                        }
                        Peer.VPNType type = null;
                        if (form.containsKey("vpn")) {
                            final String rawVPN = form.getString("vpn");
                            if (rawVPN == null) {
                                errors.add("Tunneling type is not specified.");
                            } else
                                switch (rawVPN) {
                                    case "wg":
                                        type = Peer.VPNType.WIREGUARD;
                                        break;
                                    default:
                                        errors.add("Tunneling type is unexpected.");
                                        break;
                                }
                        } else {
                            errors.add("Tunneling type is not specified.");
                        }

                        String ipv4 = null;
                        if (form.containsKey("ipv4")) {
                            ipv4 = form.getString("ipv4");
                            if (ipv4 == null || ipv4.isEmpty()) {
                                errors.add("IPv4 address is not specified.");
                                ipv4 = null; // Non-null but empty values could cause problems.
                            } else {
                                if (InetAddressValidator.getInstance().isValidInet4Address(ipv4)) {
                                    if (!new CIDRUtils("172.20.0.0/14").isInRange(ipv4)) {
                                        errors.add("IPv4 address is illegal. It must be a dn42 IPv4 address (172.20.x.x to 172.23.x.x).");
                                    }
                                } else
                                    errors.add("IPv4 address is illegal. Cannot parse your address.");
                            }
                        } else {
                            errors.add("IPv4 address is not specified.");
                        }

                        String ipv6 = null;
                        if (form.containsKey("ipv6")) {
                            ipv6 = form.getString("ipv6");
                            if (ipv6 != null && !ipv6.isEmpty()) {
                                if (InetAddressValidator.getInstance().isValidInet6Address(ipv6)) {
                                    if (!new CIDRUtils("fd00::/8").isInRange(ipv6) &&
                                            !Inet6Address.getByName(ipv6).isLinkLocalAddress()) {
                                        errors.add("IPv6 address is illegal. It must be a dn42 or link-local IPv6 address.");
                                    }
                                } else
                                    errors.add("IPv6 address is illegal. Cannot parse your address.");
                            } else {
                                ipv6 = null; // Non-null but empty values could cause problems.
                            }
                        }

                        boolean mpbgp = false;
                        if (form.containsKey("mpbgp")) {
                            if (ipv6 == null) {
                                errors.add("MP-BGP cannot be enabled if you do not have a valid IPv6 address.");
                            } else {
                                mpbgp = true;
                            }
                        }

                        String wgEndpoint = null;
                        boolean wgEndpointCorrect = false;
                        if (form.containsKey("wg_endpoint")) {
                            if (type == Peer.VPNType.WIREGUARD) {
                                wgEndpoint = form.getString("wg_endpoint");
                                if (wgEndpoint != null && !wgEndpoint.isEmpty()) {
                                    if (InetAddressValidator.getInstance().isValidInet4Address(wgEndpoint)) {
                                        if (new CIDRUtils("10.0.0.0/8").isInRange(wgEndpoint) ||
                                                new CIDRUtils("192.168.0.0/16").isInRange(wgEndpoint) ||
                                                new CIDRUtils("172.16.0.0/23").isInRange(wgEndpoint)) {
                                            errors.add("WireGuard EndPoint is illegal. It must not be an internal address.");
                                        } else {
                                            wgEndpointCorrect = true;
                                        }
                                    } else
                                        errors.add("WireGuard EndPoint is illegal. Cannot parse your address.");
                                } else {
                                    wgEndpoint = null; // Non-null but empty values could cause problems.
                                }
                            } else {
                                errors.add("WireGuard tunneling is not selected but WireGuard Endpoint configuration appears.");
                            }
                        }

                        Integer wgEndpointPort = null;
                        if (form.containsKey("wg_endpoint_port")) {
                            if (type == Peer.VPNType.WIREGUARD) {
                                final String rawPort = form.getString("wg_endpoint_port");
                                if(rawPort != null && !rawPort.isEmpty()) {
                                    if (wgEndpointCorrect) {
                                        try {
                                            wgEndpointPort = Integer.parseInt(rawPort);
                                            if (wgEndpointPort < 0 || wgEndpointPort > 65535) {
                                                errors.add("WireGuard EndPoint port must be in UDP port range.");
                                            }
                                        } catch (NumberFormatException | NullPointerException ignored) {
                                            errors.add("WireGuard EndPoint port is not valid. It must be a number.");
                                        }
                                    } else {
                                        errors.add("WireGuard EndPoint IP is not specified or invalid, but port is specified.");
                                    }
                                }
                            } else {
                                errors.add("WireGuard tunneling is not selected but WireGuard Endpoint configuration appears.");
                            }
                        }

                        // When user specified the endpoint without the port.
                        if(type == Peer.VPNType.WIREGUARD &&
                                wgEndpointCorrect &&
                                wgEndpointPort == null) {
                            errors.add("WireGuard EndPoint IP is specified, but the port is missing.");
                        }

                        String wgPubKey = null;
                        if (form.containsKey("wg_pubkey")) {
                            if (type == Peer.VPNType.WIREGUARD) {
                                wgPubKey = form.getString("wg_pubkey");
                                if (wgPubKey == null || wgPubKey.isEmpty()) {
                                    errors.add("WireGuard public key is not specified.");
                                    wgPubKey = null; // Non-null but empty values could cause problems.
                                } else {
                                    try {
                                        Key.fromBase64(wgPubKey);
                                    } catch (KeyFormatException e) {
                                        errors.add("WireGuard public key is not valid.");
                                    }
                                }
                            } else {
                                errors.add("WireGuard tunneling is not selected but WireGuard public key appears.");
                            }
                        } else {
                            if (type == Peer.VPNType.WIREGUARD) {
                                errors.add("WireGuard public key is not specified.");
                            }
                        }

                        if(node != null && !node.getSupportedVPNTypes().contains(type)) {
                            errors.add(String.format("Node %s does not support VPN type %s.", node.getName(),
                                    type));
                        }

                        Peer peer;
                        if (type == Peer.VPNType.WIREGUARD) {
                            peer = new Peer(ipv4, ipv6, wgEndpoint, wgEndpointPort, wgPubKey, mpbgp, n);
                        } else {
                            peer = new Peer(
                                    -1,
                                    type,
                                    null, /* ASN: To be filled later */
                                    ipv4,
                                    ipv6,
                                    wgEndpoint,
                                    wgEndpointPort,
                                    null, /* Self public key: Generate later if needed */
                                    null, /* Self private key: Generate later if needed */
                                    wgPubKey,
                                    null /* Preshared Secret: Generate later if needed */,
                                    ProvisionStatus.NOT_PROVISIONED,
                                    mpbgp,
                                    n
                            );
                        }
                        if(errors.isEmpty()) {
                            return Future.succeededFuture(peer);
                        } else {
                            return Future.failedFuture(new FormException(peer, errors.toArray(new String[]{})));
                        }
                    } catch (IOException e) {
                        return Future.failedFuture(e);
                    }
                });
    }

    private void renderIndex(@Nonnull TemplateEngine engine,
                             @Nonnull String asn, @Nonnull List<Peer> peers,
                             @Nonnull Handler<AsyncResult<Buffer>> handler) {
        final Map<String, Object> root = new HashMap<>();
        root.put("asn", asn);
        root.put("peers", peers.stream()
                .map(peer -> {
                    final Map<String, Object> map = new HashMap<>();
                    map.put("id", peer.getId());
                    map.put("ipv4", peer.getIpv4());
                    map.put("ipv6", peer.getIpv6());
                    map.put("type", peer.getType());
                    map.put("provisionStatus", peer.getProvisionStatus());
                    return map;
                })
                .collect(Collectors.toList()));
        engine.render(root, "manage/index.ftlh", handler);
    }

    @SuppressWarnings("unchecked")
    private void renderForm(@Nonnull TemplateEngine engine,
                            @Nonnull INodeService nodeService,
                            boolean newForm,
                            @Nonnull String asn, @Nullable Peer peer, @Nullable List<String> errors,
                            @Nonnull Handler<AsyncResult<Buffer>> handler) {
        Future.future(nodeService::listNodes)
                .compose(list -> {
                    final Map<String, Object> root = new HashMap<>();
                    root.put("asn", asn);
                    root.put("nodes", list.stream()
                            .map(node -> {
                                final Map<String, Object> map = new HashMap<>(10);
                                map.put("id", node.getId());
                                map.put("name", node.getName());
                                map.put("public_ip", node.getPublicIp());
                                map.put("notice", node.getNotice());
                                map.put("asn", node.getAsn());
                                map.put("vpn_types", node.getSupportedVPNTypes());
                                return map;
                            })
                            .collect(Collectors.toList()));
                    if (peer != null) {
                        root.put("ipv4", peer.getIpv4());
                        root.put("ipv6", peer.getIpv6());
                        switch (peer.getType()) {
                            case WIREGUARD:
                                root.put("typeWireguard", true);
                                break;
                        }
                        root.put("wgEndpoint", peer.getWgEndpoint());
                        root.put("wgEndpointPort", peer.getWgEndpointPort());
                        root.put("wgPubkey", peer.getWgPeerPubkey());
                        root.put("mpbgp", peer.isMpbgp());
                        root.put("node_checked", peer.getNode());
                        root.put("id", peer.getId());
                    } else {
                        root.put("typeWireguard", true);
                        root.put("mpbgp", false);
                        root.put("node_checked", ((List<Map<String, Object>>)root.get("nodes")).get(0).get("id"));
                    }
                    if(!newForm && peer != null)
                        root.put("action", "/manage/edit?id=" + peer.getId());
                    else
                        root.put("action", "/manage/new");
                    root.put("errors", errors);
                    return engine.render(root, newForm ? "manage/new.ftlh" : "manage/edit.ftlh");
                })
                .onComplete(handler);
    }

    private void renderChangepw(@Nonnull TemplateEngine engine,
                                @Nonnull String asn, @Nullable List<String> errors,
                                @Nonnull Handler<AsyncResult<Buffer>> handler) {
        final Map<String, Object> root = new HashMap<>();
        root.put("asn", asn);
        root.put("errors", errors);
        engine.render(root, "manage/changepw.ftlh", handler);
    }

    private void renderDA(@Nonnull TemplateEngine engine,
                          @Nonnull String asn, @Nullable List<String> errors,
                          @Nonnull Handler<AsyncResult<Buffer>> handler) {
        final Map<String, Object> root = new HashMap<>();
        root.put("asn", asn);
        root.put("errors", errors);
        engine.render(root, "manage/delete.ftlh", handler);
    }

    @Nonnull
    private Future<Buffer> renderShowConfig(@Nonnull INodeService nodeService,
                                  @Nonnull TemplateEngine engine,
                                  @Nonnull Peer peer) {
        return Future.<Node>future(f -> nodeService.getNode(peer.getNode(), f))
                .compose(node -> {
                    final Map<String, Object> root = new HashMap<>();
                    root.put("ipv4", peer.getIpv4());
                    root.put("ipv6", peer.getIpv6());
                    switch (peer.getType()) {
                        case WIREGUARD:
                            root.put("typeWireguard", true);
                            break;
                    }
                    root.put("wgPort", peer.calcWireGuardPort());
                    root.put("wgEndpoint", peer.getWgEndpoint());
                    root.put("wgEndpointPort", peer.getWgEndpointPort());
                    root.put("wgPresharedSecret", peer.getWgPresharedSecret());
                    root.put("wgSelfPubkey", peer.getWgSelfPubkey());
                    root.put("mpbgp", peer.isMpbgp());

                    if(node == null) {
                        root.put("ipv4", "This node is currently down! Edit the peer to choose another one.");
                        root.put("ipv6", "This node is currently down! Edit the peer to choose another one.");
                        root.put("asn", "This node is currently down! Edit the peer to choose another one.");
                        root.put("endpoint", "This node is currently down! Edit the peer to choose another one.");
                    } else {
                        root.put("ipv4", node.getDn42Ip4());
                        try {
                            if(peer.isIPv6LinkLocal()) {
                                root.put("ipv6", node.getDn42Ip6());
                            } else {
                                root.put("ipv6", node.getDn42Ip6NonLL());
                            }
                        } catch (IOException e) {
                            return Future.failedFuture(e);
                        }
                        root.put("asn", node.getAsn());
                        root.put("endpoint", node.getPublicIp());
                    }

                    return engine.render(root, "manage/showconf.ftlh");
                });

    }

    @Nonnull
    private Future<Void> reloadPeer(@Nonnull Vertx vertx,
                                    @Nonnull INodeService nodeService,
                                    @Nonnull Peer existingPeer, @Nonnull Peer inPeer) {
        // Check if we can reload on the fly.
        // Otherwise, we can only deprovision and provision.
        // This will cause unnecessary wastes.
        boolean canReload = inPeer.getType() == existingPeer.getType() &&
                inPeer.getNode() == existingPeer.getNode();
        // wg-quick does not support switching local IP addresses.
        // However, switch between link local addresses and real IPv6 addresses require the change of
        // local v6 address. Therefore, in such cases, we have to do a full re-provision.
        if(canReload && // Only check if no other factors prevent us from reloading.
                inPeer.getType() == Peer.VPNType.WIREGUARD &&
                existingPeer.getType() == Peer.VPNType.WIREGUARD) {
            try {
                final boolean existingLL = existingPeer.isIPv6LinkLocal();
                final boolean newLL = inPeer.isIPv6LinkLocal();
                if(existingLL != newLL) {
                    canReload = false;
                }
            } catch (IOException e) {
                return Future.failedFuture(e);
            }
        }
        Future<Void> future;
        if (canReload) {
            future = Future.<Node>future(f -> nodeService.getNode(inPeer.getNode(), f))
                    .compose(node -> {
                        if(node == null || !node.getSupportedVPNTypes().contains(inPeer.getType())) {
                            return Future.failedFuture("The node does not exist");
                        }
                        return Future.succeededFuture(node);
                    })
                    .compose(node -> {
                        final VertxAgentGrpc.AgentVertxStub stub = VertxAgentGrpc.newVertxStub(node.toChannel(vertx));
                        switch (existingPeer.getType()) {
                            case WIREGUARD:
                                return stub.reloadWG(
                                        inPeer.toWGRequest().setNode(node.toRPCNode()).build()
                                ).compose(wgReply -> Future.succeededFuture(new Pair<>(node, wgReply.getDevice())));
                            default:
                                throw new UnsupportedOperationException("Bug: Unknown type.");
                        }
                    })
                    .compose(pair -> {
                        final VertxAgentGrpc.AgentVertxStub stub = VertxAgentGrpc.newVertxStub(pair.a.toChannel(vertx));
                        return stub.reloadBGP(inPeer.toBGPRequest()
                                .setNode(pair.a.toRPCNode())
                                .setDevice(pair.b)
                                .build())
                                .compose(reply -> Future.succeededFuture(null));
                    });
        } else {
            future = unprovisionPeer(vertx, nodeService, existingPeer)
                    .compose(f -> provisionPeer(vertx, nodeService, inPeer));
        }
        return future;
    }

    private Future<Void> unprovisionPeer(@Nonnull Vertx vertx,
                                         @Nonnull INodeService nodeService,
                                         @Nonnull Peer existingPeer) {
        return Future.<Node>future(f -> nodeService.getNode(existingPeer.getNode(), f))
                .compose(node -> {
                    if(node == null) {
                        return Future.failedFuture("The node does not exist");
                    }
                    return Future.succeededFuture(node);
                })
                .compose(node -> {
                    final VertxAgentGrpc.AgentVertxStub stub = VertxAgentGrpc.newVertxStub(node.toChannel(vertx));
                    switch (existingPeer.getType()) {
                        case WIREGUARD:
                            return stub.deleteWG(WGRequest.newBuilder().setId(existingPeer.getId()).build())
                                    .compose(wgReply -> Future.succeededFuture(node));
                        default:
                            throw new UnsupportedOperationException("Bug: Unknown type.");
                    }
                })
                .compose(node -> {
                    final VertxAgentGrpc.AgentVertxStub stub = VertxAgentGrpc.newVertxStub(node.toChannel(vertx));
                    return stub.deleteBGP(BGPRequest.newBuilder().setId(existingPeer.getId())
                            .build())
                            .compose(reply -> Future.succeededFuture(null));
                })
        ;
    }

    @Nonnull
    private Future<Void> provisionPeer(@Nonnull Vertx vertx,
                               @Nonnull INodeService nodeService,
                               @Nonnull Peer inPeer) {
        return Future.<Node>future(f -> nodeService.getNode(inPeer.getNode(), f))
                .compose(node -> {
                    if(node == null || !node.getSupportedVPNTypes().contains(inPeer.getType())) {
                        return Future.failedFuture("The node does not exist");
                    }
                    return Future.succeededFuture(node);
                })
                .compose(node -> {
                    final VertxAgentGrpc.AgentVertxStub stub = VertxAgentGrpc.newVertxStub(node.toChannel(vertx));
                    switch (inPeer.getType()) {
                        case WIREGUARD:
                            return stub.provisionWG(
                                    inPeer.toWGRequest().setNode(node.toRPCNode()).build()
                            ).compose(wgReply -> Future.succeededFuture(new Pair<>(node, wgReply.getDevice())));
                        default:
                            throw new UnsupportedOperationException("Bug: Unknown type.");
                    }
                })
                .compose(pair -> {
                    final VertxAgentGrpc.AgentVertxStub stub = VertxAgentGrpc.newVertxStub(pair.a.toChannel(vertx));
                    return stub.provisionBGP(inPeer.toBGPRequest()
                            .setNode(pair.a.toRPCNode())
                            .setDevice(pair.b)
                            .build())
                            .compose(reply -> Future.succeededFuture(null));
                });
    }

    private void handleProvisionResult(@Nonnull IPeerService peerService,
                                       @Nonnull Peer inPeer,
                                       @Nonnull AsyncResult<Void> res) {
        if(res.succeeded()) {
            peerService.changeProvisionStatus(inPeer.getId(),
                    ProvisionStatus.PROVISIONED, ar -> {
                        if (ar.failed()) {
                            logger.error(String.format("Cannot update %d to provisioned.", inPeer.getId()), ar.cause());
                        }
                    });
        } else {
            logger.error(String.format("Cannot provision %d.", inPeer.getId()), res.cause());
            peerService.changeProvisionStatus(inPeer.getId(),
                    ProvisionStatus.FAIL, ar -> {
                        if (ar.failed()) {
                            logger.error(String.format("Cannot update %d to failed.", inPeer.getId()), ar.cause());
                        }
                    });
        }
    }
}
