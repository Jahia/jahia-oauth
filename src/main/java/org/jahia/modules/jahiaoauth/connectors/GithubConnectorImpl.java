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
import java.util.List;

@Component(service = { ConnectorService.class, OAuthConnectorService.class }, property = {
        JahiaAuthConstants.CONNECTOR_SERVICE_NAME + "=GitHubApi" }, immediate = true)
public class GithubConnectorImpl extends Connector implements OAuthConnectorService {

    @Override
    public String getProtectedResourceUrl(ConnectorConfig config) {
        List<String> urls = jahiaOAuthConfiguration.getGitHubUserInfoEndpoints();
        return urls.get(0);
    }

    @Activate
    public void activate() {
        List<ConnectorPropertyInfo> properties = new ArrayList<>();

        properties.add(new ConnectorPropertyInfo("id", "string"));

        properties.add(new ConnectorPropertyInfo("name", "string"));

        ConnectorPropertyInfo profilePictureUrl = new ConnectorPropertyInfo("profilePictureUrl", "string");
        profilePictureUrl.setPropertyToRequest("avatar_url");
        properties.add(profilePictureUrl);

        setAvailableProperties(properties);
    }

    @Reference
    @Override
    public void setJahiaOAuthConfiguration(JahiaOAuthConfiguration jahiaOAuthConfiguration) {
        super.setJahiaOAuthConfiguration(jahiaOAuthConfiguration);
    }
}
