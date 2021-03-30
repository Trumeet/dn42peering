<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <#include "../style.ftlh">
    <title>Change Password | Manage your peering</title>
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
<form action="/manage/change-password" method="post">
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