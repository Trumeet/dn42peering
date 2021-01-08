package moe.yuuta.dn42peering.agent.provision;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import javax.annotation.Nonnull;
import java.io.IOException;

public class AsyncShell {
    @Nonnull
    public static Future<Integer> exec(@Nonnull Vertx vertx,
                                       @Nonnull String... cmd) {
        return vertx.executeBlocking(f -> {
            try {
                int res = new ProcessBuilder()
                        .command(cmd)
                        .redirectError(ProcessBuilder.Redirect.INHERIT)
                        .start()
                        .waitFor();
                f.complete(res);
            } catch (IOException | InterruptedException e) {
                f.fail(e);
            }
        });
    }

    public static void exec(@Nonnull Vertx vertx,
                            @Nonnull Handler<AsyncResult<Integer>> handler,
                            @Nonnull String... cmd) {
        exec(vertx, cmd).onComplete(handler);
    }

    @Nonnull
    public static Future<Void> execSucc(@Nonnull Vertx vertx,
                                @Nonnull String... cmd) {
        return Future.future(f -> exec(vertx, cmd)
                .onSuccess(res -> {
                    if(res != 0) {
                        f.fail(String.format("Unexpected return code %d", res));
                    } else {
                        f.complete(null);
                    }
                })
                .onFailure(f::fail));
    }
}
