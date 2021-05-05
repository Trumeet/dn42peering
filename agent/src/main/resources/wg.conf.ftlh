[Interface]
ListenPort = ${listen_port?long?c}
PrivateKey = ${self_priv_key}

[Peer]
PublicKey = ${peer_pub_key}
PresharedKey = ${preshared_key}
<#if endpoint??>
Endpoint = ${endpoint}
</#if>
AllowedIPs = 10.127.0.0/16, 172.20.0.0/14, 172.31.0.0/16, fc00::/7, fe80::/64
