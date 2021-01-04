<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="ui" uri="http://www.jahia.org/tags/uiComponentsLib" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="s" uri="http://www.jahia.org/tags/search" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<c:set var="cssClass" value="${currentNode.properties['cssClass'].string}"/>
<c:set var="htmlId" value="${currentNode.properties['htmlId'].string}"/>
<c:set var="tagType" value="${currentNode.properties['tagType'].string}"/>
<template:addResources type="css" resources="joaFontRoboto.css"/>
<template:addResources type="css" resources="joaLinkedInButton.unified.css"/>

<template:addResources>
    <script>
        function connectToLinkedIn${fn:replace(currentNode.identifier, '-', '')}() {
            var popup = window.open('', "LinkedIn Authorization", "menubar=no,status=no,scrollbars=no,width=1145,height=725,modal=yes,alwaysRaised=yes");
            var xhr = new XMLHttpRequest();
            xhr.open('GET', '<c:url value="${url.base}${renderContext.site.home.path}"/>.connectToLinkedInAction.do');
            xhr.setRequestHeader('Accept', 'application/json;');
            xhr.send();

            xhr.onreadystatechange = function () {
                if (xhr.readyState != 4 || xhr.status != 200) return;
                var json = JSON.parse(xhr.responseText);
                popup.location.href = json.authorizationUrl;
                window.addEventListener('message', function (event) {
                    if (event.data.authenticationIsDone) {
                        setTimeout(function () {
                            popup.close();
                            if (event.data.isAuthenticate) {
                                window.location.search = 'site=${renderContext.site.siteKey}';
                            }
                        }, 3000);
                    }
                });
            };
        }
    </script>
</template:addResources>

<c:choose>
    <c:when test="${tagType eq 'button'}">
        <button class="linkedin-btn custom-btn-theme ${cssClass}" type="button"
                onclick="connectToLinkedIn${fn:replace(currentNode.identifier, '-', '')}()"
                <c:if test="${not empty htmlId}"> id="${htmlId}"</c:if>
                <c:if test="${renderContext.editMode}">disabled</c:if> >
                <span class="custom-icon-svg">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><defs><style>.cls-1{fill:#fff}</style></defs><title>icon-linkedin</title><g id="icon-linkedin"><g id="linkedin"><g id="icon-linkedin-2" data-name="icon-linkedin"><g id="logo"><path id="path28" class="cls-1" d="M9.08 39.12v-25.8H.51v25.8zM4.79 9.8c3 0 4.85-2 4.85-4.46A4.46 4.46 0 0 0 4.85.88C1.92.88 0 2.81 0 5.34A4.44 4.44 0 0 0 4.74 9.8h.06z"/><path id="path30" class="cls-1" d="M13.83 39.12h8.57V24.71a5.88 5.88 0 0 1 .28-2.09 4.69 4.69 0 0 1 4.4-3.14c3.1 0 4.34 2.37 4.34 5.83v13.8H40V24.32c0-7.92-4.23-11.61-9.87-11.61a8.54 8.54 0 0 0-7.78 4.35h.06v-3.74h-8.58c.11 2.42 0 25.8 0 25.8z"/></g></g></g></g></svg>
                </span>
            <p class="btn-text">${currentNode.displayableName}</p>
        </button>
    </c:when>
    <c:otherwise>
        <a href="#" class="linkedin-btn custom-btn-theme ${cssClass}"
           onclick="connectToLinkedIn${fn:replace(currentNode.identifier, '-', '')}();return false;"
                <c:if test="${not empty htmlId}"> id="${htmlId}"</c:if>
           <c:if test="${renderContext.editMode}">disabled</c:if> >
                <span class="custom-icon-svg">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><defs><style>.cls-1{fill:#fff}</style></defs><title>icon-linkedin</title><g id="icon-linkedin"><g id="linkedin"><g id="icon-linkedin-2" data-name="icon-linkedin"><g id="logo"><path id="path28" class="cls-1" d="M9.08 39.12v-25.8H.51v25.8zM4.79 9.8c3 0 4.85-2 4.85-4.46A4.46 4.46 0 0 0 4.85.88C1.92.88 0 2.81 0 5.34A4.44 4.44 0 0 0 4.74 9.8h.06z"/><path id="path30" class="cls-1" d="M13.83 39.12h8.57V24.71a5.88 5.88 0 0 1 .28-2.09 4.69 4.69 0 0 1 4.4-3.14c3.1 0 4.34 2.37 4.34 5.83v13.8H40V24.32c0-7.92-4.23-11.61-9.87-11.61a8.54 8.54 0 0 0-7.78 4.35h.06v-3.74h-8.58c.11 2.42 0 25.8 0 25.8z"/></g></g></g></g></svg>
                </span>
            <p class="btn-text">${currentNode.displayableName}</p>
        </a>
    </c:otherwise>
</c:choose>
