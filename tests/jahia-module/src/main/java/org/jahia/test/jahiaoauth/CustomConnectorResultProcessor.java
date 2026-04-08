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
 *     Copyright (C) 2002-2026 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.test.jahiaoauth;

import org.jahia.api.content.JCRTemplate;
import org.jahia.api.usermanager.JahiaUserManagerService;
import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.ConnectorResultProcessor;
import org.jahia.services.content.decorator.JCRUserNode;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Test-only {@link ConnectorResultProcessor} that demonstrates post-processing of complex
 * (list-valued) connector results obtained via JSONPath expressions.
 *
 * <p>When the active connector mapping includes the JSONPath expression
 * {@code $.nestedLevel.simpleArray}, this processor joins the resolved array elements with
 * {@code _} and writes the result to the {@code customProperty} JCR property of the
 * authenticated user.
 *
 * <p>This mirrors a real-world pattern: a JSONPath expression such as
 * {@code $.authorization_context.groups} can extract a multi-valued list from an identity-
 * provider token (e.g. Keycloak), pass it as a raw {@code List} through the connector
 * pipeline, and then a custom {@link ConnectorResultProcessor} implementation can turn it
 * into whatever the application needs (group-membership updates, comma-separated strings,
 * role assignments, etc.).
 *
 * <p><strong>Note:</strong> this component is registered as an OSGi service and is therefore
 * invoked for <em>every</em> successful OAuth authentication in the test environment, not
 * only the nested-fields test. The implementation guards against the case where the mapping
 * is absent.
 */
@Component(immediate = true, service = ConnectorResultProcessor.class)
public class CustomConnectorResultProcessor implements ConnectorResultProcessor {
    private JahiaUserManagerService jahiaUserManagerService;
    private JCRTemplate jcrTemplate;

    @Reference
    public void setJahiaUserManagerService(JahiaUserManagerService jahiaUserManagerService) {
        this.jahiaUserManagerService = jahiaUserManagerService;
    }

    @Reference
    public void setJcrTemplate(JCRTemplate jcrTemplate) {
        this.jcrTemplate = jcrTemplate;
    }

    @Override
    public void execute(ConnectorConfig connectorConfig, Map<String, Object> results) {
        Object login = results.get("id");
        Object simpleArray = results.get("$.nestedLevel.simpleArray");
        // Guard: only execute when both the user login and the array value are present.
        // This processor runs for every successful OAuth authentication (it is an OSGi service),
        // so connectors not configured with the $.nestedLevel.simpleArray mapping will have a
        // null simpleArray value — which must not cause a NullPointerException.
        if (login instanceof String && simpleArray instanceof List) {
            try {
                jcrTemplate.doExecuteWithSystemSession(session -> {
                    JCRUserNode user = jahiaUserManagerService.lookupUser((String) login, session);
                    String simpleArrayJoined = ((List<?>) simpleArray).stream().map(Object::toString).collect(Collectors.joining("_"));
                    user.setProperty("customProperty", simpleArrayJoined);
                    session.save();
                    return null;
                });
            } catch (RepositoryException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
