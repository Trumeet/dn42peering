package moe.yuuta.dn42peering.provision;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import moe.yuuta.dn42peering.agent.proto.BGPRequest;

import javax.annotation.Nonnull;

@DataObject
public class BGPRequestCommon {
    private NodeCommon node;
    private Long id;
    private String asn;
    private Boolean mpbgp;
    private String ipv4;
    private String ipv6;
    private String device;

    public BGPRequestCommon(NodeCommon node,
                            Long id,
                            String asn,
                            Boolean mpbgp,
                            String ipv4,
                            String ipv6,
                            String device) {
        this.node = node;
        this.id = id;
        this.asn = asn;
        this.mpbgp = mpbgp;
        this.ipv4 = ipv4;
        this.ipv6 = ipv6;
        this.device = device;
    }

    public BGPRequestCommon(@Nonnull JsonObject json) {
        if(json.getValue("node") != null) this.node = new NodeCommon(json.getJsonObject("node"));
        if(json.getValue("id") != null) this.id = json.getLong("id");
        this.asn = json.getString("asn");
        if(json.getValue("mpbgp") != null) this.mpbgp = json.getBoolean("mpbgp");
        this.ipv4 = json.getString("ipv4");
        this.ipv6 = json.getString("ipv6");
        this.device = json.getString("device");
    }

    @Nonnull
    public JsonObject toJson() {
        return new JsonObject()
                .put("node", node == null ? null : node.toJson())
                .put("id", id)
                .put("asn", asn)
                .put("mpbgp", mpbgp)
                .put("ipv4", ipv4)
                .put("ipv6", ipv6)
                .put("device", device);
    }

    @Nonnull
    public BGPRequest toGRPC() {
        final BGPRequest.Builder builder = BGPRequest.newBuilder();
        if(node != null) builder.setNode(node.toGRPC());
        if(id != null) builder.setId(id);
        if(asn != null) builder.setAsn(asn);
        if(mpbgp != null) builder.setMpbgp(mpbgp);
        if(ipv4 != null) builder.setIpv4(ipv4);
        if(ipv6 != null) builder.setIpv6(ipv6);
        if(device != null) builder.setDevice(device);
        return builder.build();
    }

    // Getters & Setters

    public NodeCommon getNode() {
        return node;
    }

    public void setNode(NodeCommon node) {
        this.node = node;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAsn() {
        return asn;
    }

    public void setAsn(String asn) {
        this.asn = asn;
    }

    public Boolean getMpbgp() {
        return mpbgp;
    }

    public void setMpbgp(Boolean mpbgp) {
        this.mpbgp = mpbgp;
    }

    public String getIpv4() {
        return ipv4;
    }

    public void setIpv4(String ipv4) {
        this.ipv4 = ipv4;
    }

    public String getIpv6() {
        return ipv6;
    }

    public void setIpv6(String ipv6) {
        this.ipv6 = ipv6;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }
}
