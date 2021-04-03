[Interface]
ListenPort = ${listen_port?long?c}
PrivateKey = ${self_priv_key}

[Peer]
PublicKey = ${peer_pub_key}
PresharedKey = ${preshared_key}
<#if endpoint??>
Endpoint = ${endpoint}
</#if>
AllowedIPs = 0.0.0.0/0, ::/0
