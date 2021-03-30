package moe.yuuta.dn42peering.asn;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;
import io.vertx.ext.web.api.service.WebApiServiceGen;

import javax.annotation.Nonnull;

@WebApiServiceGen
public interface IASNHttpService {
    String ADDRESS = IASNHttpService.class.getName();

    void index(@Nonnull ServiceRequest context,
               @Nonnull Handler<AsyncResult<ServiceResponse>> handler);

    void register(@Nonnull JsonObject body,
                  @Nonnull ServiceRequest context,
                  @Nonnull Handler<AsyncResult<ServiceResponse>> handler);
}
