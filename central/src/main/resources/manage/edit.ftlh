<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <#include "../style.ftlh">
    <title>Edit Peer | Manage your peering</title>
</head>
<body class="markdown-body">
<h1>Edit Peer</h1>
<p>You are logged in as: ${asn}.</p>
<#include "form.ftlh">
<h2>More Actions</h2>
<a href="/manage/delete?id=${id}">Delete</a>
</body>
</html>