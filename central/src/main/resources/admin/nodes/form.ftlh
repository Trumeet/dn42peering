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
    <label>Basic Information</label><br />
    <label for="asn">ASN:</label><br />
    <input type="text" id="asn" name="asn"
           placeholder="${asn}"
           pattern="[aA][sS]424242[0-9][0-9][0-9][0-9]"
           value="${(input_asn)!}"><br />
    <br />
    <label for="name">Name:</label><br />
    <input type="text" id="name" name="name" required
        placeholder="My Awesome Node"
           value="${(name)!}"><br />
    <br />
    <label for="notice">Notice (Optional, HTML enabled):</label><br />
    <input type="text" id="notice" name="notice"
        placeholder="<b>Hi!</b>"
           value="${(notice)!}"><br />
    <br />

    <label>DN42 Network Information</label><br />
    <label for="ipv4">DN42 IPv4 Address:</label><br />
    <input type="text" id="ipv4" name="ipv4" required
           placeholder="172.22.114.10"
           pattern="^172\.2[0-3](\.([1-9]?\d|[12]\d\d)){2}$"
           value="${(ipv4)!}"><br />
    <br />
    <label for="ipv6">Link Local IPv6 Address:</label><br />
    <input type="text" id="ipv6" name="ipv6" required
           placeholder="fe80::2980"
           value="${(ipv6)!}"><br />
    <br />
    <label for="ipv6_non_ll">DN42 IPv6 Address:</label><br />
    <input type="text" id="ipv6_non_ll" name="ipv6_non_ll" required
           placeholder="fd3f:a1f1:54ed::1"
           value="${(ipv6_non_ll)!}"><br />
    <br />

    <label>Public Network Information</label><br />
    <label for="public_ip">Public IP or Domain (No validation. For end user display only.):</label><br />
    <input type="text" id="public_ip" name="public_ip" required
           placeholder="tyo1.jp.dn42.yuuta.moe"
           value="${(public_ip)!}"><br />
    <br />

    <label>Management Network Information</label><br />
    <label for="internal_ip">Internal IP or domain (No validation. Type with care.):</label><br />
    <input type="text" id="internal_ip" name="internal_ip" required
           placeholder="192.168.10.1"
           value="${(internal_ip)!}"><br />
    <br />
    <label for="internal_port">Internal Port:</label><br />
    <input type="text" id="internal_port" name="internal_port"
           placeholder="49200"
           pattern="[0-9]+"
           value="${(internal_port?long?c)!}"><br />
    <br />

    <label>Tunneling Methods</label><br />
    <input type="checkbox" id="tunneling_method_wireguard"
    name="tunneling_method_wireguard"
    value="tunneling_method_wireguard"
    ${tunneling_method_wireguard?string('checked', '')}>
    <label for="tunneling_method_wireguard">Support WireGuard</label><br /><br />

    <input type="submit">
</form>