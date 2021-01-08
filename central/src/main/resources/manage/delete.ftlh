<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Delete Account | Manage your peering</title>
</head>
<body>
<h1>Delete Account</h1>
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
<form action="/manage/delete-account" method="post">
    <label for="submit">Are you sure to delete your account? This will delete the ASN record on this node.</label><br />
    <input type="submit" id="submit">
</form>
</body>
</html>