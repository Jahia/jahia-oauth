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

<template:addResources type="javascript" resources="google-oauth-connector/google-controller.js"/>

<md-card ng-controller="GoogleController as google">
    <div layout="row">
        <md-card-title flex>
            <md-card-title-text>
                <span class="md-headline" message-key="joant_googleOAuthView"></span>
            </md-card-title-text>
        </md-card-title>
        <div flex layout="row" layout-align="end center">
            <md-button class="md-icon-button" ng-click="google.toggleCard()">
                <md-tooltip md-direction="top">
                    <span message-key="joant_googleOAuthView.tooltip.toggleSettings"></span>
                </md-tooltip>
                <md-icon ng-show="!google.expandedCard">
                    keyboard_arrow_down
                </md-icon>
                <md-icon ng-show="google.expandedCard">
                    keyboard_arrow_up
                </md-icon>
            </md-button>
        </div>
    </div>

    <md-card-content layout="column" ng-show="google.expandedCard">
        <form name="googleForm">

            <md-switch ng-model="google.enabled">
                <span message-key="joant_googleOAuthView.label.activate"></span>
            </md-switch>

            <div layout="row">
                <md-input-container flex>
                    <label message-key="joant_googleOAuthView.label.apiKey"></label>
                    <input type="text" ng-model="google.apiKey" name="apiKey" required>
                    <div ng-messages="googleForm.apiKey.$error" role="alert">
                        <div ng-message="required" message-key="joant_googleOAuthView.error.apiKey.required"></div>
                    </div>
                </md-input-container>

                <div flex="5"></div>

                <md-input-container flex>
                    <label message-key="joant_googleOAuthView.label.apiSecret"></label>
                    <input type="text" ng-model="google.apiSecret" name="apiSecret" required>
                    <div ng-messages="googleForm.apiSecret.$error" role="alert">
                        <div ng-message="required" message-key="joant_googleOAuthView.error.apiSecret.required"></div>
                    </div>
                </md-input-container>
            </div>

            <div layout="row">
                <md-input-container class="md-block" flex>
                    <label message-key="joant_googleOAuthView.label.scope"></label>
                    <input type="text" ng-model="google.scope" name="scope">
                    <div class="hint" ng-show="!googleForm.scope.$invalid" message-key="joant_googleOAuthView.hint.scope"></div>
                    <div ng-messages="googleForm.scope.$error" role="alert">
                        <div ng-message="required" message-key="joant_googleOAuthView.error.scope.required"></div>
                    </div>
                </md-input-container>
            </div>

            <div layout="row">
                <md-input-container class="md-block" flex>
                    <label message-key="joant_googleOAuthView.label.callbackURL"></label>
                    <input type="url" ng-model="google.callbackUrl" name="callbackUrl">
                    <div class="hint" ng-show="googleForm.callbackUrl.$valid" message-key="joant_googleOAuthView.hint.callbackURL"></div>
                    <div ng-messages="googleForm.callbackUrl.$error" ng-show="googleForm.callbackUrl.$invalid" role="alert">
                        <div ng-message="url" message-key="joant_googleOAuthView.error.callbackURL.notAValidURL"></div>
                    </div>
                </md-input-container>
            </div>
        </form>

        <md-card-actions layout="row" layout-align="end center">
            <md-button class="md-accent" message-key="joant_googleOAuthView.label.mappers"
                       ng-click="google.goToMappers()"
                       ng-show="google.connectorHasSettings">
            </md-button>
            <md-button class="md-accent" message-key="joant_googleOAuthView.label.save"
                       ng-click="google.saveSettings()">
            </md-button>
        </md-card-actions>

    </md-card-content>
</md-card>