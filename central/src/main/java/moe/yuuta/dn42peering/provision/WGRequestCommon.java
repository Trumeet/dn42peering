package moe.yuuta.dn42peering.provision;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import moe.yuuta.dn42peering.agent.proto.WGRequest;

import javax.annotation.Nonnull;

@DataObject
public class WGRequestCommon {
    private NodeCommon node;
    private Long id;
    private Integer listenPort;
    private String endpoint;
    private String peerPubKey;
    private String selfPrivKey;
    private String selfPresharedSecret;
    private String peerIPv4;
    private String peerIPv6;

    public WGRequestCommon(NodeCommon node,
                           Long id,
                           Integer listenPort,
                           String endpoint,
                           String peerPubKey,
                           String selfPrivKey,
                           String selfPresharedSecret,
                           String peerIPv4,
                           String peerIPv6) {
        this.node = node;
        this.id = id;
        this.listenPort = listenPort;
        this.endpoint = endpoint;
        this.peerPubKey = peerPubKey;
        this.selfPrivKey = selfPrivKey;
        this.selfPresharedSecret = selfPresharedSecret;
        this.peerIPv4 = peerIPv4;
        this.peerIPv6 = peerIPv6;
    }

    public WGRequestCommon(@Nonnull JsonObject json) {
        if(json.getValue("node") != null) this.node = new NodeCommon(json.getJsonObject("node"));
        if(json.getValue("id") != null) this.id = json.getLong("id");
        if(json.getValue("listen_port") != null) this.listenPort = json.getInteger("listen_port");
        this.endpoint = json.getString("endpoint");
        this.peerPubKey = json.getString("peer_public_key");
        this.selfPrivKey = json.getString("self_private_key");
        this.selfPresharedSecret = json.getString("self_preshared_secret");
        this.peerIPv4 = json.getString("peer_ipv4");
        this.peerIPv6 = json.getString("peer_ipv6");
    }

    @Nonnull
    public JsonObject toJson() {
        return new JsonObject()
                .put("node", node == null ? null : node.toJson())
                .put("id", id)
                .put("listen_port", listenPort)
                .put("endpoint", endpoint)
                .put("peer_public_key", peerPubKey)
                .put("self_private_key", selfPrivKey)
                .put("self_preshared_secret", selfPresharedSecret)
                .put("peer_ipv4", peerIPv4)
                .put("peer_ipv6", peerIPv6);
    }

    @Nonnull
    public WGRequest toGRPC() {
        final WGRequest.Builder builder = WGRequest.newBuilder();
        if(node != null) builder.setNode(node.toGRPC());
        if(id != null) builder.setId(id);
        if(listenPort != null) builder.setListenPort(listenPort);
        if(endpoint != null) builder.setEndpoint(endpoint);
        if(peerPubKey != null) builder.setPeerPubKey(peerPubKey);
        if(selfPrivKey != null) builder.setSelfPrivKey(selfPrivKey);
        if(selfPresharedSecret != null) builder.setSelfPresharedSecret(selfPresharedSecret);
        if(peerIPv4 != null) builder.setPeerIPv4(peerIPv4);
        if(peerIPv6 != null) builder.setPeerIPv6(peerIPv6);
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

    public Integer getListenPort() {
        return listenPort;
    }

    public void setListenPort(Integer listenPort) {
        this.listenPort = listenPort;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getPeerPubKey() {
        return peerPubKey;
    }

    public void setPeerPubKey(String peerPubKey) {
        this.peerPubKey = peerPubKey;
    }

    public String getSelfPrivKey() {
        return selfPrivKey;
    }

    public void setSelfPrivKey(String selfPrivKey) {
        this.selfPrivKey = selfPrivKey;
    }

    public String getSelfPresharedSecret() {
        return selfPresharedSecret;
    }

    public void setSelfPresharedSecret(String selfPresharedSecret) {
        this.selfPresharedSecret = selfPresharedSecret;
    }

    public String getPeerIPv4() {
        return peerIPv4;
    }

    public void setPeerIPv4(String peerIPv4) {
        this.peerIPv4 = peerIPv4;
    }

    public String getPeerIPv6() {
        return peerIPv6;
    }

    public void setPeerIPv6(String peerIPv6) {
        this.peerIPv6 = peerIPv6;
    }
}
