<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <title>${fn:escapeXml(renderContext.mainResource.node.displayableName)}</title>
</head>
<body>
<div class="bodywrapper">
    <div class="user-info">
        <c:choose>
            <c:when test="${renderContext.loggedIn}">
                <c:set var="firstName" value="${renderContext.user.properties['j:firstName']}"/>
                <c:set var="lastName" value="${renderContext.user.properties['j:lastName']}"/>
                <c:set var="email" value="${renderContext.user.properties['j:email']}"/>
                <div data-test="user-logged-in">
                    <p>Username:<span data-test="username">${fn:escapeXml(renderContext.user.username)}</span></p>
                    <p>First name:<span data-test="firstName">${firstName}</span></p>
                    <p>Last name:<span data-test="lastName">${lastName}</span></p>
                    <p>Email:<span data-test="email">${email}</span></p>
                </div>
            </c:when>
            <c:otherwise>
                <div data-test="user-guest">Guest</div>
            </c:otherwise>
        </c:choose>
    </div>

    <template:area path="pagecontent"/>
</div>
<c:if test="${renderContext.editMode}">
    <template:addResources type="css" resources="edit.css"/>
</c:if>
<template:theme/>
</body>
</html>
