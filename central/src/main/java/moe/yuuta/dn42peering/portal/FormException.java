package moe.yuuta.dn42peering.portal;

import javax.annotation.Nonnull;

public class FormException extends Exception {
    public String[] errors;
    public final Object data;

    public FormException(@Nonnull String... errors) {
        this(null, errors);
    }

    public FormException(Object data, @Nonnull String... errors) {
        this.errors = errors;
        this.data = data;
    }
}
