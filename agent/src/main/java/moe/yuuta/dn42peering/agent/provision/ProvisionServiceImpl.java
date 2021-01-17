package moe.yuuta.dn42peering.agent.provision;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.web.common.template.TemplateEngine;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.Inet6Address;
import java.util.HashMap;
import java.util.Map;

class ProvisionServiceImpl implements IProvisionService {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private final Vertx vertx;
    private final TemplateEngine engine;

    ProvisionServiceImpl(@Nonnull Vertx vertx, @Nonnull TemplateEngine engine) {
        this.vertx = vertx;
        this.engine = engine;
    }

    @Nonnull
    private static String generateBGPPath(int id) {
        return String.format("/etc/bird/peers/dn42_%d.conf", id);
    }

    @Nonnull
    private static String generateWGPath(@Nonnull String dev) {
        return String.format("/etc/wireguard/%s.conf", dev);
    }

    @Nonnull
    private static String generateWireGuardDevName(long id) {
        return String.format("wg_%d", id);
    }

    @Nonnull
    private static String getLockNameForBGP(long id) {
        return String.format("BGP:%d", id);
    }

    @Nonnull
    private static String getLockNameForWG(long id) {
        return String.format("WG:%d", id);
    }

    @Nonnull
    private Future<Void> writeBGPConfig(@Nonnull String localIP4,
                                        @Nonnull String localIP6,
                                        @Nonnull String localIP6NonLL,
                                        int id,
                                        @Nonnull String ipv4,
                                        @Nullable String ipv6,
                                        @Nullable String device,
                                        boolean mpbgp,
                                        @Nonnull String asn,
                                        boolean create) {
        final String asnNum = asn.replace("AS", "");
        return vertx.fileSystem().open(generateBGPPath(id), new OpenOptions()
                .setCreateNew(create)
                .setTruncateExisting(true)
                .setWrite(true))
                .compose(asyncFile -> {
                    if (mpbgp) return Future.succeededFuture(asyncFile);
                    final Map<String, Object> params = new HashMap<>(3);
                    params.put("name", id);
                    params.put("ipv4", ipv4);
                    params.put("asn", asnNum);
                    return engine.render(params, "bird2_v4.conf.ftlh")
                            .compose(buffer -> asyncFile.write(buffer)
                                    .compose(_v1 -> Future.succeededFuture(asyncFile)));
                })
                .compose(asyncFile -> {
                    if (ipv6 == null) return Future.succeededFuture(asyncFile);
                    final Map<String, Object> params = new HashMap<>(4);
                    params.put("name", id);
                    params.put("ipv6", ipv6);
                    params.put("asn", asnNum);
                    params.put("dev", device);
                    return engine.render(params, "bird2_v6.conf.ftlh")
                            .compose(buffer -> asyncFile.write(buffer)
                                    .compose(_v1 -> Future.succeededFuture(asyncFile)));
                })
                .compose(AsyncFile::close);
    }

    @Nonnull
    private Future<Void> writeWGConfig(boolean create,
                                       @Nonnull String localIP4,
                                       @Nonnull String localIP6,
                                       @Nonnull String localIP6NonLL,
                                       @Nonnull String dev,
                                       int listenPort,
                                       @Nullable String endpointWithPort,
                                       @Nonnull String peerPubKey,
                                       @Nonnull String selfPrivKey,
                                       @Nonnull String selfPresharedSecret,
                                       @Nonnull String peerIPv4,
                                       @Nullable String peerIPv6) {
        return vertx.fileSystem().open(generateWGPath(dev), new OpenOptions()
                .setCreateNew(create)
                .setTruncateExisting(true)
                .setWrite(true))
                .compose(asyncFile -> {
                    final Map<String, Object> params = new HashMap<>(9);
                    params.put("listen_port", listenPort);
                    params.put("self_priv_key", selfPrivKey);
                    params.put("dev", dev);
                    params.put("self_ipv4", localIP4);
                    params.put("peer_ipv4", peerIPv4);
                    params.put("peer_ipv6", peerIPv6);
                    if (peerIPv6 != null) {
                        try {
                            final boolean ll = Inet6Address.getByName(peerIPv6).isLinkLocalAddress();
                            params.put("peer_ipv6_ll", ll);
                            if(ll)
                                params.put("self_ipv6", localIP6);
                            else
                                params.put("self_ipv6", localIP6NonLL);
                        } catch (IOException e) {
                            return Future.failedFuture(e);
                        }
                    }
                    params.put("preshared_key", selfPresharedSecret);
                    params.put("endpoint", endpointWithPort);
                    params.put("peer_pub_key", peerPubKey);

                    return engine.render(params, "wg.conf.ftlh")
                            .compose(buffer -> asyncFile.write(buffer)
                                    .compose(_v1 -> Future.succeededFuture(asyncFile)));
                })
                .compose(AsyncFile::close);
    }

    @Nonnull
    private Future<Void> deleteBGPConfig(int id) {
        return vertx.fileSystem().exists(generateBGPPath(id))
                .compose(exists -> {
                    if (exists) return vertx.fileSystem().delete(generateBGPPath(id));
                    else return Future.succeededFuture(null);
                });
    }

    @Nonnull
    private Future<Void> deleteWGConfig(@Nonnull String dev) {
        return vertx.fileSystem().delete(generateWGPath(dev));
    }

