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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Use Facebook credentials to connect to Jahia
 *
 * @author dgaillard
 */
@Component(service = { ConnectorService.class, OAuthConnectorService.class }, property = {
        JahiaAuthConstants.CONNECTOR_SERVICE_NAME + "=FacebookApi" }, immediate = true)
public class FacebookConnectorImpl extends Connector implements OAuthConnectorService {
    private static final Logger logger = LoggerFactory.getLogger(FacebookConnectorImpl.class);

    @Activate
    public void activate() {
        List<ConnectorPropertyInfo> properties = new ArrayList<>();

        properties.add(new ConnectorPropertyInfo("id", "string"));
        properties.add(new ConnectorPropertyInfo("name", "string"));
        properties.add(new ConnectorPropertyInfo("first_name", "string"));
        properties.add(new ConnectorPropertyInfo("last_name", "string"));
        properties.add(new ConnectorPropertyInfo("gender", "string"));

        ConnectorPropertyInfo birthday = new ConnectorPropertyInfo("birthday", "date");
        birthday.setValueFormat("MM/dd/yyyy");
        properties.add(birthday);

        properties.add(new ConnectorPropertyInfo("email", "email"));

        ConnectorPropertyInfo pictureUrl = new ConnectorPropertyInfo("pictureUrl", "string");
        pictureUrl.setPropertyToRequest("picture");
        pictureUrl.setValuePath("/data/url");
        properties.add(pictureUrl);

        ConnectorPropertyInfo locationName = new ConnectorPropertyInfo("locationName", "string");
        locationName.setPropertyToRequest("location");
        locationName.setValuePath("/name");
        properties.add(locationName);

        ConnectorPropertyInfo hometownName = new ConnectorPropertyInfo("hometownName", "string");
        hometownName.setPropertyToRequest("hometown");
        hometownName.setValuePath("/name");
        properties.add(hometownName);

        setAvailableProperties(properties);
    }

    @Override
    public String getProtectedResourceUrl(ConnectorConfig config) {
        List<String> urls = jahiaOAuthConfiguration.getFacebookUserInfoEndpoints();
        String userInfoEndpoint = urls.get(0);
        String urlWithProperties = userInfoEndpoint.concat(getAvailableProperties().stream()
                .map(property -> property.getPropertyToRequest() == null ? property.getName() : property.getPropertyToRequest()).distinct()
                .collect(Collectors.joining(",")));

        if (logger.isDebugEnabled()) {
            logger.debug("Protected Resource URL = {}", urlWithProperties);
        }
        return urlWithProperties;
    }

    @Reference
    @Override
    public void setJahiaOAuthConfiguration(JahiaOAuthConfiguration jahiaOAuthConfiguration) {
        super.setJahiaOAuthConfiguration(jahiaOAuthConfiguration);
    }
}
