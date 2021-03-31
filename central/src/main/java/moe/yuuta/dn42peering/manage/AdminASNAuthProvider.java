package moe.yuuta.dn42peering.manage;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import moe.yuuta.dn42peering.asn.IASNService;

import javax.annotation.Nonnull;

public class AdminASNAuthProvider extends ASNAuthProvider {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private final String adminASN;

    public AdminASNAuthProvider(@Nonnull String adminASN,
                         @Nonnull IASNService asnService) {
        super(asnService);
        this.adminASN = adminASN;
    }

    @Override
    public void authenticate(JsonObject credentials, Handler<AsyncResult<User>> resultHandler) {
        super.authenticate(credentials, ar -> {
            if(ar.failed()) {
                resultHandler.handle(ar);
                return;
            }
            final User user = ar.result();
            if(!adminASN.equals(user.principal().getString("username"))) {
                logger.warn("Unsuccessful admin attempt by " + user.principal().getString("username"));
                resultHandler.handle(Future.failedFuture("Incorrect logon"));
            } else {
                logger.info("Successful admin attempt by " + user.principal().getString("username"));
                user.attributes().put("admin", true);
                resultHandler.handle(ar);
            }
        });
    }
}
