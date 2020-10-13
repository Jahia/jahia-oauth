/**
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
import org.jahia.modules.jahiaauth.service.ConnectorPropertyInfo;
import org.jahia.modules.jahiaauth.service.ConnectorService;

import java.io.IOException;
import java.util.List;

/**
 * @author dgaillard
 */
public class GoogleConnectorImpl implements ConnectorService {
    private String protectedResourceUrl;
    private String serviceName;
    private List<ConnectorPropertyInfo> availableProperties;

    @Override
    public String getProtectedResourceUrl() {
        return protectedResourceUrl;
    }

    @Override
    public List<ConnectorPropertyInfo> getAvailableProperties() {
        return availableProperties;
    }

    @Override
    public void validateSettings(ConnectorConfig settings) throws IOException {
        // Do nothing
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setProtectedResourceUrl(String protectedResourceUrl) {
        this.protectedResourceUrl = protectedResourceUrl;
    }

    public void setAvailableProperties(List<ConnectorPropertyInfo> availableProperties) {
        this.availableProperties = availableProperties;
    }
}
