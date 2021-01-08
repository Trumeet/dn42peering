<#if errors??>
<div>
    <p style="color:red">Errors in the previous form:</p>
    <ul>
        <#list errors as error>
        <li>${error}</li>
    </#list>
    </ul>
</div>
</#if>
<form action="${action}" method="post">
    <label for="ipv4">dn42 IPv4 Address:</label><br />
    <input type="text" id="ipv4" name="ipv4" required
           placeholder="172.22.114.10"
           pattern="^172\.2[0-3](\.([1-9]?\d|[12]\d\d)){2}$"
           value="${(ipv4)!}"><br />
    <br />
    <label for="ipv6">dn42 IPv6 Address (Optional):</label><br />
    <input type="text" id="ipv6" name="ipv6"
           placeholder="fe80::2980"
           value="${(ipv6)!}"><br />
    <br />
    <input type="checkbox" id="mpbgp" name="mpbgp" value="mpbgp" ${mpbgp?string('checked', '')}>
    <label for="mpbgp">Enable MP-BGP (Only works when you provide a IPv6 address)</label><br /><br />

    <label>Node:</label><br />
    <#list nodes as node>
        <input type="radio" id="node_${node.id}" name="node" value="${node.id}" required ${(node_checked == node.id)?string('checked', '')}>
        <label for="node_${node.id}">${node.name} (${node.public_ip}, ${node.asn}, ${node.vpn_types?join(",")})
            <#if node.notice??><br />${node.notice?no_esc}</#if>
        </label><br />
    </#list>
    <br />

    <label>Tunneling Method:</label><br />
    <input type="radio" id="wg" name="vpn" value="wg" required ${typeWireguard?string('checked', '')}>
    <label for="wg">WireGuard</label><br />
    <br />

    <label for="wg_endpoint"><b>WireGuard Specific Settings:</b></label><br />
    <label for="wg_endpoint">WireGuard Endpoint IP (Optional if you do not have a public IPv4 address):</label><br />
    <input type="text" id="wg_endpoint" name="wg_endpoint"
           placeholder="114.51.4.191"
           pattern="[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+"
           value="${(wgEndpoint)!}"><br />
    <br />
    <label for="wg_endpoint_port">WireGuard Endpoint Port (Optional if you do not have a public IPv4 address):</label><br />
    <input type="text" id="wg_endpoint_port" name="wg_endpoint_port"
           placeholder="19198"
           pattern="[0-9]+"
           value="${(wgEndpointPort?long?c)!}"><br />
    <br />
    <label for="wg_pubkey">WireGuard Public Key:</label><br />
    <input type="text" id="wg_pubkey" name="wg_pubkey" required
           placeholder="CdX7EQwezTPaVjhp1Kw29RsmYYN3nTjPxOln4WaPOAU="
           value="${(wgPubkey)!}"><br />
    <br />

    <input type="submit">
</form>