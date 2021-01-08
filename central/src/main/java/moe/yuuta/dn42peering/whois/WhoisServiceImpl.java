package moe.yuuta.dn42peering.whois;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import moe.yuuta.dn42peering.jaba.OutParm;
import org.apache.commons.net.whois.WhoisClient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class WhoisServiceImpl implements IWhoisService {
    private final Vertx vertx;

    WhoisServiceImpl(@Nonnull Vertx vertx) {
        this.vertx = vertx;
    }

    @Nonnull
    @Override
    public IWhoisService query(@Nonnull String handle, @Nonnull Handler<AsyncResult<WhoisObject>> handler) {
        final WhoisClient whoisClient = new WhoisClient();
        vertx.<String>executeBlocking(f -> {
            try {
                whoisClient.connect(vertx.getOrCreateContext().config().getString("whois"));
                final String result = whoisClient.query(handle);
                whoisClient.disconnect();
                f.complete(result);
            } catch (IOException e) {
                f.fail(e);
            }
        }).compose(rawOutput -> {
            if(rawOutput == null) return Future.succeededFuture(null);
            else return Future.succeededFuture(parseObject(rawOutput));
        }).onComplete(handler);
        return this;
    }

    @Nullable
    private static WhoisObject parseObject(@Nonnull String s) {
        final Map<String, List<String>> obj = new HashMap<>(5);
        for(final String line : s.split("\n")) {
            final OutParm<String> key = new OutParm<>();
            final OutParm<String> value = new OutParm<>();
            if(!parseLine(line, key, value)) {
                continue;
            }
            final List<String> values = obj.containsKey(key.out) ? obj.get(key.out) : new ArrayList<>(1);
            values.add(value.out);
            obj.put(key.out, values);
        }
        if(obj.isEmpty()) return null;
        return new WhoisObject(obj);
    }

    /* Testing only */ static boolean parseLine(@Nonnull String line, @Nonnull OutParm<String> key, @Nonnull OutParm<String> value) {
        if(line.startsWith("#") || line.startsWith("%")) return false;
        if(line.length() < 20) return false;
        final String part1 = line.substring(0, 20);
        final String part2 = line.substring(20);
        key.out = part1.split(":")[0];
        value.out = part2;
        return true;
    }
}
