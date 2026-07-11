/*
 * Copyright (C) 2002-2021 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

/**
 * @author dgaillard
 */
public class ConnectToOAuthProvider extends Action {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private SettingsService settingsService;
    private JahiaOAuthService jahiaOAuthService;
    private String connectorName;
    private Map<String, String> additionalParams;

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource, JCRSessionWrapper session,
            Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {

        // Issue an unpredictable, single-use state value and bind it to the server-side session
        // (RFC 6749 §10.12), rather than deriving the state from the session id. OAuthCallback checks
        // and consumes it; the mapper cache there remains keyed by the session id, so the SSO flow is
        // unchanged.
        final String state = new BigInteger(130, SECURE_RANDOM).toString(32);
        req.getSession().setAttribute(JahiaOAuthConstants.SESSION_OAUTH_STATE, state);

        ConnectorConfig oauthConfig = settingsService.getConnectorConfig(renderContext.getSite().getSiteKey(), connectorName);

        // When an OIDC nonce is included in the authorization request, bind it to the session so the
        // callback can verify the id_token carries the same value (OpenID Connect Core §3.1.2.1).
        Map<String, String> additionalParams = getAdditionalParams();
        if (additionalParams != null && additionalParams.containsKey(JahiaOAuthConstants.NONCE)) {
            req.getSession().setAttribute(JahiaOAuthConstants.SESSION_OAUTH_NONCE, additionalParams.get(JahiaOAuthConstants.NONCE));
        }

        String authorizationUrl = jahiaOAuthService.getAuthorizationUrl(oauthConfig, state, additionalParams);
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
