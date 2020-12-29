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

<template:addResources type="javascript" resources="i18n/jahia-oauth-i18n_${currentResource.locale}.js" var="i18nJSFile"/>
<c:if test="${empty i18nJSFile}">
    <template:addResources type="javascript" resources="i18n/jahia-oauth-i18n_en.js"/>
</c:if>

<template:addResources type="javascript" resources="github-oauth-connector/github-controller.js"/>

<md-card ng-controller="GithubController as github">
    <div layout="row">
        <md-card-title flex>
            <md-card-title-text>
                <span class="md-headline" message-key="joant_githubOAuthView"></span>
            </md-card-title-text>
        </md-card-title>
        <div flex layout="row" layout-align="end center">
            <md-button class="md-icon-button" ng-click="github.toggleCard()">
                <md-tooltip md-direction="top">
                    <span message-key="joant_githubOAuthView.tooltip.toggleSettings"></span>
                </md-tooltip>
                <md-icon ng-show="!github.expandedCard">
                    keyboard_arrow_down
                </md-icon>
                <md-icon ng-show="github.expandedCard">
                    keyboard_arrow_up
                </md-icon>
            </md-button>
        </div>
    </div>

    <md-card-content layout="column" ng-show="github.expandedCard">
        <form name="githubForm">

            <md-switch ng-model="github.enabled">
                <span message-key="joant_githubOAuthView.label.activate"></span>
            </md-switch>

            <div layout="row">
                <md-input-container flex>
                    <label message-key="joant_githubOAuthView.label.apiKey"></label>
                    <input type="text" ng-model="github.apiKey" name="apiKey" required>
                    <div ng-messages="githubForm.apiKey.$error" role="alert">
                        <div ng-message="required" message-key="joant_githubOAuthView.error.apiKey.required"></div>
                    </div>
                </md-input-container>

                <div flex="5"></div>

                <md-input-container flex>
                    <label message-key="joant_githubOAuthView.label.apiSecret"></label>
                    <input type="text" ng-model="github.apiSecret" name="apiSecret" required>
                    <div ng-messages="githubForm.apiSecret.$error" role="alert">
                        <div ng-message="required" message-key="joant_githubOAuthView.error.apiSecret.required"></div>
                    </div>
                </md-input-container>
            </div>

            <div layout="row">
                <md-input-container class="md-block" flex>
                    <label message-key="joant_githubOAuthView.label.scope"></label>
                    <input type="text" ng-model="github.scope" name="scope">
                    <div class="hint" ng-show="!githubForm.scope.$invalid" message-key="joant_githubOAuthView.hint.scope"></div>
                    <div ng-messages="githubForm.scope.$error" role="alert">
                        <div ng-message="required" message-key="joant_githubOAuthView.error.scope.required"></div>
                    </div>
                </md-input-container>
            </div>

            <div layout="row">
                <md-input-container class="md-block" flex>
                    <label message-key="joant_githubOAuthView.label.callbackURL"></label>
                    <input type="url" ng-model="github.callbackUrl" name="callbackUrl">
                    <div class="hint" ng-show="githubForm.callbackUrl.$valid" message-key="joant_githubOAuthView.hint.callbackURL"></div>
                    <div ng-messages="githubForm.callbackUrl.$error" ng-show="githubForm.callbackUrl.$invalid" role="alert">
                        <div ng-message="url" message-key="joant_githubOAuthView.error.callbackURL.notAValidURL"></div>
                    </div>
                </md-input-container>
            </div>
        </form>

        <md-card-actions layout="row" layout-align="end center">
            <md-button class="md-accent" message-key="joant_githubOAuthView.label.mappers"
                       ng-click="github.goToMappers()"
                       ng-show="github.connectorHasSettings">
            </md-button>
            <md-button class="md-accent" message-key="joant_githubOAuthView.label.save"
                       ng-click="github.saveSettings()">
            </md-button>
        </md-card-actions>

    </md-card-content>
</md-card>
