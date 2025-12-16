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
package org.jahia.modules.jahiaoauth.connectors;

import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.ConnectorPropertyInfo;
import org.jahia.modules.jahiaauth.service.ConnectorService;
import org.jahia.modules.jahiaauth.service.JahiaAuthConstants;
import org.jahia.modules.jahiaoauth.config.JahiaOAuthConfiguration;
import org.jahia.modules.jahiaoauth.service.OAuthConnectorService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(service = { ConnectorService.class, OAuthConnectorService.class }, property = {
        JahiaAuthConstants.CONNECTOR_SERVICE_NAME + "=FranceConnectApi" }, immediate = true)
public class FranceConnectConnectorImpl extends Connector implements OAuthConnectorService {

    private Map<String, String> mapProtectedResourceUrl;

    @Activate
    public void activate() {
        // Initialize the protected resource URL map
        mapProtectedResourceUrl = new HashMap<>();
        mapProtectedResourceUrl.put("FranceConnectApi", "https://app.franceconnect.gouv.fr/api/v1/userinfo");
        mapProtectedResourceUrl.put("FranceConnectApiDev", "https://fcp.integ01.dev-franceconnect.fr/api/v1/userinfo");

        List<ConnectorPropertyInfo> properties = new ArrayList<>();

        ConnectorPropertyInfo id = new ConnectorPropertyInfo("id", "string");
        id.setPropertyToRequest("sub");
        properties.add(id);

        properties.add(new ConnectorPropertyInfo("given_name", "string"));
        properties.add(new ConnectorPropertyInfo("family_name", "string"));
        properties.add(new ConnectorPropertyInfo("gender", "string"));

        ConnectorPropertyInfo birthdate = new ConnectorPropertyInfo("birthdate", "date");
        birthdate.setValueFormat("yyyy-MM-dd");
        properties.add(birthdate);

        properties.add(new ConnectorPropertyInfo("email", "email"));

        setAvailableProperties(properties);
    }

    @Override
    public String getProtectedResourceUrl(ConnectorConfig config) {
        return mapProtectedResourceUrl
                .get(config.getProperty("oauthApiName") != null ? config.getProperty("oauthApiName") : config.getConnectorName());
    }

    @Reference
    @Override
    public void setJahiaOAuthConfiguration(JahiaOAuthConfiguration jahiaOAuthConfiguration) {
        super.setJahiaOAuthConfiguration(jahiaOAuthConfiguration);
    }
}
