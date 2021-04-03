<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <#include "../style.ftlh">
    <title>Admin</title>
</head>
<body class="markdown-body">
<h1>Admin Portal</h1>
<p>You are logged in as: ${asn}.</p>
<p>dn42peering administrative tasks.</p>
<ul>
<li><a href="/admin/asn">ASN Management</a></li>
<li><a href="/admin/sudo">sudo</a></li>
</ul>
<h2>Registered ASNs and Peering</h2>
<p>Total ASN: ${asnTotal}</p>
<p>Total peers: ${peersTotal}</p>
<h2>Nodes</h2>
<a href="/admin/nodes/new">New Node</a>
<table style="width: 100%">
    <tr>
        <th>Name</th>
        <th>Public IP</th>
        <th>DN42 IP</th>
        <th>Internal IP</th>
        <th>Tunnel Methods</th>
        <th>Actions</th>
    </tr>
    <#list nodes as node>
    <tr>
        <td>${node.name}</td>
        <td>${node.publicIp}</td>
        <td>${node.dn42IP4}<br />${node.dn42IP6}<br />${node.dn42IP6NonLL}</td>
        <td>${node.internalIP}:${node.internalPort?long?c}</td>
        <td>
            <#list node.tunnelingMethods as method>
            ${method}<br />
            </#list>
        </td>
        <td><a href="/admin/nodes/edit?id=${node.id}">Edit</a><br />
        <a href="/admin/nodes/redeploy?id=${node.id}">Redeploy</a></td>
    </tr>
    </#list>
</table>
</body>
</html>