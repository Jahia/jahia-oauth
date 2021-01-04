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

<template:addResources type="javascript" resources="i18n/linkedin-oauth-connector-i18n_${currentResource.locale}.js" var="i18nJSFile"/>
<c:if test="${empty i18nJSFile}">
    <template:addResources type="javascript" resources="i18n/linkedin-oauth-connector-i18n_en.js"/>
</c:if>

<template:addResources type="javascript" resources="linkedin-oauth-connector/linkedIn-controller.js"/>

<md-card ng-controller="LinkedInController as linkedIn">
    <div layout="row">
        <md-card-title flex>
            <md-card-title-text>
                <span class="md-headline" message-key="joant_linkedInOAuthView"></span>
            </md-card-title-text>
        </md-card-title>
        <div flex layout="row" layout-align="end center">
            <md-button class="md-icon-button" ng-click="linkedIn.toggleCard()">
                <md-tooltip md-direction="top">
                    <span message-key="joant_linkedInOAuthView.tooltip.toggleSettings"></span>
                </md-tooltip>
                <md-icon ng-show="!linkedIn.expandedCard">
                    keyboard_arrow_down
                </md-icon>
                <md-icon ng-show="linkedIn.expandedCard">
                    keyboard_arrow_up
                </md-icon>
            </md-button>
        </div>
    </div>

    <md-card-content layout="column" ng-show="linkedIn.expandedCard">
        <form name="linkedInForm">

            <md-switch ng-model="linkedIn.enabled">
                <span message-key="joant_linkedInOAuthView.label.activate"></span>
            </md-switch>

            <div layout="row">
                <md-input-container flex>
                    <label message-key="joant_linkedInOAuthView.label.apiKey"></label>
                    <input type="text" ng-model="linkedIn.apiKey" name="apiKey" required>
                    <div ng-messages="linkedInForm.apiKey.$error" role="alert">
                        <div ng-message="required" message-key="joant_linkedInOAuthView.error.apiKey.required"></div>
                    </div>
                </md-input-container>

                <div flex="5"></div>

                <md-input-container flex>
                    <label message-key="joant_linkedInOAuthView.label.apiSecret"></label>
                    <input type="text" ng-model="linkedIn.apiSecret" name="apiSecret" required>
                    <div ng-messages="linkedInForm.apiSecret.$error" role="alert">
                        <div ng-message="required" message-key="joant_linkedInOAuthView.error.apiSecret.required"></div>
                    </div>
                </md-input-container>
            </div>

            <div layout="row">
                <md-input-container class="md-block" flex>
                    <label message-key="joant_linkedInOAuthView.label.scope"></label>
                    <input type="text" ng-model="linkedIn.scope" name="scope">
                    <div class="hint" ng-show="!linkedInForm.scope.$invalid" message-key="joant_linkedInOAuthView.hint.scope"></div>
                    <div ng-messages="linkedInForm.scope.$error" role="alert">
                        <div ng-message="required" message-key="joant_linkedInOAuthView.error.scope.required"></div>
                    </div>
                </md-input-container>
            </div>

            <div layout="row">
                <md-input-container class="md-block" flex>
                    <label message-key="joant_linkedInOAuthView.label.callbackURL"></label>
                    <input type="url" ng-model="linkedIn.callbackUrl" name="callbackUrl">
                    <div class="hint" ng-show="linkedInForm.callbackUrl.$valid" message-key="joant_linkedInOAuthView.hint.callbackURL"></div>
                    <div ng-messages="linkedInForm.callbackUrl.$error" ng-show="linkedInForm.callbackUrl.$invalid" role="alert">
                        <div ng-message="url" message-key="joant_linkedInOAuthView.error.callbackURL.notAValidURL"></div>
                    </div>
                </md-input-container>
            </div>
        </form>

        <md-card-actions layout="row" layout-align="end center">
            <md-button class="md-accent" message-key="joant_linkedInOAuthView.label.mappers"
                       ng-click="linkedIn.goToMappers()"
                       ng-show="linkedIn.connectorHasSettings">
            </md-button>
            <md-button class="md-accent"message-key="joant_linkedInOAuthView.label.save"
                       ng-click="linkedIn.saveSettings()">
            </md-button>
        </md-card-actions>

    </md-card-content>
</md-card>