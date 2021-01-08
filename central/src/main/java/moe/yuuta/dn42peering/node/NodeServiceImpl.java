package moe.yuuta.dn42peering.node;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.templates.SqlTemplate;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class NodeServiceImpl implements INodeService {
    private final MySQLPool pool;
    private final Vertx vertx;

    NodeServiceImpl(@Nonnull Vertx vertx, @Nonnull MySQLPool mySQLPool) {
        this.vertx = vertx;
        this.pool = mySQLPool;
    }

    @Nonnull
    @Override
    public INodeService listNodes(@Nonnull Handler<AsyncResult<List<Node>>> handler) {
        SqlTemplate
                .forQuery(pool, "SELECT id, public_ip, dn42_ip4, dn42_ip6, asn, " +
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
                        "dn42_ip4, dn42_ip6, " +
                        "internal_ip, internal_port, name, notice, vpn_type_wg " +
                        "FROM node " +
                        "WHERE id = #{id}")
                .mapTo(NodeRowMapper.INSTANCE)
                .execute(Collections.singletonMap("id", id))
                .compose(nodeRowMappers -> {
                    if(nodeRowMappers.iterator().hasNext()) {
                        return Future.succeededFuture(nodeRowMappers.iterator().next());
                    } else {
                        return Future.succeededFuture(null);
                    }
                })
                .onComplete(handler);
        return this;
    }
}
