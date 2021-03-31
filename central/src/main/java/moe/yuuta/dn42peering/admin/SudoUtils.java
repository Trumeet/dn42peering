package moe.yuuta.dn42peering.admin;

import io.vertx.core.http.Cookie;

import javax.annotation.Nonnull;

public class SudoUtils {
    public static final String SUDO_COOKIE = "sudo";

    @Nonnull
    public static String getTargetASN(@Nonnull Cookie cookie) {
        return cookie.getValue();
    }
}
