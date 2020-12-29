package org.jahia.modules.jahiaoauth.connectors;

import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.ConnectorPropertyInfo;
import org.jahia.modules.jahiaoauth.service.OAuthConnectorService;

import java.io.IOException;
import java.util.List;

public class GithubConnectorImpl implements OAuthConnectorService {

    private String protectedResourceUrl;
    private List<ConnectorPropertyInfo> availableProperties;

    @Override
    public String getProtectedResourceUrl(ConnectorConfig config) {
        return protectedResourceUrl;
    }

    @Override
    public List<ConnectorPropertyInfo> getAvailableProperties() {
        return availableProperties;
    }

    @Override
    public void validateSettings(ConnectorConfig connectorConfig) throws IOException {
        //
    }

    public String getProtectedResourceUrl() {
        return null;
    }

    public String getServiceName() {
        return null;
    }

    public void setProtectedResourceUrl(String protectedResourceUrl) {
        this.protectedResourceUrl = protectedResourceUrl;
    }

    public void setAvailableProperties(List<ConnectorPropertyInfo> availableProperties) {
        this.availableProperties = availableProperties;
    }
}
