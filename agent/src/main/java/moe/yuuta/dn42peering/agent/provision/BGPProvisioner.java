package moe.yuuta.dn42peering.agent.provision;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystemException;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
import moe.yuuta.dn42peering.agent.proto.BGPConfig;
import moe.yuuta.dn42peering.agent.proto.Node;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.stream.Collectors;

public class BGPProvisioner implements IProvisioner<BGPConfig> {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private final TemplateEngine engine;
    private final Vertx vertx;

    public BGPProvisioner(@Nonnull Vertx vertx) {
        this(FreeMarkerTemplateEngine.create(vertx, "ftlh"), vertx);
    }

    public BGPProvisioner(@Nonnull TemplateEngine engine,
                          @Nonnull Vertx vertx) {
        this.engine = engine;
        this.vertx = vertx;
    }

    @Nonnull
    private Future<List<Change>> calculateDeleteChanges(@Nonnull List<BGPConfig> allDesired) {
        final String[] actualNamesRaw = new File("/etc/bird/peers").list((dir, name) -> name.matches("dn42_.*\\.conf"));
        final List<String> actualNames = Arrays.stream(actualNamesRaw == null ? new String[]{} : actualNamesRaw)
                .sorted()
                .collect(Collectors.toList());
        final String[] desiredNames = allDesired
                .stream()
                .map(desired -> generateBGPPath(desired.getId()))
                .sorted()
                .collect(Collectors.toList())
                .toArray(new String[]{});
        final List<Integer> toRemove = new ArrayList<>(actualNames.size());
        for (int i = 0; i < desiredNames.length; i ++) {
            toRemove.clear();
            for(int j = 0; j < actualNames.size(); j ++) {
                if(("/etc/bird/peers/" + actualNames.get(j)).equals(desiredNames[i])) {
                    toRemove.add(j);
                }
            }
            for (int j = 0; j < toRemove.size(); j ++) {
                actualNames.remove(toRemove.get(j).intValue());
            }
        }
        return Future.succeededFuture(actualNames.stream()
                .map(string -> new FileChange("/etc/bird/peers/" + string, null, FileChange.Action.DELETE.toString()))
                .collect(Collectors.toList()));
    }

    @Nonnull
    private static String generateBGPPath(long id) {
        return String.format("/etc/bird/peers/dn42_%d.conf", id);
    }

    @Nonnull
    private Future<Buffer> readConfig(long id) {
        return Future.future(f -> {
            vertx.fileSystem()
                    .readFile(generateBGPPath(id))
                    .onFailure(err -> {
                        if(err instanceof FileSystemException &&
                        err.getCause() instanceof NoSuchFileException) {
                            f.complete(null);
                        } else {
                            f.fail(err);
                        }
                    })
                    .onSuccess(f::complete);
        });
    }

    @Nonnull
    private Future<Buffer> renderConfig(@Nonnull BGPConfig config) {
        final Map<String, Object> params = new HashMap<>(3);
        params.put("name", config.getId());
        params.put("asn", config.getAsn());
        params.put("ipv4", config.getIpv4());
        params.put("ipv6", config.getIpv6().equals("") ? null : config.getIpv6());
        params.put("mpbgp", config.getMpbgp());
        params.put("dev", config.getInterface());
        return engine.render(params, "bird2.conf.ftlh");
    }

    @Nonnull
    private Future<List<Change>> calculateSingleChange(@Nonnull BGPConfig desiredConfig) {
        return CompositeFuture.all(readConfig(desiredConfig.getId()), renderConfig(desiredConfig))
                .compose(future -> {
                    final Buffer actualBuff = future.resultAt(0);
                    final String actual = actualBuff == null ? null : actualBuff.toString();
                    final String desired = future.resultAt(1).toString();
                    final List<Change> changes = new ArrayList<>(1);
                    if(actual == null) {
                        changes.add(new FileChange(generateBGPPath(desiredConfig.getId()),
                                desired,
                                FileChange.Action.CREATE_AND_WRITE.toString()));
                    } else if(!actual.equals(desired)) {
                        changes.add(new FileChange(generateBGPPath(desiredConfig.getId()),
                                desired,
                                FileChange.Action.OVERWRITE.toString()));
                    }
                    return Future.succeededFuture(changes);
                });
    }

    @Nonnull
    @Override
    public Future<List<Change>> calculateChanges(@Nonnull Node node, @Nonnull List<BGPConfig> allDesired) {
        final List<Future<List<Change>>> addOrModifyChanges =
                allDesired.stream().map(this::calculateSingleChange).collect(Collectors.toList());
        final Future<List<Change>> deleteChanges =
                calculateDeleteChanges(allDesired);
        final Future<List<Change>> reloadChange =
                Future.succeededFuture(Collections.singletonList(
                        new CommandChange(new String[] { "birdc", "configure" })));

        final List<Future> futures = new ArrayList<>(addOrModifyChanges.size() + 2);
        futures.addAll(addOrModifyChanges);
        futures.add(deleteChanges);
        futures.add(reloadChange);
        return CompositeFuture.all(futures)
                .compose(compositeFuture -> {
                    final List<Change> changes = new ArrayList<>(futures.size());
                    for(int i = 0; i < futures.size(); i ++) {
                        changes.addAll(compositeFuture.resultAt(i));
                    }
                    return Future.succeededFuture(changes);
                });
    }
}
