package moe.yuuta.dn42peering.agent.provision;

import io.vertx.core.Future;
import moe.yuuta.dn42peering.agent.proto.Node;

import javax.annotation.Nonnull;
import java.util.List;

public interface IProvisioner<T> {
    @Nonnull
    Future<List<Change>> calculateChanges(@Nonnull Node node, @Nonnull List<T> allDesired);
}
