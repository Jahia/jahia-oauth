/**
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 * <p>
 * http://www.jahia.com
 * <p>
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 * <p>
 * Copyright (C) 2002-2020 Jahia Solutions Group. All rights reserved.
 * <p>
 * This file is part of a Jahia's Enterprise Distribution.
 * <p>
 * Jahia's Enterprise Distributions must be used in accordance with the terms
 * contained in the Jahia Solutions Group Terms & Conditions as well as
 * the Jahia Sustainable Enterprise License (JSEL).
 * <p>
 * For questions regarding licensing, support, production usage...
 * please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 * <p>
 * ==========================================================================================
 */
package org.jahia.modules.jahiaoauth.connectors;

import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.ConnectorPropertyInfo;
import org.jahia.modules.jahiaoauth.service.OAuthConnectorService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class FranceConnectConnectorImpl implements OAuthConnectorService {

    private Map<String, String> protectedResourceUrl;
    private List<ConnectorPropertyInfo> availableProperties;

    @Override
    public String getProtectedResourceUrl(ConnectorConfig config) {
        return protectedResourceUrl.get(config.getProperty("oauthApiName") != null ? config.getProperty("oauthApiName") : config.getConnectorName());
    }

    public String getProtectedResourceUrl() {
        // Deprecated
        return null;
    }

    public void setProtectedResourceUrl(Map<String, String> protectedResourceUrl) {
        this.protectedResourceUrl = protectedResourceUrl;
    }

    @Override
    public List<ConnectorPropertyInfo> getAvailableProperties() {
        return availableProperties;
    }

    public void setAvailableProperties(List<ConnectorPropertyInfo> availableProperties) {
        this.availableProperties = availableProperties;
    }

    public String getServiceName() {
        // Deprecated
        return null;
    }

    @Override
    public void validateSettings(ConnectorConfig settings) throws IOException {
        // Do nothing
    }

}
