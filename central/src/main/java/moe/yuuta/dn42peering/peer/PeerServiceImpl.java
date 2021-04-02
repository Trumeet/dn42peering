package moe.yuuta.dn42peering.peer;

import io.vertx.core.*;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.mysqlclient.MySQLException;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlResult;
import io.vertx.sqlclient.templates.SqlTemplate;
import moe.yuuta.dn42peering.database.DatabaseUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

class PeerServiceImpl implements IPeerService {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private final Vertx vertx;
    private final Pool pool;

    PeerServiceImpl(@Nonnull Vertx vertx, @Nonnull Pool sql) {
        this.vertx = vertx;
        this.pool = sql;
    }

    @Nonnull
    @Override
    public IPeerService listUnderASN(@Nonnull String asn, @Nonnull Handler<AsyncResult<List<Peer>>> handler) {
        SqlTemplate
                .forQuery(pool, "SELECT id, type, asn, ipv4, ipv6, " +
                        "wg_endpoint, wg_endpoint_port, " +
                        "wg_self_pubkey, wg_self_privkey, wg_peer_pubkey, wg_preshared_secret, " +
                        "provision_status, mpbgp, node FROM peer " +
                        "WHERE asn = #{asn}")
                .mapTo(PeerRowMapper.INSTANCE)
                .execute(Collections.singletonMap("asn", asn))
                .compose(peers -> {
                    final List<Peer> peerList = new ArrayList<>();
                    for (Peer peer : peers) peerList.add(peer);
                    return Future.succeededFuture(peerList);
                })
                .onFailure(err -> err.printStackTrace())
                .onComplete(handler);
        return this;
    }

    @Nonnull
    @Override
    public IPeerService existsUnderASN(@Nonnull String asn, @Nonnull Handler<AsyncResult<Boolean>> handler) {
        SqlTemplate
                .forQuery(pool, "SELECT COUNT(id) FROM peer " +
                        "WHERE asn = #{asn}")
                .execute(Collections.singletonMap("asn", asn))
                .compose(rows -> Future.succeededFuture(rows.iterator().next().getInteger(0) > 0))
                .onComplete(handler);
        return this;
    }

    @Nonnull
    @Override
    public IPeerService updateTo(@Nonnull Peer peer, @Nonnull Handler<AsyncResult<Void>> handler) {
        SqlTemplate
                .forUpdate(pool, "UPDATE peer SET " +
                        "type = #{type}, " +
                        "ipv4 = #{ipv4}, " +
                        "ipv6 = #{ipv6}, " +
                        "wg_endpoint = #{wg_endpoint}, " +
                        "wg_endpoint_port = #{wg_endpoint_port}, " +
                        "wg_self_pubkey = #{wg_self_pubkey}, " +
                        "wg_self_privkey = #{wg_self_privkey}, " +
                        "wg_peer_pubkey = #{wg_peer_pubkey}, " +
                        "wg_preshared_secret = #{wg_preshared_secret}, " +
                        "provision_status = #{provision_status}, " +
                        "mpbgp = #{mpbgp}, " +
                        "node = #{node} " +
                        "WHERE id = #{id} AND asn = #{asn}")
                .mapFrom(PeerParametersMapper.INSTANCE)
                .execute(peer)
                .<Void>compose(res -> Future.succeededFuture(null))
                .onComplete(handler);
        return this;
    }

    @Nonnull
    @Override
    public IPeerService addNew(@Nonnull Peer peer, @Nonnull Handler<AsyncResult<Long>> handler) {
        peer.setId(0);
        Future.<Long>future(f -> {
            Future.<RowSet<Peer>>future(f1 -> SqlTemplate
                    .forQuery(pool, "INSERT INTO peer VALUES (#{id}, #{type}, #{asn}, " +
                            "#{ipv4}, #{ipv6}, " +
                            "#{wg_endpoint}, #{wg_endpoint_port}, " +
                            "#{wg_self_pubkey}, #{wg_self_privkey}, " +
                            "#{wg_peer_pubkey}, #{wg_preshared_secret}, " +
                            "#{provision_status}, #{mpbgp}, #{node}" +
                            ")")
                    .mapFrom(PeerParametersMapper.INSTANCE)
                    .mapTo(PeerRowMapper.INSTANCE)
                    .execute(peer, f1))
                    .compose(rows -> Future.succeededFuture(rows.property(DatabaseUtils.LAST_INSERTED_ID)))
                    .onFailure(err -> {
                        if (err instanceof MySQLException) {
                            if (((MySQLException) err).getErrorCode() == 1062 /* Duplicate */) {
                                f.fail(new DuplicatePeerException());
                                return;
                            }
                            f.fail(err);
                        }
                    })
                    .onSuccess(f::complete);
        }).onComplete(handler);
        return this;
    }

