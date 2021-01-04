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

<template:addResources type="javascript" resources="i18n/facebook-oauth-connector-i18n_${currentResource.locale}.js" var="i18nJSFile"/>
<c:if test="${empty i18nJSFile}">
    <template:addResources type="javascript" resources="i18n/facebook-oauth-connector-i18n_en.js"/>
</c:if>

<template:addResources type="javascript" resources="facebook-oauth-connector/facebook-controller.js"/>

<md-card ng-controller="FacebookController as facebook">
    <div layout="row">
        <md-card-title flex>
            <md-card-title-text>
                <span class="md-headline" message-key="joant_facebookOAuthView"></span>
            </md-card-title-text>
        </md-card-title>
        <div flex layout="row" layout-align="end center">
            <md-button class="md-icon-button" ng-click="facebook.toggleCard()">
                <md-tooltip md-direction="top">
                    <span message-key="joant_facebookOAuthView.tooltip.toggleSettings"></span>
                </md-tooltip>
                <md-icon ng-show="!facebook.expandedCard">
                    keyboard_arrow_down
                </md-icon>
                <md-icon ng-show="facebook.expandedCard">
                    keyboard_arrow_up
                </md-icon>
            </md-button>
        </div>
    </div>

    <md-card-content layout="column" ng-show="facebook.expandedCard">
        <form name="facebookForm">

            <md-switch ng-model="facebook.enabled">
                <span message-key="joant_facebookOAuthView.label.activate"></span>
            </md-switch>

            <div layout="row">
                <md-input-container flex>
                    <label message-key="joant_facebookOAuthView.label.apiKey"></label>
                    <input type="text" ng-model="facebook.apiKey" name="apiKey" required>
                    <div ng-messages="facebookForm.apiKey.$error" role="alert">
                        <div ng-message="required" message-key="joant_facebookOAuthView.error.apiKey.required"></div>
                    </div>
                </md-input-container>

                <div flex="5"></div>

                <md-input-container flex>
                    <label message-key="joant_facebookOAuthView.label.apiSecret"></label>
                    <input type="text" ng-model="facebook.apiSecret" name="apiSecret" required>
                    <div ng-messages="facebookForm.apiSecret.$error" role="alert">
                        <div ng-message="required" message-key="joant_facebookOAuthView.error.apiSecret.required"></div>
                    </div>
                </md-input-container>
            </div>

            <div layout="row">
                <md-input-container class="md-block" flex>
                    <label message-key="joant_facebookOAuthView.label.scope"></label>
                    <input type="text" ng-model="facebook.scope" name="scope">
                    <div class="hint" ng-show="!facebookForm.scope.$invalid" message-key="joant_facebookOAuthView.hint.scope"></div>
                    <div ng-messages="facebookForm.scope.$error" role="alert">
                        <div ng-message="required" message-key="joant_facebookOAuthView.error.scope.required"></div>
                    </div>
                </md-input-container>
            </div>

            <div layout="row">
                <md-input-container class="md-block" flex>
                    <label message-key="joant_facebookOAuthView.label.callbackURL"></label>
                    <input type="url" ng-model="facebook.callbackUrl" name="callbackUrl">
                    <div class="hint" ng-show="facebookForm.callbackUrl.$valid" message-key="joant_facebookOAuthView.hint.callbackURL"></div>
                    <div ng-messages="facebookForm.callbackUrl.$error" ng-show="facebookForm.callbackUrl.$invalid" role="alert">
                        <div ng-message="url" message-key="joant_facebookOAuthView.error.callbackURL.notAValidURL"></div>
                    </div>
                </md-input-container>
            </div>
        </form>

        <md-card-actions layout="row" layout-align="end center">
            <md-button class="md-accent" message-key="joant_facebookOAuthView.label.mappers"
                       ng-click="facebook.goToMappers()"
                       ng-show="facebook.connectorHasSettings">
            </md-button>
            <md-button class="md-accent" message-key="joant_facebookOAuthView.label.save"
                       ng-click="facebook.saveSettings()">
            </md-button>
        </md-card-actions>

    </md-card-content>
</md-card>