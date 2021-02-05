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
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * @author dgaillard
 */
public class ConnectToOAuthProvider extends Action {
    private SettingsService settingsService;
    private JahiaOAuthService jahiaOAuthService;
    private String connectorName;
    private Map<String, String> additionalParams;

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource, JCRSessionWrapper session,
            Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {

        final String sessionId = req.getSession().getId();
        ConnectorConfig oauthConfig = settingsService.getConnectorConfig(renderContext.getSite().getSiteKey(), connectorName);

        String authorizationUrl = jahiaOAuthService.getAuthorizationUrl(oauthConfig, sessionId, getAdditionalParams());

        JSONObject response = new JSONObject();
        response.put(JahiaOAuthConstants.AUTHORIZATION_URL, authorizationUrl);
        return new ActionResult(HttpServletResponse.SC_OK, null, response);
    }

    public Map<String, String> getAdditionalParams() {
        return additionalParams;
    }

    public void setJahiaOAuthService(JahiaOAuthService jahiaOAuthService) {
        this.jahiaOAuthService = jahiaOAuthService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setConnectorName(String connectorName) {
        this.connectorName = connectorName;
    }

    public void setAdditionalParams(Map<String, String> additionalParams) {
        this.additionalParams = additionalParams;
    }
}
