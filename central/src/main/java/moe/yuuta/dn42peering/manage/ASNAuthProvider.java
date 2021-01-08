package moe.yuuta.dn42peering.manage;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import moe.yuuta.dn42peering.asn.IASNService;

import javax.annotation.Nonnull;

class ASNAuthProvider implements AuthenticationProvider {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private final IASNService asnService;

    ASNAuthProvider(@Nonnull IASNService asnService) {
        this.asnService = asnService;
    }

    @Override
    public void authenticate(JsonObject credentials, Handler<AsyncResult<User>> resultHandler) {
        final String username = credentials.getString("username");
        final String password = credentials.getString("password");
        Future.<Boolean>future(f -> asnService.auth(username.toUpperCase(), password, f))
                .compose(succ -> {
                    if(succ) {
                        return Future.succeededFuture(User.fromName(username.toUpperCase()));
                    } else {
                        return Future.failedFuture("Incorrect logon");
                    }
                })
                .onComplete(resultHandler);
    }
}
