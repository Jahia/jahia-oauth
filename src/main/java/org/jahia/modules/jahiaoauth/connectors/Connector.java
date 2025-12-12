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
import org.jahia.modules.jahiaoauth.config.JahiaOAuthConfiguration;
import org.jahia.modules.jahiaoauth.service.OAuthConnectorService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public abstract class Connector implements OAuthConnectorService {

    private final Function<JahiaOAuthConfiguration, List<String>> configurationUrlsResolver;
    protected String protectedResourceUrl;
    protected List<String> protectedResourceUrls;
    protected List<ConnectorPropertyInfo> availableProperties;
    private JahiaOAuthConfiguration jahiaOAuthConfiguration;

    protected Connector(Function<JahiaOAuthConfiguration, List<String>> configurationUrlsResolver) {
        this.configurationUrlsResolver = configurationUrlsResolver;
    }

    @Override
    public String getProtectedResourceUrl(ConnectorConfig config) {
        return resolveConfigurationUrl();
    }

    @Override
    public List<String> getProtectedResourceUrls(ConnectorConfig config) {
        return resolveConfigurationUrls();
    }

    @Override
    public List<ConnectorPropertyInfo> getAvailableProperties() {
        return availableProperties;
    }

    public void setProtectedResourceUrl(String protectedResourceUrl) {
        this.protectedResourceUrl = protectedResourceUrl;
    }

    public void setAvailableProperties(List<ConnectorPropertyInfo> availableProperties) {
        this.availableProperties = new ArrayList<>(availableProperties);
    }

    public void setProtectedResourceUrls(List<String> protectedResourceUrls) {
        this.protectedResourceUrls = protectedResourceUrls;
    }

    public void setJahiaOAuthConfiguration(JahiaOAuthConfiguration jahiaOAuthConfiguration) {
        this.jahiaOAuthConfiguration = jahiaOAuthConfiguration;
    }

    protected List<String> resolveConfigurationUrls() {
        return configurationUrlsResolver == null ? Collections.emptyList() : configurationUrlsResolver.apply(jahiaOAuthConfiguration);
    }

    protected String resolveConfigurationUrl() {
        List<String> resolvedUrls = resolveConfigurationUrls();
        return resolvedUrls.isEmpty() ? "" : resolvedUrls.get(0);
    }
}
