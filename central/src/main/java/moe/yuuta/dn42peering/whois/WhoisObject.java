package moe.yuuta.dn42peering.whois;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@DataObject(generateConverter = true)
public class WhoisObject {
    private Map<String, JsonArray> map;

    public WhoisObject(JsonObject jsonObject) {
        map = new HashMap<>(jsonObject.size());
        jsonObject.stream()
                .forEach(entity -> {
                    map.put(entity.getKey(), (JsonArray) entity.getValue());
                });
    }

    public WhoisObject(@Nonnull Map<String, List<String>> map) {
        this.map = new HashMap<>(map.size());
        map.keySet().forEach(key -> {
            final JsonArray array = new JsonArray();
            map.get(key).forEach(array::add);
            this.map.put(key, array);
        });
    }

    @Nonnull
    public JsonObject toJson() {
        final JsonObject jsonObject = new JsonObject();
        map.keySet()
                .forEach(key -> jsonObject.put(key, map.get(key)));
        return jsonObject;
    }

    public boolean containsKey(@Nonnull String key) {
        return map.containsKey(key);
    }

    public List<String> getOrDefault(@Nonnull String key, List<String> def) {
        if(containsKey(key)) return map.get(key).stream().map(obj -> (String)obj).collect(Collectors.toList());
        return def;
    }

    @Nonnull
    public List<String> get(@Nonnull String key) {
        return getOrDefault(key, Collections.emptyList());
    }
}
