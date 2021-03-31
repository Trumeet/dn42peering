package moe.yuuta.dn42peering.admin;

import com.wireguard.crypto.Key;
import com.wireguard.crypto.KeyFormatException;
import edazdarevic.commons.net.CIDRUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.common.template.TemplateEngine;
import moe.yuuta.dn42peering.node.INodeService;
import moe.yuuta.dn42peering.node.Node;
import moe.yuuta.dn42peering.peer.Peer;
import moe.yuuta.dn42peering.peer.ProvisionStatus;
import moe.yuuta.dn42peering.portal.FormException;
import org.apache.commons.validator.routines.InetAddressValidator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.Inet6Address;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static moe.yuuta.dn42peering.portal.RenderingUtils.getGeneralRenderingHandler;

class AdminUI {
    public static void renderIndex(@Nonnull TemplateEngine engine,
                                   @Nonnull String asn,
                                   @Nonnull RoutingContext ctx) {
        final Map<String, Object> root = new HashMap<>();
        root.put("asn", asn);
        engine.render(root, "admin/index.ftlh", getGeneralRenderingHandler(ctx));
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
