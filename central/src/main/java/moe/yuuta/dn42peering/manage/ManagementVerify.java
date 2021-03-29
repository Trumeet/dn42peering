package moe.yuuta.dn42peering.manage;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import moe.yuuta.dn42peering.peer.Peer;
import moe.yuuta.dn42peering.portal.FormException;
import moe.yuuta.dn42peering.whois.IWhoisService;
import moe.yuuta.dn42peering.whois.WhoisObject;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Objects;

class ManagementVerify {
    public static Future<Peer> verifyIPv4RouteOrThrow(@Nonnull IWhoisService whoisService,
                                                  @Nonnull Peer peer,
                                                  @Nonnull String asn) {
        return Future.<WhoisObject>future(f -> whoisService.query(peer.getIpv4(), f))
                .compose(whoisObject -> {
                    if (whoisObject == null ||
                            !whoisObject.containsKey("origin") ||
                            !whoisObject.get("origin").contains(asn)) {
                        return Future.failedFuture(new FormException(peer,
                                "The IPv4 address you specified does not have a route with your ASN."));
                    } else {
                        return Future.succeededFuture(peer);
                    }
                });
    }

    public static Future<Peer> verifyIPv6RouteOrThrow(@Nonnull IWhoisService whoisService,
                                                      @Nonnull Peer peer,
                                                      @Nonnull String asn) {
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
    }

    public static Future<Peer> verifyAllOrThrow(@Nonnull IWhoisService whoisService,
                                                @Nonnull Peer peer,
                                                @Nonnull String asn) {
        return CompositeFuture.all(verifyIPv4RouteOrThrow(whoisService, peer, asn),
                verifyIPv6RouteOrThrow(whoisService, peer, asn))
                .compose(_res -> Future.succeededFuture(peer));
    }

    public static boolean determineIfNeedCheckIPv4(@Nonnull Peer existingPeer,
                                                   @Nonnull Peer inPeer) {
        boolean needCheckIPv4Conflict;

        if (existingPeer.getType() != inPeer.getType()) {
            needCheckIPv4Conflict = true;
        } else {
            needCheckIPv4Conflict =
                    !Objects.equals(existingPeer.getIpv4(), inPeer.getIpv4());
        }

        return needCheckIPv4Conflict;
    }

    public static boolean determineIfNeedCheckIPv6(@Nonnull Peer existingPeer,
                                                   @Nonnull Peer inPeer) {
        boolean needCheckIPv6Conflict;

        if (existingPeer.getType() != inPeer.getType()) {
            needCheckIPv6Conflict = true;
        } else {
            needCheckIPv6Conflict =
                    !Objects.equals(existingPeer.getIpv6(), inPeer.getIpv6());
            if (inPeer.getIpv6() == null) needCheckIPv6Conflict = false;
            else {
                try {
                    if(inPeer.isIPv6LinkLocal())
                        needCheckIPv6Conflict = false;
                } catch (IOException ignored) {}
            }
        }

        return needCheckIPv6Conflict;
    }
}
