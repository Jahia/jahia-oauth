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
<c:if test="${renderContext.user.name == 'guest' or renderContext.editMode or renderContext.request.getAttribute('ce_preview') != null}">

    <c:set var="cssClass" value="${currentNode.properties['cssClass'].string}"/>
    <c:set var="htmlId" value="${currentNode.properties['htmlId'].string}"/>
    <c:set var="tagType" value="${currentNode.properties['tagType'].string}"/>
    <template:addResources type="css" resources="button.css"/>

    <template:addResources>
        <script>
            function connectToFacebook${fn:replace(currentNode.identifier, '-', '')}() {
                var popup = window.open('', "Facebook Authorization", "menubar=no,status=no,scrollbars=no,width=1145,height=725,modal=yes,alwaysRaised=yes");
                var xhr = new XMLHttpRequest();
                xhr.open('GET', '<c:url value="${url.base}${renderContext.site.home.path}"/>.connectToFacebookAction.do');
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
            <button class="btn-light button-${currentNode.properties['buttonSize'].string} ${cssClass}"
                    onclick="connectToFacebook${fn:replace(currentNode.identifier, '-', '')}()"
                    <c:if test="${not empty htmlId}"> id="${htmlId}"</c:if>
                    <c:if test="${renderContext.editMode}">disabled</c:if> >
                <span class="icon-svg">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="88.428 12.828 107.543 207.085">
                        <path d="M158.232 219.912v-94.461h31.707l4.747-36.813h-36.454V65.134c0-10.658 2.96-17.922 18.245-17.922l19.494-.009V14.278c-3.373-.447-14.944-1.449-28.406-1.449-28.106 0-47.348 17.155-47.348 48.661v27.149H88.428v36.813h31.788v94.461l38.016-.001z"
                              fill="#3c5a9a"/>
                    </svg>
                </span>
                <p class="btn-text">${currentNode.displayableName}</p>
            </button>
        </c:when>
        <c:otherwise>
            <a href="#" style="color: transparent;" class="${cssClass}"
               onclick="connectToFacebook${fn:replace(currentNode.identifier, '-', '')}();return false;"
                    <c:if test="${not empty htmlId}"> id="${htmlId}"</c:if>
               <c:if test="${renderContext.editMode}">disabled</c:if> >
                 <span class="icon-svg">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="88.428 12.828 107.543 207.085">
                        <path d="M158.232 219.912v-94.461h31.707l4.747-36.813h-36.454V65.134c0-10.658 2.96-17.922 18.245-17.922l19.494-.009V14.278c-3.373-.447-14.944-1.449-28.406-1.449-28.106 0-47.348 17.155-47.348 48.661v27.149H88.428v36.813h31.788v94.461l38.016-.001z"
                              fill="#3c5a9a"/>
                    </svg>
                </span>
                <p class="btn-text">${currentNode.displayableName}</p>
            </a>
        </c:otherwise>
    </c:choose>
</c:if>
