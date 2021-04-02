package moe.yuuta.dn42peering.peer;

import com.wireguard.crypto.Key;
import com.wireguard.crypto.KeyFormatException;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.format.SnakeCase;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.templates.annotations.Column;
import io.vertx.sqlclient.templates.annotations.ParametersMapped;
import io.vertx.sqlclient.templates.annotations.RowMapped;
import io.vertx.sqlclient.templates.annotations.TemplateParameter;
import moe.yuuta.dn42peering.agent.proto.BGPConfig;
import moe.yuuta.dn42peering.agent.proto.WireGuardConfig;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.Inet6Address;

// DB Table: peer
@DataObject(jsonPropertyNameFormatter = SnakeCase.class)
@RowMapped(formatter = SnakeCase.class)
@ParametersMapped(formatter = SnakeCase.class)
public class Peer {
    public enum VPNType {
        WIREGUARD
    }

    @Column(name = "id")
    @TemplateParameter(name = "id")
    private int id;

    @Column(name = "type")
    @TemplateParameter(name = "type")
    private VPNType type;

    @Column(name = "asn")
    @TemplateParameter(name = "asn")
    private String asn;

    @Column(name = "ipv4")
    @TemplateParameter(name = "ipv4")
    private String ipv4;

    @Column(name = "ipv6")
    @TemplateParameter(name = "ipv6")
    private String ipv6;

    @Column(name = "wg_endpoint")
    @TemplateParameter(name = "wg_endpoint")
    private String wgEndpoint;

    @Column(name = "wg_endpoint_port")
    @TemplateParameter(name = "wg_endpoint_port")
    private Integer wgEndpointPort;

    @Column(name = "wg_self_pubkey")
    @TemplateParameter(name = "wg_self_pubkey")
    private String wgSelfPubkey;

    @Column(name = "wg_self_privkey")
    @TemplateParameter(name = "wg_self_privkey")
    private String wgSelfPrivKey;

    @Column(name = "wg_peer_pubkey")
    @TemplateParameter(name = "wg_peer_pubkey")
    private String wgPeerPubkey;

    @Column(name = "wg_preshared_secret")
    @TemplateParameter(name = "wg_preshared_secret")
    private String wgPresharedSecret;

    @Column(name = "provision_status")
    @TemplateParameter(name = "provision_status")
    private ProvisionStatus provisionStatus;

    @Column(name = "mpbgp")
    @TemplateParameter(name = "mpbgp")
    private boolean mpbgp;

    @Column(name = "node")
    @TemplateParameter(name = "node")
    private int node;

    public Peer() {}

    public Peer(int id,
                VPNType type,
                String asn,
                String ipv4,
                String ipv6,
                String wgEndpoint,
                Integer wgEndpointPort,
                String wgSelfPubkey,
                String wgSelfPrivKey,
                String wgPeerPubkey,
                String wgPresharedSecret,
                ProvisionStatus provisionStatus,
                boolean mpbgp,
                int node) {
        this.id = id;
        this.type = type;
        this.asn = asn;
        this.ipv4 = ipv4;
        this.ipv6 = ipv6;
        this.wgEndpoint = wgEndpoint;
        this.wgEndpointPort = wgEndpointPort;
        this.wgSelfPubkey = wgSelfPubkey;
        this.wgSelfPrivKey = wgSelfPrivKey;
        this.wgPeerPubkey = wgPeerPubkey;
        this.wgPresharedSecret = wgPresharedSecret;
        this.provisionStatus = provisionStatus;
        this.mpbgp = mpbgp;
        this.node = node;
    }

    public Peer(@Nonnull JsonObject jsonObject) {
        this(jsonObject.getInteger("id"),
                jsonObject.getString("type") == null ? null : VPNType.valueOf(jsonObject.getString("type")),
                jsonObject.getString("asn"),
                jsonObject.getString("ipv4"),
                jsonObject.getString("ipv6"),
                jsonObject.getString("wg_endpoint"),
                jsonObject.getInteger("wg_endpoint_port"),
                jsonObject.getString("wg_self_pubkey"),
                jsonObject.getString("wg_self_privkey"),
                jsonObject.getString("wg_peer_pubkey"),
                jsonObject.getString("wg_preshared_secret"),
                jsonObject.getString("provision_status") == null ? null :
                ProvisionStatus.valueOf(jsonObject.getString("provision_status")),
                jsonObject.getBoolean("mpbgp"),
                jsonObject.getInteger("node"));
    }

    /**
     * Create an instance with unfinished new WireGuard peering.
     * Keys are generated. ASN is not filled in. ID is set to -1. ProvisionStatus is set to not provisioned.
     */
    public Peer(String ipv4,
                String ipv6,
                String wgEndpoint,
                Integer wgEndpointPort,
                String wgPeerPubkey,
                boolean mpbgp,
                int node) {
        this(-1,
                VPNType.WIREGUARD,
                null /* ASN: To be filled later */,
                ipv4,
                ipv6,
                wgEndpoint,
                wgEndpointPort,
                null /* Public key: generate later */,
                Key.generatePrivateKey().toBase64(),
                wgPeerPubkey,
                Key.generatePrivateKey().toBase64(),
                ProvisionStatus.NOT_PROVISIONED,
                mpbgp,
                node);
        try {
            wgSelfPubkey = Key.generatePublicKey(Key.fromBase64(wgSelfPrivKey)).toBase64();
        } catch (KeyFormatException e) { /* Should not happen */
            throw new RuntimeException(e); }
    }

