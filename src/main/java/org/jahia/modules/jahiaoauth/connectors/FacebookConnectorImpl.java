/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms & Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.jahiaoauth.connectors;

import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaoauth.service.OAuthConnectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

/**
 * Use Facebook credentials to connect to Jahia
 *
 * @author dgaillard
 */
public class FacebookConnectorImpl extends Connector implements OAuthConnectorService {
    private static final Logger logger = LoggerFactory.getLogger(FacebookConnectorImpl.class);

    @Override
    public String getProtectedResourceUrl(ConnectorConfig config) {
        String urlWithProperties = protectedResourceUrl.concat(getAvailableProperties().stream()
                .map(property -> property.getPropertyToRequest() == null ? property.getName() : property.getPropertyToRequest()).distinct()
                .collect(Collectors.joining(",")));

        if (logger.isDebugEnabled()) {
            logger.debug("Protected Resource URL = {}", urlWithProperties);
        }
        return urlWithProperties;
    }
}
