package org.jahia.modules.jahiaoauth.service;

import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.ConnectorService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface OAuthConnectorService extends ConnectorService {
    String getProtectedResourceUrl(ConnectorConfig config);

    @Override
    default void validateSettings(ConnectorConfig connectorConfig) throws IOException {
        // do nothing
    }

    default List<String> getProtectedResourceUrls(ConnectorConfig config) {
        return Collections.singletonList(getProtectedResourceUrl(config));
    }
}
