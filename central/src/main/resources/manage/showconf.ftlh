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
    <li>ASN: ${asn}</li>
    <li>WireGuard Public Key: ${wgSelfPubkey}</li>
    <li>WireGuard Preshared Secret: ${wgPresharedSecret}</li>
    <li>MP-BGP: ${mpbgp?string('Yes', 'No')}</li>
</ul>
<a href="/manage">Back</a>
</body>
</html>