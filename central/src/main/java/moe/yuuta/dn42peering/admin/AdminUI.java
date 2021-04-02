package moe.yuuta.dn42peering.admin;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.common.template.TemplateEngine;
import moe.yuuta.dn42peering.asn.IASNService;
import moe.yuuta.dn42peering.node.INodeService;
import moe.yuuta.dn42peering.peer.IPeerService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static moe.yuuta.dn42peering.portal.RenderingUtils.getGeneralRenderingHandler;

class AdminUI {
    public static void renderIndex(@Nonnull TemplateEngine engine,
                                   @Nonnull IASNService asnService,
                                   @Nonnull IPeerService peerService,
                                   @Nonnull INodeService nodeService,
                                   @Nonnull String asn,
                                   @Nonnull RoutingContext ctx) {
        Future.future(nodeService::listNodes)
                .compose(nodes -> {
                    final Map<String, Object> root = new HashMap<>();
                    root.put("asn", asn);
                    // Don't need to do that in async thread?
                    final JsonArray mapping = new JsonArray();
                    nodes.stream().map(node -> {
                        final JsonObject nodeJson = new JsonObject();
                        nodeJson.put("id", node.getId());
                        nodeJson.put("name", node.getName());
                        nodeJson.put("publicIp", node.getPublicIp());
                        nodeJson.put("dn42IP4", node.getDn42Ip4());
                        nodeJson.put("dn42IP6", node.getDn42Ip6());
                        nodeJson.put("dn42IP6NonLL", node.getDn42Ip6NonLL());
                        nodeJson.put("internalIP", node.getInternalIp());
                        nodeJson.put("internalPort", node.getInternalPort());
                        nodeJson.put("tunnelingMethods", node.getSupportedVPNTypes());
                        return nodeJson;
                    }).forEach(mapping::add);
                    root.put("nodes", mapping);
                    return Future.succeededFuture(root);
                })
                .compose(root -> {
                    return Future.future(asnService::count)
                            .compose(count -> {
                                root.put("asnTotal", count);
                                return Future.succeededFuture(root);
                            });
                })
                .compose(root -> {
                    return Future.future(asnService::count)
                            .compose(count -> {
                                root.put("peersTotal", count);
                                return Future.succeededFuture(root);
                            });
                })
                .compose(json -> {
                    return engine.render(json, "admin/index.ftlh");
                })
                .onComplete(getGeneralRenderingHandler(ctx));
    }

    public static void renderSudo(@Nonnull TemplateEngine engine,
                                  @Nonnull String asn,
                                  @Nullable List<String> errors,
                                  @Nullable String targetASN,
                                  @Nonnull RoutingContext ctx) {
        final Map<String, Object> root = new HashMap<>();
        root.put("asn", asn);
        root.put("errors", errors);
        root.put("target_asn", targetASN);
        engine.render(root, "admin/sudo.ftlh", getGeneralRenderingHandler(ctx));
    }
}
