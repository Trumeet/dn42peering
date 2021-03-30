package moe.yuuta.dn42peering.portal;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.api.service.ServiceResponse;

import javax.annotation.Nonnull;

public class RenderingUtils {
    public static Handler<AsyncResult<Buffer>> getGeneralRenderingHandler(@Nonnull Handler<AsyncResult<ServiceResponse>> handler) {
        return res -> {
            if (res.succeeded()) {
                handler.handle(Future.succeededFuture(new ServiceResponse(200,
                        null,
                        res.result(),
                        MultiMap.caseInsensitiveMultiMap().add(HttpHeaders.CONTENT_TYPE, "text/html"))));
            } else {
                handler.handle(Future.failedFuture(res.cause()));
            }
        };
    }
}
