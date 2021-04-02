package moe.yuuta.dn42peering.node;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.mysqlclient.MySQLException;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlResult;
import io.vertx.sqlclient.templates.SqlTemplate;
import moe.yuuta.dn42peering.database.DatabaseUtils;

import javax.annotation.Nonnull;
import java.util.*;

class NodeServiceImpl implements INodeService {
    private final Pool pool;
    private final Vertx vertx;

    NodeServiceImpl(@Nonnull Vertx vertx, @Nonnull Pool mySQLPool) {
        this.vertx = vertx;
        this.pool = mySQLPool;
    }

    @Nonnull
    @Override
    public INodeService listNodes(@Nonnull Handler<AsyncResult<List<Node>>> handler) {
        SqlTemplate
                .forQuery(pool, "SELECT id, public_ip, dn42_ip4, dn42_ip6, dn42_ip6_nonll, asn, " +
                        "internal_ip, internal_port, name, notice, vpn_type_wg " +
                        "FROM node")
                .mapTo(NodeRowMapper.INSTANCE)
                .execute(null)
                .compose(nodeRowMappers -> {
                    final List<Node> nodes = new ArrayList<>(10);
                    for (Node node : nodeRowMappers)
                        nodes.add(node);
                    return Future.succeededFuture(nodes);
                })
                .onComplete(handler);
        return this;
    }

    @Nonnull
    @Override
    public INodeService getNode(int id, @Nonnull Handler<AsyncResult<Node>> handler) {
        SqlTemplate
                .forQuery(pool, "SELECT id, public_ip, asn, " +
                        "dn42_ip4, dn42_ip6, dn42_ip6_nonll, " +
                        "internal_ip, internal_port, name, notice, vpn_type_wg " +
                        "FROM node " +
                        "WHERE id = #{id}")
                .mapTo(NodeRowMapper.INSTANCE)
                .execute(Collections.singletonMap("id", id))
                .compose(nodeRowMappers -> {
                    if (nodeRowMappers.iterator().hasNext()) {
                        return Future.succeededFuture(nodeRowMappers.iterator().next());
                    } else {
                        return Future.succeededFuture(null);
                    }
                })
                .onComplete(handler);
        return this;
    }

    @Nonnull
    @Override
    public INodeService addNew(@Nonnull Node node,
                               @Nonnull Handler<AsyncResult<Long>> handler) {
        node.setId(0);
        Future.<Long>future(f -> {
            Future.<RowSet<Node>>future(f1 -> SqlTemplate
                    .forUpdate(pool, "INSERT INTO node (id, asn, name, notice, " +
                            "public_ip, " +
                            "dn42_ip4, dn42_ip6, dn42_ip6_nonll," +
                            "internal_ip, internal_port," +
                            "vpn_type_wg) " +
                            "VALUES (#{id}, #{asn}, #{name}, #{notice}, " +
                            "#{public_ip}, " +
                            "#{dn42_ip4}, #{dn42_ip6}, #{dn42_ip6_nonll}, " +
                            "#{internal_ip}, #{internal_port}, " +
                            "#{vpn_type_wg}" +
                            ")")
                    .mapFrom(NodeParametersMapper.INSTANCE)
                    .mapTo(NodeRowMapper.INSTANCE)
                    .execute(node, f1))
                    .compose(rows -> Future.succeededFuture(rows.property(DatabaseUtils.LAST_INSERTED_ID)))
                    .onFailure(err -> {
                        if (err instanceof MySQLException) {
                            if (((MySQLException) err).getErrorCode() == 1062 /* Duplicate */) {
                                f.fail(new DuplicateNodeException());
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
    public INodeService updateTo(@Nonnull Node node, @Nonnull Handler<AsyncResult<Long>> handler) {
        Future.<Long>future(f -> {
            Future.<RowSet<Node>>future(f1 -> SqlTemplate
                    .forUpdate(pool, "UPDATE node SET " +
                            "asn = #{asn}," +
                            "name = #{name}," +
                            "notice = #{notice}," +
                            "public_ip = #{public_ip}," +
                            "dn42_ip4 = #{dn42_ip4}," +
                            "dn42_ip6 = #{dn42_ip6}," +
                            "dn42_ip6_nonll = #{dn42_ip6_nonll}," +
                            "internal_ip = #{internal_ip}," +
                            "internal_port = #{internal_port}," +
                            "vpn_type_wg = #{vpn_type_wg} " +
                            "WHERE id = #{id}")
                    .mapFrom(NodeParametersMapper.INSTANCE)
                    .mapTo(NodeRowMapper.INSTANCE)
                    .execute(node, f1))
                    .compose(rows -> Future.succeededFuture(rows.property(DatabaseUtils.LAST_INSERTED_ID)))
                    .onFailure(err -> {
                        if (err instanceof MySQLException) {
                            if (((MySQLException) err).getErrorCode() == 1062 /* Duplicate */) {
                                f.fail(new DuplicateNodeException());
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
    public INodeService delete(int id, @Nonnull Handler<AsyncResult<Void>> handler) {
        final Map<String, Object> params = new HashMap<>(2);
        params.put("id", id);
        Future.<SqlResult<Void>>future(f -> SqlTemplate
                .forUpdate(pool, "DELETE FROM node WHERE id = #{id}")
                .execute(params, f))
                .<Void>compose(voidSqlResult -> Future.succeededFuture(null))
                .onComplete(handler);
        return this;
    }
}
