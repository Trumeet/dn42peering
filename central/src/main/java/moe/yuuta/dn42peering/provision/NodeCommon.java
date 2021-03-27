package moe.yuuta.dn42peering.provision;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import moe.yuuta.dn42peering.agent.proto.Node;

import javax.annotation.Nonnull;

@DataObject
public class NodeCommon {
    private Long id;
    private String ipv4;
    private String ipv6;
    private String ipv6NonLL;
    private String internalIp;
    private int internalPort;

    public NodeCommon(long id,
                      @Nonnull String ipv4,
                      @Nonnull String ipv6,
                      @Nonnull String ipv6NonLL,
                      @Nonnull String internalIp,
                      @Nonnull Integer internalPort) {
        this.id = id;
        this.ipv4 = ipv4;
        this.ipv6 = ipv6;
        this.ipv6NonLL = ipv6NonLL;
        this.internalIp = internalIp;
        this.internalPort = internalPort;
    }

    public NodeCommon(@Nonnull JsonObject json) {
        if(json.getValue("id") != null) this.id = json.getLong("id");
        else this.id = null;
        this.ipv4 = json.getString("ipv4");
        this.ipv6 = json.getString("ipv6");
        this.ipv6NonLL = json.getString("ipv6_non_ll");
        this.internalIp = json.getString("internal_ip");
        if(json.getValue("internal_port") != null) this.internalPort = json.getInteger("internal_port");
    }

    @Nonnull
    public JsonObject toJson() {
        return new JsonObject()
                .put("id", id)
                .put("ipv4", ipv4)
                .put("ipv6", ipv6)
                .put("ipv6_non_ll", ipv6NonLL)
                .put("internal_ip", internalIp)
                .put("internal_port", internalPort);
    }

    @Nonnull
    public Node toGRPC() {
        final Node.Builder builder = Node.newBuilder()
                .setIpv4(ipv4)
                .setIpv6(ipv6)
                .setIpv6NonLL(ipv6NonLL);
        if(id != null) builder.setId(id);
        return builder.build();
    }

    // Getters & Getters

    public Long getId() {
        return id;
    }

    public String getIpv4() {
        return ipv4;
    }

    public String getIpv6() {
        return ipv6;
    }

    public String getIpv6NonLL() {
        return ipv6NonLL;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setIpv4(String ipv4) {
        this.ipv4 = ipv4;
    }

    public void setIpv6(String ipv6) {
        this.ipv6 = ipv6;
    }

    public void setIpv6NonLL(String ipv6NonLL) {
        this.ipv6NonLL = ipv6NonLL;
    }

    String getInternalIp() {
        return internalIp;
    }

    void setInternalIp(String internalIp) {
        this.internalIp = internalIp;
    }

    int getInternalPort() {
        return internalPort;
    }

    void setInternalPort(int internalPort) {
        this.internalPort = internalPort;
    }
}
