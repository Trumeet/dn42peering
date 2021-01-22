<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Peering Configuration</title>
</head>
<body>
<h1>Peering Configuration</h1>
<p>Here are the information needed for you to setup:</p>
<ul>
    <li>WireGuard Endpoint: ${endpoint}:${wgPort}</li>
    <li>dn42 IPv4 Address: ${ipv4}</li>
    <li>dn42 IPv6 Address: ${ipv6}</li>
    <li>ASN: AS${asn}</li>
    <li>WireGuard Public Key: ${wgSelfPubkey}</li>
    <li>WireGuard Preshared Secret: ${wgPresharedSecret}</li>
    <li>MP-BGP: ${mpbgp?string('Yes', 'No')}</li>
</ul>
<#if show_example_config>
<h2>Example Configuration</h2>
<p>Notice: The following configuration is for newcomers or referencing purposes only.
    You should always check and modify them according to your environment.
<b>Bold parts are placeholders and you should customize them.</b></p>
<h3>BIRD2 Configuration</h3>
<p>You first need to setup your BIRD2 by following <a href="https://wiki.dn42.us/howto/Bird2">dn42 Wiki</a>.</p>
<pre>
    # /etc/bird/peers/<b>&lt;Custom Name&gt;</b>.conf
    <#if !mpbgp>
    protocol bgp dn42_<b>&lt;Custom Name&gt;</b> from dnpeers {
            neighbor ${ipv4} as ${asn};
            direct;
    }
    </#if>
    <#if peer_ipv6??>
    protocol bgp dn42_<b>&lt;Custom Name&gt;</b>_v6 from dnpeers {
        neighbor ${ipv6}<#if peer_link_local><b>%&lt;Your WireGuard interface name&gt</b>;</#if> as ${asn};
        direct;
    }
    </#if>
</pre>
<#if peer_type == "WIREGUARD">
<h3>WireGuard Configuration</h3>
<p>Choose ONE of the follow method.</p>
<h4>wg-quick</h4>
<pre>
    # /etc/wireguard/<b>&lt;Your WireGuard interface name&gt;</b>.conf
    [Interface]
    <#if peer_wg_listen_port??>
    ListenPort = ${peer_wg_listen_port?long?c}
    </#if>
    PrivateKey = <b>&lt;Your WireGuard Private Key&gt;</b>
    PostUp = ip addr add dev <b>&lt;Your WireGuard interface name&gt</b>; ${peer_ipv4}/32 peer ${ipv4}/32
    <#if peer_ipv6??>
    <#if peer_link_local>
    PostUp = ip addr add dev <b>&lt;Your WireGuard interface name&gt</b>; ${peer_ipv6}/64
    <#else>
    PostUp = ip addr add dev <b>&lt;Your WireGuard interface name&gt</b>; ${peer_ipv6}/64 peer ${ipv6}/64
    </#if>
    </#if>
    Table = off

    [Peer]
    PublicKey = ${wgSelfPubkey}
    PresharedKey = ${wgPresharedSecret}
    Endpoint = ${endpoint}:${wgPort}
    AllowedIPs = 0.0.0.0/0, ::/0
</pre>
</#if>
<p>Then, enable and start wg-quick service. For systemd users, you may want to use systemctl enable --now wg-quick@<b>&lt;Your WireGuard interface name&gt</b>.</p>
</#if>
<a href="/manage">Back</a>
</body>
</html>