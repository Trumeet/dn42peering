<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8" />
    <#include "../style.ftlh">
    <title>Register your ASN</title>
</head>
<body class="markdown-body">
<h1>Register your ASN</h1>
<p>You may register your ASN now. One ASN can have multiple peers with us.</p>
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
<form action="/asn" method="post">
    <label for="asn">ASN (You cannot change this after registration):</label><br />
    <input type="text" id="asn" name="asn" required
           placeholder="AS4242422980"
           pattern="[aA][sS]424242[0-9][0-9][0-9][0-9]"
           value="${input_asn}"><br />
    <br />

    <label for="submit">When you click the following button, we will send an email with initial password to your tech-c person (All possible emails). Make sure you have access to that!</label><br />
    <label for="submit">If you did not receive the email, you may contact us for manual password reset.</label><br />
    <input type="submit" id="submit">
</form>
</body>
</html>