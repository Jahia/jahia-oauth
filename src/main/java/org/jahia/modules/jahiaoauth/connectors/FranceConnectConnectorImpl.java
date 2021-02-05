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
import org.jahia.modules.jahiaoauth.service.OAuthConnectorService;

import java.util.Map;

public class FranceConnectConnectorImpl extends Connector implements OAuthConnectorService {

    private Map<String, String> protectedResourceUrl;

    @Override
    public String getProtectedResourceUrl(ConnectorConfig config) {
        return protectedResourceUrl
                .get(config.getProperty("oauthApiName") != null ? config.getProperty("oauthApiName") : config.getConnectorName());
    }

    public void setProtectedResourceUrl(Map<String, String> protectedResourceUrl) {
        this.protectedResourceUrl = protectedResourceUrl;
    }

}
