package org.jahia.modules.jahiaoauth.impl;

import com.github.scribejava.core.builder.api.DefaultApi20;
import org.jahia.modules.jahiaauth.service.ConnectorConfig;

/**
 * Default API builder which return the default api object which is stored for this builder
 */
public class JahiaOAuthDefaultAPIBuilder implements JahiaOAuthAPIBuilder{

    DefaultApi20 defaultApi20;

    public void setDefaultApi20(DefaultApi20 defaultApi20) {
        this.defaultApi20 = defaultApi20;
    }

    @Override
    public DefaultApi20 build(ConnectorConfig connectorConfig) {
        return defaultApi20;
    }
}
