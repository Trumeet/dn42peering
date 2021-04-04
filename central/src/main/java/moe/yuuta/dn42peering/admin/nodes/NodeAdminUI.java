package moe.yuuta.dn42peering.admin.nodes;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.common.template.TemplateEngine;
import moe.yuuta.dn42peering.RPC;
import moe.yuuta.dn42peering.node.INodeService;
import moe.yuuta.dn42peering.node.Node;
import moe.yuuta.dn42peering.portal.FormException;
import moe.yuuta.dn42peering.portal.RenderingUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class NodeAdminUI {
    public static void renderForm(@Nonnull TemplateEngine engine,
                                  @Nonnull INodeService nodeService,
                                  @Nonnull String asn,
                                  boolean newForm,
                                  @Nullable Node node,
                                  @Nullable List<String> errors,
                                  @Nonnull RoutingContext ctx) {
        final Map<String, Object> root = new HashMap<>();
        root.put("asn", asn);
        root.put("tunneling_method_wireguard", true);
        if (node != null) {
            root.put("id", node.getId());
            root.put("input_asn", node.getAsn());
            root.put("name", node.getName());
            root.put("ipv4", node.getDn42Ip4());
            root.put("ipv6", node.getDn42Ip6());
            root.put("ipv6_non_ll", node.getDn42Ip6NonLL());
            root.put("public_ip", node.getPublicIp());
            root.put("internal_ip", node.getInternalIp());
            root.put("internal_port", node.getInternalPort());
            root.put("notice", node.getNotice());
            root.put("tunneling_method_wireguard", node.isWireguard());
        }
        if(!newForm && node != null)
            root.put("action", "/admin/nodes/edit?id=" + node.getId());
        else
            root.put("action", "/admin/nodes/new");
        root.put("errors", errors);
        engine.render(root, newForm ? "admin/nodes/new.ftlh" : "admin/nodes/edit.ftlh",
                RenderingUtils.getGeneralRenderingHandler(ctx));
    }

    @Nonnull
    public static Future<Node> parseForm(@Nonnull INodeService nodeService,
                                         @Nonnull String authAsn,
                                         @Nonnull JsonObject form) {
        final List<String> errors = new ArrayList<>(10);
        final Node node = new Node();
        node.setAsn(form.getString("asn", authAsn));
        node.setName(form.getString("name"));
        node.setNotice(form.getString("notice"));
        node.setDn42Ip4(form.getString("ipv4"));
        node.setDn42Ip6(form.getString("ipv6"));
        node.setDn42Ip6NonLL(form.getString("ipv6_non_ll"));
        node.setInternalIp(form.getString("internal_ip"));
        node.setPublicIp(form.getString("public_ip"));
        try {
            String raw = form.getString("internal_port");
            if(raw == null || raw.isEmpty()) {
                node.setInternalPort(RPC.AGENT_PORT);
            } else {
                node.setInternalPort(Integer.parseInt(raw));
            }
        } catch (NumberFormatException e) {
            errors.add("Invalid internal port");
        }
        node.setWireguard(form.containsKey("tunneling_method_wireguard"));

        if(!node.getAsn().matches("[aA][sS]424242[0-9][0-9][0-9][0-9]"))
            errors.add("ASN is invalid");
        if(node.getName() == null || node.getName().trim().isEmpty())
            errors.add("No name supplied");
        if(node.getDn42Ip4() != null) {
            final IPAddress address = new IPAddressString(node.getDn42Ip4()).getHostAddress();
            if (address != null) {
                if (!new IPAddressString("172.20.0.0/14").getAddress().contains(address)) {
                    errors.add("DN42 IPv4 address is illegal. It must be a dn42 IPv4 address (172.20.x.x to 172.23.x.x).");
                }
                // Remove prefix
                node.setDn42Ip4(address.toNormalizedString());
            } else
                errors.add("DN42 IPv4 address is illegal. Cannot parse your address.");
        } else {
            errors.add("DN42 IPv4 address is not supplied");
        }
        if(node.getDn42Ip6() != null) {
            final IPAddress address = new IPAddressString(node.getDn42Ip6()).getHostAddress();
            if (address != null) {
                if (!address.isLinkLocal()) {
                    errors.add("DN42 IPv6 address is illegal. It must be a link-local IPv6 address.");
                }
                // Compress & remove prefix
                node.setDn42Ip6(address.toCanonicalString());
            } else
                errors.add("DN42 Link Local IPv6 address is illegal. Cannot parse your address.");
        } else {
            errors.add("DN42 IPv6 Link Local address is not supplied");
        }
        if(node.getDn42Ip6NonLL() != null) {
            final IPAddress address = new IPAddressString(node.getDn42Ip6NonLL()).getHostAddress();
            if (address != null) {
                if (!new IPAddressString("fd00::/8").getAddress().contains(address) ||
                        address.isLinkLocal()) {
                    errors.add("DN42 IPv6 address is illegal. It must be a dn42 IPv6 address.");
                }
                // Compress & remove prefix
                node.setDn42Ip6NonLL(address.toCanonicalString());
            } else
                errors.add("IPv6 address is illegal. Cannot parse your address.");
        } else {
            errors.add("DN42 IPv6 address is not supplied");
        }

        if(node.getInternalIp() == null) {
            errors.add("Internal IP is not supplied.");
        }

        if(node.getInternalPort() < 0 ||
            node.getInternalPort() > 65535) {
            errors.add("Internal Port is out of range. Supported range: [0, 65535].");
        }

        if(node.getPublicIp() == null || node.getPublicIp().isEmpty()) {
            errors.add("Public IP is not supplied.");
        }

        if(errors.isEmpty()) {
            return Future.succeededFuture(node);
        } else {
            return Future.failedFuture(new FormException(node, errors.toArray(new String[]{})));
        }
    }
}
