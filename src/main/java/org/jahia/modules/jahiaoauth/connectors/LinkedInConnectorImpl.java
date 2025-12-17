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
 * Auth linkedin connector
 * Use linkedin credentials to connect to Jahia
 *
 * @author dgaillard
 */
@Component(service = { ConnectorService.class, OAuthConnectorService.class }, property = {
        JahiaAuthConstants.CONNECTOR_SERVICE_NAME + "=LinkedInApi20" }, immediate = true)
public class LinkedInConnectorImpl extends Connector implements OAuthConnectorService {
    private static final Logger logger = LoggerFactory.getLogger(LinkedInConnectorImpl.class);

    @Activate
    public void activate() {
        List<ConnectorPropertyInfo> properties = new ArrayList<>();

        properties.add(new ConnectorPropertyInfo("id", "string"));

        properties.add(new ConnectorPropertyInfo("localizedFirstName", "string"));

        properties.add(new ConnectorPropertyInfo("localizedLastName", "string"));

        ConnectorPropertyInfo pictureUrl = new ConnectorPropertyInfo("pictureUrl", "string");
        pictureUrl.setPropertyToRequest("profilePicture(displayImage~:playableStreams)");
        pictureUrl.setValuePath("/profilePicture/displayImage~/elements[0]/identifiers[0]/identifier");
        properties.add(pictureUrl);

        ConnectorPropertyInfo emailAddress = new ConnectorPropertyInfo("emailAddress", "email");
        emailAddress.setValuePath("[0]/handle~/emailAddress");
        emailAddress.setPropertyToRequest("elements");
        properties.add(emailAddress);

        setAvailableProperties(properties);
    }

    @Override
    public String getProtectedResourceUrl(ConnectorConfig config) {
        return null; // never called
    }

    @Override
    public List<String> getProtectedResourceUrls(ConnectorConfig config) {
        List<String> urls = jahiaOAuthConfiguration.getLinkedInUserInfoEndpoints();
        return urls.stream().map(this::getProtectedResourceUrl).collect(Collectors.toList());
    }

    private String getProtectedResourceUrl(String protectedResourceUrl) {
        final String properties = getAvailableProperties().stream()
                .map(property -> property.getPropertyToRequest() == null ? property.getName() : property.getPropertyToRequest()).distinct()
                .collect(Collectors.joining(","));
        String urlWithProperties = String.format(protectedResourceUrl, properties);
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
