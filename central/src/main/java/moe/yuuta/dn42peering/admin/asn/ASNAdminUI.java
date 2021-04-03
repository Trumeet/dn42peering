package moe.yuuta.dn42peering.admin.asn;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.common.template.TemplateEngine;
import moe.yuuta.dn42peering.asn.ASN;
import moe.yuuta.dn42peering.asn.IASNService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static moe.yuuta.dn42peering.portal.RenderingUtils.getGeneralRenderingHandler;

class ASNAdminUI {
    public static void renderIndex(@Nonnull String asn,
                                   @Nonnull TemplateEngine engine,
                                   @Nonnull IASNService asnService,
                                   @Nonnull RoutingContext ctx) {
        Future.<List<ASN>>future(asnService::list)
                .compose(asns -> {
                    final Map<String, Object> root = new HashMap<>();
                    root.put("asn", asn);
                    // Don't need to do that in async thread?
                    final JsonArray mapping = new JsonArray();
                    asns.stream().map(asnObj -> {
                        final JsonObject asnJson = new JsonObject();
                        asnJson.put("asn", asnObj.getAsn());
                        asnJson.put("activated", asnObj.isActivated());
                        return asnJson;
                    }).forEach(mapping::add);
                    root.put("asns", mapping);
                    return Future.succeededFuture(root);
                })
                .compose(json -> {
                    return engine.render(json, "admin/asn/index.ftlh");
                })
                .onComplete(getGeneralRenderingHandler(ctx));
    }

    public static void renderChangePassword(@Nonnull String asn,
                                   @Nullable String targetASN,
                                   @Nullable List<String> errors,
                                   @Nonnull TemplateEngine engine,
                                   @Nonnull RoutingContext ctx) {
        final Map<String, Object> root = new HashMap<>();
        root.put("asn", asn);
        root.put("target_asn", targetASN);
        root.put("errors", errors);
        engine.render(root, "admin/asn/changepw.ftlh")
                .onComplete(getGeneralRenderingHandler(ctx));
    }
}
