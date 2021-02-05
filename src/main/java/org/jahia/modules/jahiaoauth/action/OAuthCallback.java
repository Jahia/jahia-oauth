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
package org.jahia.modules.jahiaoauth.action;

import org.apache.commons.lang.StringUtils;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.SettingsService;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthConstants;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthService;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * @author dgaillard
 */
public class OAuthCallback extends Action {
    private static final Logger logger = LoggerFactory.getLogger(OAuthCallback.class);

    private JahiaOAuthService jahiaOAuthService;
    private SettingsService settingsService;
    private String connectorName;

    @Override
    public ActionResult doExecute(final HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  final JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {

        Boolean isAuthenticate = false;
        if (parameters.containsKey("code") && parameters.containsKey(JahiaOAuthConstants.STATE)) {
            final String token = parameters.get("code").get(0);
            final String state = parameters.get(JahiaOAuthConstants.STATE).get(0);
            if (StringUtils.isBlank(token) || StringUtils.isBlank(state)) {
                return ActionResult.BAD_REQUEST;
            }

            ConnectorConfig oauthConfig = settingsService.getConnectorConfig(renderContext.getSite().getSiteKey(), connectorName);
            try {
                jahiaOAuthService.extractAccessTokenAndExecuteMappers(oauthConfig, token, state);
                isAuthenticate = true;
            } catch (Exception ex) {
                logger.error("Could not authenticate user", ex);
            }
        } else {
            logger.error("Could not authenticate user with Google, the callback from the Google server was missing mandatory parameters");
        }

        return new ActionResult(HttpServletResponse.SC_OK,
                jahiaOAuthService.getResultUrl(renderContext.getSite().getUrl(), isAuthenticate),
                true, null);
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setJahiaOAuthService(JahiaOAuthService jahiaOAuthService) {
        this.jahiaOAuthService = jahiaOAuthService;
    }

    public void setConnectorName(String connectorName) {
        this.connectorName = connectorName;
    }
}
