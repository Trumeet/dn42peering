<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <#include "../style.ftlh">
    <title>Register your ASN</title>
</head>
<body class="markdown-body">
<h1>Your ASN is registered</h1>
<p>Please check your email for initial password. We had sent the password to the following emails addresses:</p>
<div>
    <ul>
        <#list emails as email>
        <li>${email}</li>
        </#list>
    </ul>
</div>
<p>Important: You may re-register at any time if you did not rececive the email. However, upon one successful login, you will not be able to re-register.</p>
<a href="/manage">Login Now</a> | <a href="/">Back to homepage</a>
</body>
</html>