    @Nonnull
    @Override
    public IProvisionService provisionBGP(@Nonnull String localIP4,
                                          @Nonnull String localIP6,
                                          @Nonnull String localIP6NonLL,
                                          int id,
                                          @Nonnull String ipv4,
                                          @Nullable String ipv6,
                                          @Nullable String device,
                                          boolean mpbgp,
                                          @Nonnull String asn,
                                          @Nonnull Handler<AsyncResult<Void>> handler) {
        vertx.sharedData().getLocalLockWithTimeout(getLockNameForBGP(id), 1000)
                .compose(lock ->
                        writeBGPConfig(localIP4, localIP6, localIP6NonLL, id, ipv4, ipv6, device, mpbgp, asn, true)
                                .compose(_v -> AsyncShell.execSucc(vertx, "birdc", "configure"))
                                .onComplete(ar -> lock.release())
                )
                .onComplete(handler);
        return this;
    }

    @Nonnull
    @Override
    public IProvisionService reloadBGP(@Nonnull String localIP4,
                                       @Nonnull String localIP6,
                                       @Nonnull String localIP6NonLL,
                                       int id,
                                       @Nonnull String ipv4,
                                       @Nullable String ipv6,
                                       @Nullable String device,
                                       boolean mpbgp,
                                       @Nonnull String asn,
                                       @Nonnull Handler<AsyncResult<Void>> handler) {
        vertx.sharedData().getLocalLockWithTimeout(getLockNameForBGP(id), 1000)
                .compose(lock ->
                        writeBGPConfig(localIP4, localIP6, localIP6NonLL, id, ipv4, ipv6, device, mpbgp, asn, false)
                                .compose(_v -> AsyncShell.execSucc(vertx, "birdc", "configure"))
                                .onComplete(ar -> lock.release())
                )
                .onComplete(handler);
        return this;
    }

    @Nonnull
    @Override
    public IProvisionService unprovisionBGP(int id, @Nonnull Handler<AsyncResult<Void>> handler) {
        vertx.sharedData().getLocalLockWithTimeout(getLockNameForBGP(id), 1000)
                .compose(lock ->
                        deleteBGPConfig(id)
                                .compose(_v -> AsyncShell.execSucc(vertx, "birdc", "configure"))
                                .onComplete(ar -> lock.release())
                )
                .onComplete(handler);
        return this;
    }

    @Nonnull
    @Override
    public IProvisionService provisionVPNWireGuard(@Nonnull String localIP4,
                                                   @Nonnull String localIP6,
                                                   @Nonnull String localIP6NonLL,
                                                   int id,
                                                   int listenPort,
                                                   @Nullable String endpointWithPort,
                                                   @Nonnull String peerPubKey,
                                                   @Nonnull String selfPrivKey,
                                                   @Nonnull String selfPresharedSecret,
                                                   @Nonnull String peerIPv4,
                                                   @Nullable String peerIPv6,
                                                   @Nonnull Handler<AsyncResult<String>> handler) {
        vertx.sharedData()
                .getLocalLockWithTimeout(getLockNameForWG(id), 1000)
                .compose(lock -> writeWGConfig(true,
                        localIP4,
                        localIP6,
                        localIP6NonLL,
                        generateWireGuardDevName(id),
                        listenPort,
                        endpointWithPort,
                        peerPubKey,
                        selfPrivKey,
                        selfPresharedSecret,
                        peerIPv4,
                        peerIPv6)
                        .compose(_v -> AsyncShell.execSucc(vertx, "systemctl", "enable", "--now", "-q",
                                String.format("wg-quick@%s", generateWireGuardDevName(id))))
                        .onComplete(_v -> lock.release()))
                .compose(_v -> Future.succeededFuture(generateWireGuardDevName(id)))
                .onComplete(handler);
        return this;
    }

    @Nonnull
    @Override
    public IProvisionService reloadVPNWireGuard(@Nonnull String localIP4,
                                                @Nonnull String localIP6,
                                                @Nonnull String localIP6NonLL,
                                                int id,
                                                int listenPort,
                                                @Nullable String endpointWithPort,
                                                @Nonnull String peerPubKey,
                                                @Nonnull String selfPrivKey,
                                                @Nonnull String selfPresharedSecret,
                                                @Nonnull String peerIPv4,
                                                @Nullable String peerIPv6,
                                                @Nonnull Handler<AsyncResult<String>> handler) {
        vertx.sharedData()
                .getLocalLockWithTimeout(getLockNameForWG(id), 1000)
                .compose(lock -> writeWGConfig(false,
                        localIP4,
                        localIP6,
                        localIP6NonLL,
                        generateWireGuardDevName(id),
                        listenPort,
                        endpointWithPort,
                        peerPubKey,
                        selfPrivKey,
                        selfPresharedSecret,
                        peerIPv4,
                        peerIPv6)
                        .compose(_v -> AsyncShell.execSucc(vertx, "systemctl", "enable", "-q",
                                String.format("wg-quick@%s", generateWireGuardDevName(id))))
                        .compose(_v -> AsyncShell.execSucc(vertx, "systemctl", "reload-or-restart",
                                String.format("wg-quick@%s", generateWireGuardDevName(id))))
                        .onComplete(_v -> lock.release()))
                .compose(_v -> Future.succeededFuture(generateWireGuardDevName(id)))
                .onComplete(handler);
        return this;
    }

    @Nonnull
    @Override
    public IProvisionService unprovisionVPNWireGuard(int id, @Nonnull Handler<AsyncResult<Void>> handler) {
        vertx.sharedData()
                .getLocalLockWithTimeout(getLockNameForWG(id), 1000)
                .compose(lock -> AsyncShell.execSucc(vertx, "systemctl", "disable", "--now", "-q",
                                String.format("wg-quick@%s", generateWireGuardDevName(id)))
                        // We need to stop the service first, then delete the configuration.
                        .compose(_v -> deleteWGConfig(generateWireGuardDevName(id)))
                        .onComplete(_v -> lock.release()))
                .onComplete(handler);
        return this;
    }
}
