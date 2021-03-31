<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <#include "../style.ftlh">
    <title>sudo | Admin</title>
</head>
<body class="markdown-body">
<h1>sudo</h1>
<p>You are logged in as: ${asn}.</p>
<p>Set a cookie that allows you to sudo as another ASN.</p>
<p>The target ASN will not be created if it does not exist.</p>
<p>The target ASN may not exist in the WHOIS database.</p>
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
<form action="/admin/sudo" method="post">
    <label for="asn">Target ASN:</label><br />
    <input type="text" id="asn" name="asn"
           placeholder="${asn}"
           value="${(target_asn)!}"><br />
    <br />
    <input type="submit">
</form>
</body>
</html>