package moe.yuuta.dn42peering.manage;

import com.wireguard.crypto.Key;
import com.wireguard.crypto.KeyFormatException;
import edazdarevic.commons.net.CIDRUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
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

class ManagementUI {
    private static Handler<AsyncResult<Buffer>> getGeneralRenderingHandler(@Nonnull RoutingContext ctx) {
        return res -> {
            if (res.succeeded()) {
                ctx.response()
                        .putHeader(HttpHeaders.CONTENT_TYPE, "text/html")
                        .end(res.result());
            } else {
                ctx.fail(res.cause());
            }
        };
    }

    public static void renderIndex(@Nonnull TemplateEngine engine,
                                   @Nonnull String asn, @Nonnull List<Peer> peers,
                                   @Nonnull RoutingContext ctx) {
        renderIndex(engine, asn, peers, getGeneralRenderingHandler(ctx));
    }

    public static void renderIndex(@Nonnull TemplateEngine engine,
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

    public static void renderForm(@Nonnull TemplateEngine engine,
                                  @Nonnull INodeService nodeService,
                                  boolean newForm,
                                  @Nonnull String asn, @Nullable Peer peer, @Nullable List<String> errors,
                                  @Nonnull RoutingContext ctx) {
        renderForm(engine, nodeService, newForm, asn, peer, errors, getGeneralRenderingHandler(ctx));
    }

    @SuppressWarnings("unchecked")
    public static void renderForm(@Nonnull TemplateEngine engine,
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


    public static void renderChangepw(@Nonnull TemplateEngine engine,
                                      @Nonnull String asn, @Nullable List<String> errors,
                                      @Nonnull RoutingContext ctx) {
        renderChangepw(engine, asn, errors, getGeneralRenderingHandler(ctx));
    }

    public static void renderChangepw(@Nonnull TemplateEngine engine,
                                @Nonnull String asn, @Nullable List<String> errors,
                                @Nonnull Handler<AsyncResult<Buffer>> handler) {
        final Map<String, Object> root = new HashMap<>();
        root.put("asn", asn);
        root.put("errors", errors);
        engine.render(root, "manage/changepw.ftlh", handler);
    }

    public static void renderDA(@Nonnull TemplateEngine engine,
                                @Nonnull String asn, @Nullable List<String> errors,
                                @Nonnull RoutingContext ctx) {
        renderDA(engine, asn, errors, getGeneralRenderingHandler(ctx));
    }

    public static void renderDA(@Nonnull TemplateEngine engine,
                          @Nonnull String asn, @Nullable List<String> errors,
                          @Nonnull Handler<AsyncResult<Buffer>> handler) {
        final Map<String, Object> root = new HashMap<>();
        root.put("asn", asn);
        root.put("errors", errors);
        engine.render(root, "manage/delete.ftlh", handler);
    }

    public static void renderShowConfig(@Nonnull INodeService nodeService,
                                                  @Nonnull TemplateEngine engine,
                                                  @Nonnull Peer peer,
                                                  @Nonnull RoutingContext ctx) {
        renderShowConfig(nodeService, engine, peer).onComplete(getGeneralRenderingHandler(ctx));
    }

    @Nonnull
    public static Future<Buffer> renderShowConfig(@Nonnull INodeService nodeService,
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
                    root.put("peer_type", peer.getType());
                    root.put("peer_ipv4", peer.getIpv4());
                    root.put("peer_ipv6", peer.getIpv6());
                    if(peer.getWgEndpoint() != null) {
                        root.put("peer_wg_listen_port", peer.getWgEndpointPort());
                    }

                    if(node == null) {
                        root.put("ipv4", "This node is currently down! Edit the peer to choose another one.");
                        root.put("ipv6", "This node is currently down! Edit the peer to choose another one.");
                        root.put("asn", "This node is currently down! Edit the peer to choose another one.");
                        root.put("endpoint", "This node is currently down! Edit the peer to choose another one.");
                        root.put("show_example_config", false);
                    } else {
                        root.put("show_example_config", true);
                        root.put("ipv4", node.getDn42Ip4());
                        try {
                            if(peer.isIPv6LinkLocal()) {
                                root.put("ipv6", node.getDn42Ip6());
                                root.put("peer_link_local", true);
                            } else {
                                root.put("ipv6", node.getDn42Ip6NonLL());
                                root.put("peer_link_local", false);
                            }
                        } catch (IOException e) {
                            return Future.failedFuture(e);
                        }
                        root.put("asn", node.getAsn().substring(2));
                        root.put("endpoint", node.getPublicIp());
                    }

                    return engine.render(root, "manage/showconf.ftlh");
                });

    }

    @Nonnull
    public static Future<Peer> parseForm(@Nonnull INodeService nodeService,
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
                                    ipv6 = ipv6.replaceAll("((?::0\\b){2,}):?(?!\\S*\\b\\1:0\\b)(\\S*)", "::$2");
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
}
