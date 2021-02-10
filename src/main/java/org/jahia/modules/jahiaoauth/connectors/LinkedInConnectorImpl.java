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
import org.jahia.modules.jahiaoauth.service.OAuthConnectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Auth linkedin connector
 * Use linkedin credentials to connect to Jahia
 *
 * @author dgaillard
 */
public class LinkedInConnectorImpl extends Connector implements OAuthConnectorService {
    private static final Logger logger = LoggerFactory.getLogger(LinkedInConnectorImpl.class);

    @Override
    public String getProtectedResourceUrl(ConnectorConfig config) {
        return getProtectedResourceUrl(protectedResourceUrl);
    }

    @Override
    public List<String> getProtectedResourceUrls(ConnectorConfig config) {
        return protectedResourceUrls.stream().map(this::getProtectedResourceUrl).collect(Collectors.toList());
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

}
