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
package org.jahia.modules.jahiaoauth.impl;

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.ConnectorPropertyInfo;
import org.jahia.modules.jahiaoauth.service.OAuthConnectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Auth linkedin connector
 * Use linkedin credentials to connect to Jahia
 * @author dgaillard
 */
public class LinkedInConnectorImpl implements OAuthConnectorService {
    private static final Logger logger = LoggerFactory.getLogger(LinkedInConnectorImpl.class);

    private String protectedResourceUrl;
    private List<ConnectorPropertyInfo> availableProperties;

    public String getProtectedResourceUrl() {
        // Deprecated
        return null;
    }

    @Override
    public String getProtectedResourceUrl(ConnectorConfig config) {
        StringBuilder propertiesAsString = new StringBuilder();
        boolean asPrevious = false;
        for (ConnectorPropertyInfo entry: getAvailableProperties()) {
            if (entry.getPropertyToRequest() == null) {
                if (asPrevious) {
                    propertiesAsString.append(",");
                }
                propertiesAsString.append(entry.getName());
                asPrevious = true;
            } else {
                String propertyToRequest = entry.getPropertyToRequest();
                if (!StringUtils.contains(propertiesAsString.toString(), propertyToRequest)) {
                    if (asPrevious) {
                        propertiesAsString.append(",");
                    }
                    propertiesAsString.append(propertyToRequest);
                    asPrevious = true;
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Protected Resource URL = {}", protectedResourceUrl + propertiesAsString);
        }
        return String.format(protectedResourceUrl, propertiesAsString);
    }

    @Override
    public List<ConnectorPropertyInfo> getAvailableProperties() {
        return new ArrayList<>(availableProperties);
    }

    public String getServiceName() {
        // Deprecated
        return null;
    }

    public void setProtectedResourceUrl(String protectedResourceUrl) {
        this.protectedResourceUrl = protectedResourceUrl;
    }

    public void setAvailableProperties(List<ConnectorPropertyInfo> availableProperties) {
        this.availableProperties = new ArrayList<>(availableProperties);
    }

    @Override
    public void validateSettings(ConnectorConfig settings) throws IOException {
        // Done on client side.
    }
}
