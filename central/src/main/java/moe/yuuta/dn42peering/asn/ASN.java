package moe.yuuta.dn42peering.asn;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.format.SnakeCase;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.templates.annotations.Column;
import io.vertx.sqlclient.templates.annotations.ParametersMapped;
import io.vertx.sqlclient.templates.annotations.RowMapped;
import io.vertx.sqlclient.templates.annotations.TemplateParameter;
import moe.yuuta.dn42peering.utils.PasswordAuthentication;

import javax.annotation.Nonnull;
import java.util.Date;

// DB Table: asn
@DataObject
@RowMapped(formatter = SnakeCase.class)
@ParametersMapped(formatter = SnakeCase.class)
public class ASN {
    @Column(name = "asn")
    @TemplateParameter(name = "asn")
    @Nonnull
    private String asn;

    @Column(name = "password_hash")
    @TemplateParameter(name = "password_hash")
    @Nonnull
    private String passwordHash;

    @Column(name = "activated")
    @TemplateParameter(name = "activated")
    private boolean activated;

    @Column(name = "register_date")
    @TemplateParameter(name = "register_date")
    private Date registerDate;

    public ASN() {
        this("", "", false, new Date());
    }

    public ASN(@Nonnull String asn, @Nonnull String passwordHash,
                boolean activated,
                @Nonnull Date registerDate) {
        this.asn = asn;
        this.passwordHash = passwordHash;
        this.activated = activated;
        this.registerDate = registerDate;
    }

    public ASN(@Nonnull JsonObject jsonObject) {
        this(jsonObject.getString("asn"),
                jsonObject.getString("password_hash"),
                jsonObject.getBoolean("activated"),
                new Date(jsonObject.getLong("register_date")));
    }

    /**
     * Create an ASN object when registering using random password.
     */
    public ASN(@Nonnull String asn, @Nonnull String randomPassword) {
        this(asn,
                new PasswordAuthentication()
                        .hash(randomPassword.toCharArray()),
            false,
        new Date());
    }

    @Nonnull
    public JsonObject toJson() {
        return new JsonObject()
                .put("asn", asn)
                .put("password_hash", passwordHash)
                .put("activated", activated)
                .put("register_date", registerDate.getTime());
    }

    public final boolean auth(@Nonnull String password) {
        return new PasswordAuthentication().authenticate(password.toCharArray(), passwordHash);
    }

    public String getAsn() {
        return asn;
    }

    public boolean isActivated() {
        return activated;
    }

    public Date getRegisterDate() {
        return registerDate;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    // BEGIN GETTERS / SETTERS

    public void setAsn(@Nonnull String asn) {
        this.asn = asn;
    }

    @Nonnull
    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(@Nonnull String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setRegisterDate(Date registerDate) {
        this.registerDate = registerDate;
    }


    // END GETTERS / SETTERS
}