    @Nonnull
    public JsonObject toJson() {
        return new JsonObject()
                .put("id", id)
                .put("type", type)
                .put("asn", asn)
                .put("ipv4", ipv4)
                .put("ipv6", ipv6)
                .put("wg_endpoint", wgEndpoint)
                .put("wg_endpoint_port", wgEndpointPort)
                .put("wg_self_pubkey", wgSelfPubkey)
                .put("wg_self_privkey", wgSelfPrivKey)
                .put("wg_peer_pubkey", wgPeerPubkey)
                .put("wg_preshared_secret", wgPresharedSecret)
                .put("provision_status", provisionStatus)
                .put("mpbgp", mpbgp)
                .put("node", node);
    }

    @Nonnull
    public String calcWireGuardPort() {
        return asn.substring(asn.length() - 5);
    }

    @GenIgnore
    public boolean isIPv6LinkLocal() throws IOException {
        return Inet6Address.getByName(ipv6).isLinkLocalAddress();
    }

    @Nonnull
    @GenIgnore
    public WireGuardConfig toWireGuardConfig() {
        if(type != VPNType.WIREGUARD) throw new IllegalStateException("The VPN type is not WireGuard.");
        return WireGuardConfig.newBuilder()
                .setId(id)
                .setListenPort(Integer.parseInt(calcWireGuardPort()))
                .setEndpoint(wgEndpoint == null && wgEndpointPort == null ? "" :
                        String.format("%s:%d", getWgEndpoint(), getWgEndpointPort()))
                .setPeerPubKey(wgPeerPubkey)
                .setSelfPrivKey(wgSelfPrivKey)
                .setSelfPresharedSecret(wgPresharedSecret)
                .setPeerIPv4(ipv4)
                .setPeerIPv6(ipv6 == null ? "" : ipv6)
                .setInterface(calculateWireGuardInterfaceName())
                .build();
    }
    
    @GenIgnore
    public BGPConfig toBGPConfig() {
        return BGPConfig.newBuilder()
                .setId(id)
                .setAsn(Long.parseLong(asn.substring(2)))
                .setMpbgp(mpbgp)
                .setIpv4(ipv4)
                .setIpv6(ipv6 == null ? "" : ipv6)
                .setInterface(calculateWireGuardInterfaceName())
                .build();
    }

    @Nonnull
    public String calculateWireGuardInterfaceName() {
        return String.format("wg_%d", id);
    }

    // START GETTERS / SETTERS

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public VPNType getType() {
        return type;
    }

    public void setType(VPNType type) {
        this.type = type;
    }

    public String getAsn() {
        return asn;
    }

    public void setAsn(String asn) {
        this.asn = asn;
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

    public String getWgEndpoint() {
        return wgEndpoint;
    }

    public void setWgEndpoint(String wgEndpoint) {
        this.wgEndpoint = wgEndpoint;
    }

    public Integer getWgEndpointPort() {
        return wgEndpointPort;
    }

    public void setWgEndpointPort(Integer wgEndpointPort) {
        this.wgEndpointPort = wgEndpointPort;
    }

    public String getWgSelfPubkey() {
        return wgSelfPubkey;
    }

    public void setWgSelfPubkey(String wgSelfPubkey) {
        this.wgSelfPubkey = wgSelfPubkey;
    }

    public String getWgSelfPrivKey() {
        return wgSelfPrivKey;
    }

    public void setWgSelfPrivKey(String wgSelfPrivKey) {
        this.wgSelfPrivKey = wgSelfPrivKey;
    }

    public String getWgPeerPubkey() {
        return wgPeerPubkey;
    }

    public void setWgPeerPubkey(String wgPeerPubkey) {
        this.wgPeerPubkey = wgPeerPubkey;
    }

    public String getWgPresharedSecret() {
        return wgPresharedSecret;
    }

    public void setWgPresharedSecret(String wgPresharedSecret) {
        this.wgPresharedSecret = wgPresharedSecret;
    }

    public ProvisionStatus getProvisionStatus() {
        return provisionStatus;
    }

    public void setProvisionStatus(ProvisionStatus provisionStatus) {
        this.provisionStatus = provisionStatus;
    }

    public boolean isMpbgp() {
        return mpbgp;
    }

    public void setMpbgp(boolean mpbgp) {
        this.mpbgp = mpbgp;
    }

    public int getNode() {
        return node;
    }

    public void setNode(int node) {
        this.node = node;
    }

    // END GETTERS / SETTERS
}
