<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <#include "/style.ftlh">
    <title>Change Password | Admin</title>
</head>
<body class="markdown-body">
<h1>Change Password</h1>
<p>You are logged in as: ${asn}.</p>
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
<form action="/admin/asn/change-password" method="post">
    <label for="asn">New Password:</label><br />
    <input type="asn" id="asn" name="asn" required
           placeholder="${asn}"
           value="${(target_asn)!}"><br />
    <br />
    <label for="passwd">New Password:</label><br />
    <input type="password" id="passwd" name="passwd" required
           placeholder="p@ssw0rd!"><br />
    <br />
    <label for="confirm">Confirm Password:</label><br />
    <input type="password" id="confirm" name="confirm" required
           placeholder="p@ssw0rd!"><br />
    <br />
    <input type="submit" id="submit">
</form>
</body>
</html>