    @Nonnull
    @Override
    public IPeerService isIPConflict(@Nonnull Peer.VPNType type, @Nullable String ipv4, @Nullable String ipv6, @Nonnull Handler<AsyncResult<Boolean>> handler) {
        final List<Future> futures = new ArrayList<>(2);
        if (ipv4 != null) {
            final Map<String, Object> params = new HashMap<>(3);
            params.put("type", type.toString());
            params.put("ip", ipv4);

            futures.add(Future.<RowSet<Row>>future(f -> SqlTemplate
                    .forQuery(pool, "SELECT COUNT(id) FROM peer WHERE type = #{type} AND ipv4 = #{ip}")
                    .execute(params, f))
                    .compose(rows -> Future.succeededFuture(rows.iterator().next().getInteger(0) > 0)));
        }
        if (ipv6 != null) {
            final Map<String, Object> params = new HashMap<>(3);
            params.put("type", type.toString());
            params.put("ip", ipv6);

            futures.add(Future.<RowSet<Row>>future(f -> SqlTemplate
                    .forQuery(pool, "SELECT COUNT(id) FROM peer WHERE type = #{type} AND ipv6 = #{ip}")
                    .execute(params, f))
                    .compose(rows -> Future.succeededFuture(rows.iterator().next().getInteger(0) > 0)));
        }
        if(futures.isEmpty()) {
            Future.succeededFuture(false).onComplete(handler);
            return this;
        }
        CompositeFuture.all(futures)
                .compose(future -> {
                    final List<Boolean> res = future.list();
                    for (boolean b : res) {
                        if (b) return Future.succeededFuture(true);
                    }
                    return Future.succeededFuture(false);
                })
                .onComplete(handler);
        return this;
    }

    @Nonnull
    @Override
    public IPeerService getSingle(@Nonnull String asn, String id, @Nonnull Handler<AsyncResult<Peer>> handler) {
        final Map<String, Object> params = new HashMap<>(2);
        params.put("id", id);
        params.put("asn", asn);
        Future.<RowSet<Peer>>future(f -> SqlTemplate
                .forQuery(pool, "SELECT id, type, asn, " +
                        "ipv4, ipv6, " +
                        "wg_endpoint, wg_endpoint_port, " +
                        "wg_self_pubkey, wg_self_privkey, " +
                        "wg_peer_pubkey, wg_preshared_secret, " +
                        "provision_status, mpbgp, node " +
                        "FROM peer " +
                        "WHERE id = #{id} AND asn = #{asn}")
                .mapTo(PeerRowMapper.INSTANCE)
                .execute(params, f))
                .compose(peers -> {
                    if(peers.iterator().hasNext())
                        return Future.succeededFuture(peers.iterator().next());
                    return Future.succeededFuture(null);
                })
                .onComplete(handler);
        return this;
    }

    @Nonnull
    @Override
    public IPeerService deletePeer(@Nonnull String asn, String id, @Nonnull Handler<AsyncResult<Void>> handler) {
        final Map<String, Object> params = new HashMap<>(2);
        params.put("id", id);
        params.put("asn", asn);
        Future.<SqlResult<Void>>future(f -> SqlTemplate
                .forUpdate(pool, "DELETE FROM peer WHERE id = #{id} AND asn = #{asn}")
                .execute(params, f))
        .<Void>compose(voidSqlResult -> Future.succeededFuture(null))
        .onComplete(handler);
        return this;
    }

    @Nonnull
    @Override
    public IPeerService changeProvisionStatus(int id, @Nonnull ProvisionStatus provisionStatus, @Nonnull Handler<AsyncResult<Void>> handler) {
        final Map<String, Object> params = new HashMap<>(2);
        params.put("id", id);
        params.put("provision_status", provisionStatus);
        SqlTemplate
                .forUpdate(pool, "UPDATE peer SET provision_status = #{provision_status} WHERE id = #{id}")
                .execute(params)
                .compose(res -> Future.<Void>succeededFuture(null))
                .onComplete(handler);
        return this;
    }
}
