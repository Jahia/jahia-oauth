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
    <c:url var="logoUrl" value="${url.currentModule}/images/linkedIn_logo.png"/>
    <template:addResources type="css" resources="joaLinkedInButton.css"/>

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
            <div class="joaLinkedInConnector joaLinkedInConnector-${currentNode.properties['buttonSize'].string} ${cssClass}">
                <div class="joaLinkedInConnector-logo">
                    <img src="${logoUrl}">
                </div>
                <button type="button" class="joaLinkedInConnector-button"
                        onclick="connectToLinkedIn${fn:replace(currentNode.identifier, '-', '')}()"
                        <c:if test="${not empty htmlId}"> id="${htmlId}"</c:if>
                        <c:if test="${renderContext.editMode}">disabled</c:if> >
                        ${currentNode.displayableName}
                </button>
            </div>
        </c:when>
        <c:otherwise>
            <a href="#" class="joaLinkedInConnector-link ${cssClass}"
               onclick="connectToLinkedIn${fn:replace(currentNode.identifier, '-', '')}();return false;"
                    <c:if test="${not empty htmlId}"> id="${htmlId}"</c:if>
               <c:if test="${renderContext.editMode}">disabled</c:if> >
                <div class="joaLinkedInConnector joaLinkedInConnector-${currentNode.properties['buttonSize'].string}">
                    <div class="joaLinkedInConnector-logo">
                        <img src="${logoUrl}">
                    </div>
                    <div class="joaLinkedInConnector-button">${currentNode.displayableName}</div>
                </div>
            </a>
        </c:otherwise>
    </c:choose>
