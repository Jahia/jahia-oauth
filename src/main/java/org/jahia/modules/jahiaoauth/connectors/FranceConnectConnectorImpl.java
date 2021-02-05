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

import java.util.Map;

public class FranceConnectConnectorImpl extends Connector implements OAuthConnectorService {

    private Map<String, String> mapProtectedResourceUrl;

    @Override
    public String getProtectedResourceUrl(ConnectorConfig config) {
        return mapProtectedResourceUrl
                .get(config.getProperty("oauthApiName") != null ? config.getProperty("oauthApiName") : config.getConnectorName());
    }

    public void setMapProtectedResourceUrl(Map<String, String> mapProtectedResourceUrl) {
        this.mapProtectedResourceUrl = mapProtectedResourceUrl;
    }

}
