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

<template:addResources type="javascript" resources="franceconnect-oauth-connector/franceConnect-controller.js"/>

<md-card ng-controller="FranceConnectController as franceConnectCtrl">
    <div layout="row">
        <md-card-title flex>
            <md-card-title-text>
                <span class="md-headline" message-key="joant_franceConnectOAuthView"></span>
            </md-card-title-text>
        </md-card-title>
        <div flex layout="row" layout-align="end center">
            <md-button class="md-icon-button" ng-click="franceConnectCtrl.toggleCard()">
                <md-tooltip md-direction="top">
                    <span message-key="joant_franceConnectOAuthView.tooltip.toggleSettings"></span>
                </md-tooltip>
                <md-icon ng-show="!franceConnectCtrl.expandedCard">
                    keyboard_arrow_down
                </md-icon>
                <md-icon ng-show="franceConnectCtrl.expandedCard">
                    keyboard_arrow_up
                </md-icon>
            </md-button>
        </div>
    </div>

    <md-card-content layout="column" ng-show="franceConnectCtrl.expandedCard">
        <form name="franceConnectForm">
            <div layout="row">
                <md-switch ng-model="franceConnectCtrl.enabled">
                    <span message-key="joant_franceConnectOAuthView.label.activate"></span>
                </md-switch>

                <md-switch ng-model="franceConnectCtrl.dev" ng-change="franceConnectCtrl.switchDev()">
                    <span message-key="joant_franceConnectOAuthView.label.dev"></span>
                </md-switch>
            </div>

            <div layout="row">
                <md-input-container flex>
                    <label message-key="joant_franceConnectOAuthView.label.apiKey"></label>
                    <input type="text" ng-model="franceConnectCtrl.apiKey" name="apiKey" required ng-disabled="franceConnectCtrl.dev">
                    <div ng-messages="franceConnectForm.apiKey.$error" role="alert">
                        <div ng-message="required" message-key="joant_franceConnectOAuthView.error.apiKey.required"></div>
                    </div>
                </md-input-container>

                <div flex="5"></div>

                <md-input-container flex>
                    <label message-key="joant_franceConnectOAuthView.label.apiSecret"></label>
                    <input type="text" ng-model="franceConnectCtrl.apiSecret" name="apiSecret" required ng-disabled="franceConnectCtrl.dev">
                    <div ng-messages="franceConnectForm.apiSecret.$error" role="alert">
                        <div ng-message="required" message-key="joant_franceConnectOAuthView.error.apiSecret.required"></div>
                    </div>
                </md-input-container>
            </div>

            <div layout="row">
                <md-input-container class="md-block" flex>
                    <label message-key="joant_franceConnectOAuthView.label.scope"></label>
                    <input type="text" ng-model="franceConnectCtrl.scope" name="scope">
                    <div class="hint" ng-show="!franceConnectCtrl.scope.$invalid" message-key="joant_franceConnectOAuthView.hint.scope"></div>
                    <div ng-messages="franceConnectForm.scope.$error" role="alert">
                        <div ng-message="required" message-key="joant_franceConnectOAuthView.error.scope.required"></div>
                    </div>
                </md-input-container>
            </div>

            <div layout="row">
                <md-input-container class="md-block" flex>
                    <label message-key="joant_franceConnectOAuthView.label.callbackURL"></label>
                    <input type="url" ng-model="franceConnectCtrl.callbackUrl" name="callbackUrl" ng-disabled="franceConnectCtrl.dev">
                    <div class="hint" ng-show="franceConnectForm.callbackUrl.$valid" message-key="joant_franceConnectOAuthView.hint.callbackURL"></div>
                    <div ng-messages="franceConnectForm.callbackUrl.$error" ng-show="franceConnectForm.callbackUrl.$invalid" role="alert">
                        <div ng-message="url" message-key="joant_franceConnectOAuthView.error.callbackURL.notAValidURL"></div>
                    </div>
                </md-input-container>
            </div>
        </form>

        <md-card-actions layout="row" layout-align="end center">
            <md-button class="md-accent" message-key="joant_franceConnectOAuthView.label.mappers"
                       ng-click="franceConnectCtrl.goToMappers()"
                       ng-show="franceConnectCtrl.connectorHasSettings">
            </md-button>
            <md-button class="md-accent" message-key="joant_franceConnectOAuthView.label.save"
                       ng-click="franceConnectCtrl.saveSettings()">
            </md-button>
        </md-card-actions>

    </md-card-content>
</md-card>