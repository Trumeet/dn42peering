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
    private Future<List<Change>> calculateDeleteChanges() {
        final String[] actualNames = new File("/etc/bird/peers").list((dir, name) -> name.matches("dn42_.*\\.conf"));
        if(actualNames == null)
            return Future.succeededFuture(Collections.emptyList());
        return Future.succeededFuture(Arrays.stream(actualNames)
                .map(string -> new FileChange("/etc/bird/peers/" + string, null, FileChange.Action.DELETE.toString()))
                .collect(Collectors.toList()));
    }

    @Nonnull
    private String generateBGPPath() {
        return vertx
                .getOrCreateContext()
                .config()
                .getString("bird_output_path", "/etc/bird/peers/dn42peers.conf");
    }

    @Nonnull
    private Future<Buffer> readConfig() {
        return Future.future(f -> {
            vertx.fileSystem()
                    .readFile(generateBGPPath())
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
    private Future<Buffer> renderConfig(@Nonnull List<BGPConfig> configs) {
        final Map<String, Object> params = new HashMap<>(1);
        final List<Object> configParams = new ArrayList<>(configs.size());
        configs.forEach(config -> {
            final Map<String, Object> param = new HashMap<>(6);
            param.put("name", config.getId());
            param.put("asn", config.getAsn());
            param.put("ipv4", config.getIpv4());
            param.put("ipv6", config.getIpv6().equals("") ? null : config.getIpv6());
            param.put("mpbgp", config.getMpbgp());
            param.put("dev", config.getInterface());
            configParams.add(param);
        });
        params.put("sessions", configParams);
        return engine.render(params, "bird2.conf.ftlh");
    }

    @Nonnull
    private Future<List<Change>> calculateSingleChange(@Nonnull List<BGPConfig> desiredConfigs) {
        return CompositeFuture.all(readConfig(), renderConfig(desiredConfigs))
                .compose(future -> {
                    final Buffer actualBuff = future.resultAt(0);
                    final String actual = actualBuff == null ? null : actualBuff.toString();
                    final String desired = future.resultAt(1).toString();
                    final List<Change> changes = new ArrayList<>(1);
                    if(actual == null) {
                        changes.add(new FileChange(generateBGPPath(),
                                desired,
                                FileChange.Action.CREATE_AND_WRITE.toString()));
                    } else if(!actual.equals(desired)) {
                        changes.add(new FileChange(generateBGPPath(),
                                desired,
                                FileChange.Action.OVERWRITE.toString()));
                    }
                    return Future.succeededFuture(changes);
                });
    }

    @Nonnull
    @Override
    public Future<List<Change>> calculateChanges(@Nonnull Node node, @Nonnull List<BGPConfig> allDesired) {
        // All of these calculations can be done in parallel but we must wait all of them to finish.
        // The three major steps above must be done in sequence.
        // Step 1: Calculate individual BGP changes in parallel and combine them into a single future.
        return calculateSingleChange(allDesired)
                // Step 2: Calculate things to delete.
                .compose(changes -> {
                    return calculateDeleteChanges().compose(deleteChangeList -> {
                        changes.addAll(deleteChangeList);
                        return Future.succeededFuture(changes);
                    });
                })
                // Step 3: Reload at last
                .compose(changes -> {
                    return Future.succeededFuture(Collections.singletonList(
                            new CommandChange(new String[]{"birdc", "configure"}))).compose(reloadChangeList -> {
                        changes.addAll(reloadChangeList);
                        return Future.succeededFuture(changes);
                    });
                });
    }
}
