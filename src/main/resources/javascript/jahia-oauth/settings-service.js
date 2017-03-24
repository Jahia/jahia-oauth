(function() {
    'use strict';

    angular.module('JahiaOAuthApp').service('settingsService', settingsService);

    settingsService.$inject = ['$http', 'jahiaContext'];

    function settingsService($http, jahiaContext) {
        return {
            getConnectorData: getConnectorData,
            setConnectorData: setConnectorData,
            getConnectorProperties: getConnectorProperties,
            getMapperProperties: getMapperProperties,
            getMapperMapping: getMapperMapping,
            setMapperMapping: setMapperMapping
        };

        function getConnectorData(nodeName, properties) {
            var propertiesAsString = '';
            angular.forEach(properties, function(property) {
                propertiesAsString += '&properties=' + property;
            });
            return $http.get(jahiaContext.baseEdit + jahiaContext.sitePath + '.manageConnectorsSettingsAction.do?connectorServiceName=' + nodeName + propertiesAsString);
        }

        function setConnectorData(data) {
            return $http({
                method: 'POST',
                url: jahiaContext.baseEdit + jahiaContext.sitePath + '.manageConnectorsSettingsAction.do',
                params: data
            });
        }

        function getConnectorProperties(data) {
            data.action = 'getConnectorProperties';
            return $http({
                method: 'POST',
                url: jahiaContext.baseEdit + jahiaContext.sitePath + '.manageMappersAction.do',
                params: data
            })
        }

        function getMapperProperties(data) {
            data.action = 'getMapperProperties';
            return $http({
                method: 'POST',
                url: jahiaContext.baseEdit + jahiaContext.sitePath + '.manageMappersAction.do',
                params: data
            })
        }

        function getMapperMapping(data) {
            data.action = 'getMapperMapping';
            return $http({
                method: 'POST',
                url: jahiaContext.baseEdit + jahiaContext.sitePath + '.manageMappersAction.do',
                params: data
            })
        }

        function setMapperMapping(data) {
            data.action = 'setMapperMapping';
            return $http({
                method: 'POST',
                url: jahiaContext.baseEdit + jahiaContext.sitePath + '.manageMappersAction.do',
                params: data
            })
        }
    }
})();