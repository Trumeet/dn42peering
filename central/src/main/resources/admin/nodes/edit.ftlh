<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <#include "/style.ftlh">
    <title>Edit Node | Admin</title>
</head>
<body class="markdown-body">
<h1>Edit Node</h1>
<p>You are logged in as: ${asn}.</p>
<p><b>Note: If you are switching to a new machine and had changed the internal IP, configurations on the old machine will not be removed. The new machine will be provisioned as usual.</b></p>
<#include "form.ftlh">
<h2>More Actions</h2>
<a href="/admin/nodes/delete?id=${id}">Delete</a>
</body>
</html>