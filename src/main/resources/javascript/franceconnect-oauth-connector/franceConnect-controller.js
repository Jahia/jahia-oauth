/*
 * Copyright (C) 2002-2021 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function() {
    'use strict';

    angular.module('JahiaOAuthApp').controller('FranceConnectController', FranceConnectController);

    FranceConnectController.$inject = ['$location', 'settingsService', 'helperService', 'i18nService', 'jahiaContext'];

    function FranceConnectController ($location, settingsService, helperService, i18nService, jahiaContext) {
        var vm = this;

        // Variables
        vm.expandedCard = false;
        vm.callbackUrl = '';

        // Functions
        vm.saveSettings = saveSettings;
        vm.goToMappers = goToMappers;
        vm.toggleCard = toggleCard;
        vm.switchDev = switchDev;

        init();

        function saveSettings() {
            // Value can't be empty
            if (!vm.apiKey
                || !vm.apiSecret
                || !vm.callbackUrl) {
                helperService.errorToast(i18nService.message('joant_franceConnectOAuthView.message.error.missingMandatoryProperties'));
                return false;
            }

            // the node name here must be the same as the one in your spring file
            settingsService.setConnectorData({
                connectorServiceName: 'FranceConnectApi',
                properties: {
                    enabled: vm.enabled,
                    apiKey: vm.apiKey,
                    apiSecret: vm.apiSecret,
                    oauthApiName: vm.dev ? 'FranceConnectApiDev' : 'FranceConnectApi',
                    callbackUrl: vm.callbackUrl,
                    scope: vm.scope
                }
            }).success(function() {
                vm.connectorHasSettings = true;
                helperService.successToast(i18nService.message('joant_franceConnectOAuthView.message.succes.saveSuccess'));
            }).error(function(data) {
                helperService.errorToast(i18nService.message('joant_franceConnectOAuthView.message.label') + ' ' + data.error);
                console.log(data);
            });
        }

        function goToMappers() {
            // the second part of the path must be the service name
            $location.path('/mappers/FranceConnectApi');
        }

        function toggleCard() {
            vm.expandedCard = !vm.expandedCard;
        }

        function switchDev() {
            console.log(vm.dev);
            if (vm.dev) {
                vm.nonDev = {
                    apiKey: vm.apiKey,
                    apiSecret: vm.apiSecret,
                    callbackUrl: vm.callbackUrl
                }
                vm.apiKey = '211286433e39cce01db448d80181bdfd005554b19cd51b3fe7943f6b3b86ab6e'
                vm.apiSecret = '2791a731e6a59f56b6b4dd0d08c9b1f593b5f3658b9fd731cb24248e2669af4b'
                vm.callbackUrl = 'http://localhost:8080/callback?url=/cms/render/live/en' + jahiaContext.sitePath + "/home.franceConnectOAuthCallbackAction.do"
            } else {
                vm.apiKey = vm.nonDev ? vm.nonDev.apiKey : '';
                vm.apiSecret = vm.nonDev ? vm.nonDev.apiSecret : '';
                vm.callbackUrl = vm.nonDev ? vm.nonDev.callbackUrl : '';
            }
        }

        function init() {
            i18nService.addKey(oauthi18n);
            vm.siteKey = jahiaContext.siteKey;

            settingsService.getConnectorData('FranceConnectApi', ['enabled', 'oauthApiName', 'apiKey', 'apiSecret', 'callbackUrl', 'scope']).success(function(data) {
                if (data && !angular.equals(data, { })) {
                    console.log(data);
                    vm.connectorHasSettings = true;
                    vm.enabled = data.enabled;
                    vm.apiKey = data.apiKey;
                    vm.apiSecret = data.apiSecret;
                    vm.callbackUrl = data.callbackUrl;
                    vm.scope = data.scope;
                    vm.expandedCard = true;
                    vm.dev = data.oauthApiName === 'FranceConnectApiDev';
                } else {
                    vm.connectorHasSettings = false;
                    vm.enabled = false;
                    vm.scope = 'openid profile birth';
                }
            }).error(function(data) {
                helperService.errorToast(i18nService.message('joant_franceConnectOAuthView.message.label') + ' ' + data.error);
            });
        }
    }
})();
