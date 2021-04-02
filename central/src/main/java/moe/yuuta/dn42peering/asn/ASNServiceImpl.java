package moe.yuuta.dn42peering.asn;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.sqlclient.*;
import io.vertx.sqlclient.templates.SqlTemplate;
import moe.yuuta.dn42peering.utils.PasswordAuthentication;
import moe.yuuta.dn42peering.whois.IWhoisService;
import moe.yuuta.dn42peering.whois.WhoisObject;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ASNServiceImpl implements IASNService {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private final Vertx vertx;
    private final Pool pool;

    ASNServiceImpl(@Nonnull Vertx vertx, @Nonnull Pool pool) {
        this.vertx = vertx;
        this.pool = pool;
    }

    @Nonnull
    @Override
    public IASNService exists(@Nonnull String asn, boolean requireActivationStatus, boolean activated,
                              @Nonnull Handler<AsyncResult<Boolean>> handler) {
        final PreparedQuery<RowSet<Row>> preparedQuery =
                !requireActivationStatus ? pool.preparedQuery("SELECT COUNT(asn) FROM asn WHERE asn = ?")
                                : pool.preparedQuery("SELECT COUNT(asn) FROM asn WHERE asn = ? AND activated = ?");
        preparedQuery.execute(!requireActivationStatus ? Tuple.of(asn) : Tuple.of(asn, activated))
                .compose(rows -> {
                    int count = rows.iterator().next().getInteger(0);
                    return Future.succeededFuture(count > 0);
                })
                .onComplete(handler);
        return this;
    }

    @Nonnull
    @Override
    public IASNService markAsActivated(@Nonnull String asn, @Nonnull Handler<AsyncResult<Void>> handler) {
        pool.preparedQuery("UPDATE asn SET activated = 1 WHERE asn = ? AND activated = 0")
                .execute(Tuple.of(asn))
                .<Void>compose(rows -> {
                    return Future.succeededFuture(null);
                })
                .onComplete(handler);
        return this;
    }

    @Nonnull
    @Override
    public IASNService changePassword(@Nonnull String asn, @Nonnull String newPassword, @Nonnull Handler<AsyncResult<Void>> handler) {
        final Map<String, Object> params = new HashMap<>(2);
        params.put("asn", asn);
        params.put("password_hash", new PasswordAuthentication()
                .hash(newPassword.toCharArray()));
        Future.<SqlResult<Void>>future(f -> SqlTemplate
                .forUpdate(pool, "UPDATE asn SET password_hash = #{password_hash} WHERE asn = #{asn}")
                .execute(params, f))
                .<Void>compose(voidSqlResult -> Future.succeededFuture(null))
                .onComplete(handler);
        return this;
    }

    @Nonnull
    @Override
    public IASNService auth(@Nonnull String asn, @Nonnull String password, @Nonnull Handler<AsyncResult<Boolean>> handler) {
        Future.<RowSet<ASN>>future(f -> SqlTemplate
                .forQuery(pool, "SELECT password_hash FROM asn WHERE asn = #{asn}")
                .mapTo(ASNRowMapper.INSTANCE)
                .mapFrom(ASNParametersMapper.INSTANCE)
                .execute(new ASN(asn, "", false, new Date()), f))
                .compose(rows -> {
                    // No such user
                    if(!rows.iterator().hasNext()) {
                        return Future.succeededFuture(false);
                    }
                    final ASN record = rows.iterator().next();
                    return Future.succeededFuture(record.auth(password));
                })
                .onComplete(handler);
        return this;
    }

    @Nonnull
    @Override
    public IASNService delete(@Nonnull String asn, @Nonnull Handler<AsyncResult<Void>> handler) {
        Future.<SqlResult<Void>>future(f -> SqlTemplate.forUpdate(pool, "DELETE FROM asn WHERE asn = #{asn}")
        .execute(Collections.singletonMap("asn", asn), f))
                .<Void>compose(voidSqlResult -> Future.succeededFuture())
                .onComplete(handler);
        return this;
    }

    @Nonnull
    @Override
    public IASNService lookupEmails(@Nonnull WhoisObject asn, @Nonnull Handler<AsyncResult<List<String>>> handler) {
        if (!asn.containsKey("tech-c") || asn.get("tech-c").isEmpty()) {
            handler.handle(Future.succeededFuture(null));
            return this;
        }
        Future.<WhoisObject>future(f -> IWhoisService.createProxy(vertx, IWhoisService.ADDRESS)
                .query(asn.get("tech-c").get(0) /* tech-c is only permitted to appear once */, f))
                .compose(techLookup -> {
                    if(techLookup == null) {
                        return Future.succeededFuture(Collections.<String>emptyList());
                    } else {
                        final List<String> verifyMethodList = new ArrayList<>(3);
                        final List<String> contacts = new ArrayList<>();
                        contacts.addAll(techLookup.getOrDefault("contact", Collections.emptyList()));
                        contacts.addAll(techLookup.getOrDefault("e-mail", Collections.emptyList()));
                        verifyMethodList.addAll(Stream.of(contacts)
                                .flatMap(Collection::stream)
                                .map(contact -> {
                                    final Matcher m =
                                            Pattern.compile("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+")
                                                    .matcher(contact);
                                    final List<String> verifiers = new ArrayList<>(1);
                                    while (m.find()) {
                                        verifiers.add(m.group());
                                    }
                                    return verifiers;
                                })
                                .flatMap(List::stream)
                                .collect(Collectors.toList()));
                        return Future.succeededFuture(verifyMethodList);
                    }
                })
                .onComplete(handler);
        return this;
    }

    @Nonnull
    @Override
    public IASNService registerOrChangePassword(@Nonnull String asn, @Nonnull String newPassword, @Nonnull Handler<AsyncResult<Void>> handler) {
        // TODO: No activated check here.
        Future.<RowSet<Row>>future(f -> pool.preparedQuery("REPLACE INTO asn (asn, password_hash) VALUES(?, ?)")
                .execute(Tuple.of(asn, new PasswordAuthentication().hash(newPassword.toCharArray())), f))
                .<Void>compose(res -> Future.succeededFuture(null))
                .onComplete(handler);
        return this;
    }

    @Nonnull
    @Override
    public IASNService count(@Nonnull Handler<AsyncResult<Integer>> handler) {
        SqlTemplate
                .forQuery(pool, "SELECT COUNT(asn) FROM asn")
                .execute(null)
                .compose(rows -> Future.succeededFuture(rows.iterator().next().getInteger(0)))
                .onComplete(handler);
        return this;
    }
}
