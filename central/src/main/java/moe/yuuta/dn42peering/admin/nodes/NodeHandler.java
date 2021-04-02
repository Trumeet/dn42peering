package moe.yuuta.dn42peering.admin.nodes;

import io.vertx.core.Future;
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
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.serviceproxy.ServiceException;
import moe.yuuta.dn42peering.asn.IASNService;
import moe.yuuta.dn42peering.jaba.Pair;
import moe.yuuta.dn42peering.node.DuplicateNodeException;
import moe.yuuta.dn42peering.node.INodeService;
import moe.yuuta.dn42peering.node.Node;
import moe.yuuta.dn42peering.peer.IPeerService;
import moe.yuuta.dn42peering.portal.FormException;
import moe.yuuta.dn42peering.portal.HTTPException;
import moe.yuuta.dn42peering.portal.ISubRouter;
import moe.yuuta.dn42peering.provision.IProvisionRemoteService;

import javax.annotation.Nonnull;
import java.util.Arrays;

import static io.vertx.ext.web.validation.builder.Parameters.param;
import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;

public class NodeHandler implements ISubRouter {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    @Nonnull
    @Override
    public Router mount(@Nonnull Vertx vertx) {
        final IASNService asnService = IASNService.createProxy(vertx, IASNService.ADDRESS);
        final INodeService nodeService = INodeService.createProxy(vertx);
        final IPeerService peerService = IPeerService.createProxy(vertx);
        final IProvisionRemoteService provisionRemoteService = IProvisionRemoteService.create(vertx);
        final TemplateEngine engine = FreeMarkerTemplateEngine.create(vertx, "ftlh");
        final SchemaParser parser = SchemaParser.createDraft7SchemaParser(
                SchemaRouter.create(vertx, new SchemaRouterOptions()));

        final ObjectSchemaBuilder nodeSchema = objectSchema()
                .allowAdditionalProperties(false)
                .property("asn", stringSchema())
                .property("name", stringSchema())
                .property("notice", stringSchema())
                .property("ipv4", stringSchema())
                .property("ipv6", stringSchema())
                .property("ipv6_non_ll", stringSchema())
                .property("public_ip", stringSchema())
                .property("internal_ip", stringSchema())
                .property("internal_port", stringSchema())
                .property("tunneling_method_wireguard", stringSchema());

        final Router router = Router.router(vertx);
        router.post().handler(BodyHandler.create().setBodyLimit(100 * 1024));

        router.get("/new")
                .produces("text/html")
                .handler(ctx -> NodeAdminUI.renderForm(engine,
                        nodeService,
                        ctx.user().principal().getString("username"),
                        true,
                        null,
                        null,
                        ctx));

        router.post("/new")
                .handler(BodyHandler.create().setBodyLimit(100 * 1024))
                .handler(ValidationHandler
                        .builder(parser)
                        .body(Bodies.formUrlEncoded(nodeSchema))
                        .predicate(RequestPredicate.BODY_REQUIRED)
                        .build())
                .handler(ctx -> {
                    final String asn = ctx.user().principal().getString("username");
                    final JsonObject parameters = ctx.<RequestParameters>get(ValidationHandler.REQUEST_CONTEXT_KEY)
                            .body().getJsonObject();
                    NodeAdminUI.parseForm(nodeService, asn, parameters)
                            .<Node>compose(node -> Future.future(f -> nodeService.addNew(node, ar -> {
                                if (ar.succeeded()) {
                                    node.setId((int) (long) ar.result());
                                    f.complete(node);
                                } else {
                                    if(((ServiceException)ar.cause()).getDebugInfo().getString("causeName")
                                    .equals(DuplicateNodeException.class.getName())) {
                                        f.fail(new FormException(node, "A node with your given public IP already exists."));
                                        return;
                                    }
                                    f.fail(ar.cause());
                                }
                            })))
                            .onSuccess(peer -> {
                                ctx.response()
                                        .setStatusCode(303)
                                        .putHeader("Location", "/admin")
                                        .end();
                            })
                            .onFailure(err -> {
                                if (err instanceof FormException) {
                                    NodeAdminUI.renderForm(engine, nodeService,
                                            asn,
                                            true,
                                            ((Node) ((FormException) err).data),
                                            Arrays.asList(((FormException) err).errors),
                                            ctx);
                                } else {
                                    if (!(err instanceof HTTPException)) logger.error("Cannot add node.", err);
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
                    int intId;
                    try {
                        intId = Integer.parseInt(id);
                    } catch (NumberFormatException ignored) {
                        ctx.fail(new HTTPException(400));
                        return;
                    }
                    Future.<Node>future(f -> nodeService.getNode(intId, f))
                            .compose(node -> {
                                if (node == null) {
                                    return Future.failedFuture(new HTTPException(400));
                                } else {
                                    return Future.succeededFuture(node);
                                }
                            })
                            .onSuccess(node ->
                                    NodeAdminUI.renderForm(
                                            engine,
                                            nodeService,
                                            asn,
                                            false,
                                            node,
                                            null,
                                            ctx))
                            .onFailure(ctx::fail);
                });

        router.post("/edit")
                .handler(BodyHandler.create().setBodyLimit(100 * 1024))
                .handler(ValidationHandler
                        .builder(parser)
                        .body(Bodies.formUrlEncoded(nodeSchema))
                        .queryParameter(param("id", stringSchema()))
                        .predicate(RequestPredicate.BODY_REQUIRED)
                        .build())
                .handler(ctx -> {
                    final String asn = ctx.user().principal().getString("username");
                    final JsonObject parameters = ctx.<RequestParameters>get(ValidationHandler.REQUEST_CONTEXT_KEY)
                            .body().getJsonObject();
                    final String id = ctx.<RequestParameters>get(ValidationHandler.REQUEST_CONTEXT_KEY)
                            .queryParameter("id").getString();
                    int intId;
                    try {
                        intId = Integer.parseInt(id);
                    } catch (NumberFormatException ignored) {
                        ctx.response().setStatusCode(400).end();
                        return;
                    }
                    Future.<Node>future(f -> nodeService.getNode(intId, f))
                            .compose(node -> {
                                if(node == null) {
                                    return Future.failedFuture(new HTTPException(404));
                                }
                                return Future.succeededFuture(node);
                            })
                            .compose(existingNode -> {
                                return NodeAdminUI.parseForm(nodeService, asn, parameters)
                                        .compose(newNode -> {
                                            newNode.setId(existingNode.getId());
                                            return Future.succeededFuture(new Pair<>(existingNode, newNode));
                                        });
                            })
                            .<Node>compose(node -> Future.future(f -> nodeService.updateTo(node.b, ar -> {
                                if (ar.succeeded()) {
                                    f.complete(node.b);
                                } else {
                                    if(((ServiceException)ar.cause()).getDebugInfo().getString("causeName")
                                            .equals(DuplicateNodeException.class.getName())) {
                                        f.fail(new FormException(node.b, "A node with your given public IP already exists."));
                                        return;
                                    }
                                    f.fail(ar.cause());
                                }
                            })))
                            .onSuccess(node -> {
                                ctx.response()
                                        .setStatusCode(303)
                                        .putHeader("Location", "/admin")
                                        .end();
                                provisionRemoteService.deploy(node.getId(), ar -> {});
                            })
                            .onFailure(err -> {
                                if (err instanceof FormException) {
                                    NodeAdminUI.renderForm(engine, nodeService,
                                            asn,
                                            true,
                                            ((Node) ((FormException) err).data),
                                            Arrays.asList(((FormException) err).errors),
                                            ctx);
                                } else {
                                    if (!(err instanceof HTTPException)) logger.error("Cannot add node.", err);
                                    ctx.fail(err);
                                }
                            });
                });

        router.get("/delete")
                .handler(BodyHandler.create().setBodyLimit(100 * 1024))
                .handler(ValidationHandler
                        .builder(parser)
                        .queryParameter(param("id", stringSchema()))
                        .build())
                .handler(ctx -> {
                    final String id = ctx.<RequestParameters>get(ValidationHandler.REQUEST_CONTEXT_KEY)
                            .queryParameter("id").getString();
                    int intId;
                    try {
                        intId = Integer.parseInt(id);
                    } catch (NumberFormatException ignored) {
                        ctx.response().setStatusCode(400).end();
                        return;
                    }
                    nodeService.delete(intId, ar -> {
                        if(ar.succeeded()) {
                            ctx.response()
                                    .setStatusCode(303)
                                    .putHeader("Location", "/admin")
                                    .end();
                        } else {
                            logger.error("Cannot delete node " + intId, ar.cause());
                            ctx.fail(ar.cause());
                        }
                    });
                });

        return router;
    }
}
