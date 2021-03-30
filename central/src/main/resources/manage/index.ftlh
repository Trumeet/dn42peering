<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <#include "../style.ftlh">
    <title>Manage your peering</title>
</head>
<body class="markdown-body">
<h1>Manage your peering</h1>
<p>You are logged in as: ${asn}.</p>
<h2>Peers</h2>
<p><a href="/manage/new">New Peer</a></p>
<table style="width: 100%">
    <tr>
        <th>IPv4</th>
        <th>IPv6</th>
        <th>Method</th>
        <th>Provision</th>
        <th>Actions</th>
    </tr>
    <#list peers as peer>
    <tr>
        <td>${peer.ipv4}</td>
        <td><#if peer.ipv6??>${peer.ipv6}<#else>None</#if></td>
        <td>${peer.type}</td>
        <td>${peer.provisionStatus}</td>
        <td><a href="/manage/edit?id=${peer.id}">Edit</a> |
        <a href="/manage/show-configuration?id=${peer.id}">Example Conf</a></td>
    </tr>
    </#list>
</table>
<h2>More Actions</h2>
<a href="/manage/change-password">Change Password</a> |
<a href="/manage/delete-account">Delete My Account</a>
</body>
</html>