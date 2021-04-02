package moe.yuuta.dn42peering.agent.provision;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FileChange extends Change {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    public enum Action {
        CREATE_AND_WRITE,
        OVERWRITE,
        DELETE
    }

    public FileChange(@Nonnull String path,
                      @Nullable String contents,
                      @Nonnull String action) {
        super(path, contents, action);
        Action.valueOf(action); // Verify
    }

    @Nonnull
    @Override
    public Future<Void> execute(@Nonnull Vertx vertx) {
        switch (Action.valueOf(action)) {
            case CREATE_AND_WRITE:
                logger.info("Writing " + id + " with:\n" + to);
                return vertx.fileSystem().open(id, new OpenOptions()
                        .setCreateNew(true)
                        .setTruncateExisting(true)
                        .setWrite(true))
                        .compose(asyncFile -> {
                            return asyncFile.write(Buffer.buffer(to == null ? "" : to), 0)
                                    .compose(_v -> Future.succeededFuture(asyncFile));
                        })
                        .compose(asyncFile -> {
                            return asyncFile.close();
                        });
            case OVERWRITE:
                logger.info("Overwriting " + id + " with:\n" + to);
                return vertx.fileSystem().open(id, new OpenOptions()
                        .setCreateNew(false)
                        .setTruncateExisting(true)
                        .setWrite(true))
                        .compose(asyncFile -> {
                            return asyncFile.write(Buffer.buffer(to == null ? "" : to), 0)
                                    .compose(_v -> Future.succeededFuture(asyncFile));
                        })
                        .compose(asyncFile -> {
                            return asyncFile.close();
                        });
            case DELETE:
                logger.info("Deleting " + id);
                return vertx.fileSystem().delete(id);
            default:
                throw new UnsupportedOperationException("Unknown file change action " + action);
        }
    }
}
