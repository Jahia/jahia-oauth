package org.jahia.modules.jahiaoauth.service;

import com.github.scribejava.core.builder.api.DefaultApi20;
import org.jahia.modules.jahiaauth.service.ConnectorConfig;

/**
 * This interface allows to dynamically build a DefaultApi20.
 * The interface contains the method build which allow to build a custom connector before the operations which are done
 * with this connector.
 * It's possible to build a connector thanks to this interface and use the configuration passed as a parameter.
 */
public interface JahiaOAuthAPIBuilder {

    /**
     * Build a custom DefaultApi20
     * @param connectorConfig connection per site
     * @return A custom DefaultApi20 object
     */
    DefaultApi20 build(ConnectorConfig connectorConfig);
}
