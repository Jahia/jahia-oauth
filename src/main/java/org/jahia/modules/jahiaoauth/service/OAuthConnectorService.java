package org.jahia.modules.jahiaoauth.service;

import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.ConnectorService;

public interface OAuthConnectorService extends ConnectorService {
    String getProtectedResourceUrl(ConnectorConfig config);
}
