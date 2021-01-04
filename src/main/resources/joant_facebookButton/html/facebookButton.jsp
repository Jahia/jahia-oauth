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
    <template:addResources type="css" resources="joaFacebookButton.css"/>

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
            <button class="joaFacebookConnector joaFacebookConnector-${currentNode.properties['buttonSize'].string} ${cssClass}"
                    type="button"
                    onclick="connectToFacebook${fn:replace(currentNode.identifier, '-', '')}()"
                    <c:if test="${not empty htmlId}"> id="${htmlId}"</c:if>
                    <c:if test="${renderContext.editMode}">disabled</c:if> >
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 216 216" class="_55a2">
                    <path fill="white" d="
                        M204.1 0H11.9C5.3 0 0 5.3 0 11.9v192.2c0 6.6 5.3 11.9 11.9
                        11.9h103.5v-83.6H87.2V99.8h28.1v-24c0-27.9 17-43.1 41.9-43.1
                        11.9 0 22.2.9 25.2 1.3v29.2h-17.3c-13.5 0-16.2 6.4-16.2
                        15.9v20.8h32.3l-4.2 32.6h-28V216h55c6.6 0 11.9-5.3
                        11.9-11.9V11.9C216 5.3 210.7 0 204.1 0z"></path>
                </svg>
                <span>${currentNode.displayableName}</span>
            </button>
        </c:when>
        <c:otherwise>
            <a href="#" style="color: transparent;" class="${cssClass}"
               onclick="connectToFacebook${fn:replace(currentNode.identifier, '-', '')}();return false;"
                <c:if test="${not empty htmlId}"> id="${htmlId}"</c:if>
                <c:if test="${renderContext.editMode}">disabled</c:if> >
                <div class="joaFacebookConnector joaFacebookConnector-${currentNode.properties['buttonSize'].string}">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 216 216" class="_55a2">
                        <path fill="white" d="
                            M204.1 0H11.9C5.3 0 0 5.3 0 11.9v192.2c0 6.6 5.3 11.9 11.9
                            11.9h103.5v-83.6H87.2V99.8h28.1v-24c0-27.9 17-43.1 41.9-43.1
                            11.9 0 22.2.9 25.2 1.3v29.2h-17.3c-13.5 0-16.2 6.4-16.2
                            15.9v20.8h32.3l-4.2 32.6h-28V216h55c6.6 0 11.9-5.3
                            11.9-11.9V11.9C216 5.3 210.7 0 204.1 0z"></path>
                    </svg>
                    <span>${currentNode.displayableName}</span>
                </div>
            </a>
        </c:otherwise>
    </c:choose>
