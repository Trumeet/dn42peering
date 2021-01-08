package moe.yuuta.dn42peering.node;

import io.grpc.ManagedChannel;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.format.SnakeCase;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.sqlclient.templates.annotations.Column;
import io.vertx.sqlclient.templates.annotations.ParametersMapped;
import io.vertx.sqlclient.templates.annotations.RowMapped;
import io.vertx.sqlclient.templates.annotations.TemplateParameter;
import moe.yuuta.dn42peering.peer.Peer;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@DataObject
@RowMapped(formatter = SnakeCase.class)
@ParametersMapped(formatter = SnakeCase.class)
public class Node {
    @Column(name = "id")
    @TemplateParameter(name = "id")
    private int id;

    @Column(name = "public_ip")
    @TemplateParameter(name = "public_ip")
    private String publicIp;

    @Column(name = "dn42_ip4")
    @TemplateParameter(name = "dn42_ip4")
    private String dn42Ip4;

    @Column(name = "dn42_ip6")
    @TemplateParameter(name = "dn42_ip6")
    private String dn42Ip6;

    @Column(name = "asn")
    @TemplateParameter(name = "asn")
    private String asn;

    @Column(name = "internal_ip")
    @TemplateParameter(name = "internal_ip")
    private String internalIp;

    @Column(name = "internal_port")
    @TemplateParameter(name = "internal_port")
    private int internalPort;

    @Column(name = "name")
    @TemplateParameter(name = "name")
    private String name;

    @Column(name = "notice")
    @TemplateParameter(name = "notice")
    private String notice;

    @Column(name = "vpn_type_wg")
    @TemplateParameter(name = "vpn_type_wg")
    private boolean wireguard;

    public Node() {}

    public Node(@Nonnull JsonObject object) {
        this.id = object.getInteger("id");
        this.publicIp = object.getString("public_ip");
        this.dn42Ip4 = object.getString("dn42_ip4");
        this.dn42Ip6 = object.getString("dn42_ip6");
        this.asn = object.getString("asn");
        this.internalIp = object.getString("internal_ip");
        this.internalPort = object.getInteger("internal_port");
        this.name = object.getString("name");
        this.notice = object.getString("notice");
        this.wireguard = object.getBoolean("wireguard");
    }

    @Nonnull
    public JsonObject toJson() {
        return new JsonObject()
                .put("id", id)
                .put("public_ip", publicIp)
                .put("dn42_ip4", dn42Ip4)
                .put("dn42_ip6", dn42Ip6)
                .put("asn", asn)
                .put("internal_ip", internalIp)
                .put("internal_port", internalPort)
                .put("name", name)
                .put("notice", notice)
                .put("wireguard", wireguard);
    }

    @GenIgnore
    @Nonnull
    public List<Peer.VPNType> getSupportedVPNTypes() {
        final List<Peer.VPNType> vpnTypes = new ArrayList<>(1);
        if(wireguard) vpnTypes.add(Peer.VPNType.WIREGUARD);
        return vpnTypes;
    }

    @GenIgnore
    @Nonnull
    public moe.yuuta.dn42peering.agent.proto.Node toRPCNode() {
        return moe.yuuta.dn42peering.agent.proto.Node.newBuilder()
                .setId(id)
                .setIpv4(dn42Ip4)
                .setIpv6(dn42Ip6)
                .build();
    }

    @GenIgnore
    @Nonnull
    public ManagedChannel toChannel(@Nonnull Vertx vertx) {
        return VertxChannelBuilder.forAddress(vertx, internalIp, internalPort)
                .usePlaintext()
                .build();
    }

    // BEGIN GETTER / SETTER

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public String getDn42Ip4() {
        return dn42Ip4;
    }

    public void setDn42Ip4(String dn42Ip4) {
        this.dn42Ip4 = dn42Ip4;
    }

    public String getDn42Ip6() {
        return dn42Ip6;
    }

    public void setDn42Ip6(String dn42Ip6) {
        this.dn42Ip6 = dn42Ip6;
    }

    public String getAsn() {
        return asn;
    }

    public void setAsn(String asn) {
        this.asn = asn;
    }

    public String getInternalIp() {
        return internalIp;
    }

    public void setInternalIp(String internalIp) {
        this.internalIp = internalIp;
    }

    public int getInternalPort() {
        return internalPort;
    }

    public void setInternalPort(int internalPort) {
        this.internalPort = internalPort;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    public boolean isWireguard() {
        return wireguard;
    }

    public void setWireguard(boolean wireguard) {
        this.wireguard = wireguard;
    }


    // END GETTER / SETTER
}
