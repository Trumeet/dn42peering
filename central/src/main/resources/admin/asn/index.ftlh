<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <#include "/style.ftlh">
    <title>ASN | Admin</title>
</head>
<body class="markdown-body">
<h1>ASN Management</h1>
<table style="width: 100%">
    <tr>
        <th>ASN</th>
        <th>Activated</th>
        <th>Actions</th>
    </tr>
    <#list asns as asn>
    <tr>
        <td>${asn.asn}</td>
        <td>${asn.activated?string('Yes', 'No')}</td>
        <td><a href="/admin/asn/change-password?asn=${asn.asn}">Change Password</a></td>
    </tr>
    </#list>
</table>
</body>
</html>