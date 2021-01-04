/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.jahiaoauth.impl;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.jahiaauth.service.*;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthConstants;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthException;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthService;
import org.jahia.modules.jahiaoauth.service.OAuthConnectorService;
import org.jahia.osgi.BundleUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dgaillard
 */
public class JahiaOAuthServiceImpl implements JahiaOAuthService {
    private static final Logger logger = LoggerFactory.getLogger(JahiaOAuthServiceImpl.class);

    private final Map<String, DefaultApi20> oAuthDefaultApi20Map;
    private JahiaAuthMapperService jahiaAuthMapperService;

    public JahiaOAuthServiceImpl() {
        this.oAuthDefaultApi20Map = new HashMap<>();
    }

    public JahiaOAuthServiceImpl(Map<String, DefaultApi20> oAuthDefaultApi20Map) {
        this();
        if (oAuthDefaultApi20Map != null && !oAuthDefaultApi20Map.isEmpty()) {
            oAuthDefaultApi20Map.forEach(this::addOAuthDefaultApi20);
        }
    }

    @Override
    public String getAuthorizationUrl(ConnectorConfig config, String sessionId) {
        return getAuthorizationUrl(config, sessionId, null);
    }

    @Override
    public String getAuthorizationUrl(ConnectorConfig config, String sessionId, Map<String, String> additionalParams) {
        OAuth20Service service = createOAuth20Service(config);

        return service.createAuthorizationUrlBuilder().additionalParams(additionalParams).state(sessionId).build();
    }

    @Override
    public String getResultUrl(String siteUrl, Boolean isAuthenticate) {
        return StringUtils.substringBeforeLast(siteUrl, ".html") + "/oauth-result.html?isAuthenticate=" + isAuthenticate;
    }

    @Override
    public Map<String, Object> refreshAccessToken(ConnectorConfig config, String refreshToken) throws Exception {
        OAuth20Service service = createOAuth20Service(config);
        OAuth2AccessToken accessToken = service.refreshAccessToken(refreshToken);
        return extractAccessTokenData(accessToken);
    }

    @Override
    public void extractAccessTokenAndExecuteMappers(ConnectorConfig config, String token, String state) throws Exception {
        OAuth20Service service = createOAuth20Service(config);
        OAuth2AccessToken accessToken = service.getAccessToken(token);

        OAuthConnectorService connectorService = BundleUtils.getOsgiService(OAuthConnectorService.class, "(" + JahiaAuthConstants.CONNECTOR_SERVICE_NAME + "=" + config.getConnectorName() + ")");
        if (connectorService == null) {
            logger.error("Connector service was null for service name: {}", config.getConnectorName());
            throw new JahiaOAuthException("Connector service was null for service name: " + config.getConnectorName());
        }

        // Request all the properties available right now
        OAuthRequest request = new OAuthRequest(Verb.GET, connectorService.getProtectedResourceUrl(config));
        request.addHeader("x-li-format", "json");
        service.signRequest(accessToken, request);
        Response response = service.execute(request);

        // if we got the properties then execute mapper
        if (response.getCode() == HttpServletResponse.SC_OK) {
            try {
                JSONObject responseJson = new JSONObject(response.getBody());
                if (logger.isDebugEnabled()) {
                    logger.debug(responseJson.toString());
                }

                // Store in a simple map the results by properties as mapped in the connector
                Map<String, Object> propertiesResult = getPropertiesResult(connectorService, responseJson);
                addTokensData(config.getConnectorName(), accessToken, propertiesResult, config.getSiteKey());

                // Get Mappers
                for (MapperConfig mapperConfig : config.getMappers()) {
                    if (mapperConfig.isActive()) {
                        jahiaAuthMapperService.executeMapper(state, mapperConfig, propertiesResult);
                    }
                }
            } catch (Exception e) {
                logger.error("Did not received expected json, response message was: {} and response body was: {}", response.getMessage(), response.getBody());
                throw e;
            }
        } else {
            logger.error("Did not received expected response, response code: {}, response message: {} response body was: {}", response.getCode(), response.getMessage(), response.getBody());
            throw new JahiaOAuthException("Did not received expected response, response code: " + response.getCode() + ", response message: " + response.getMessage() + " response body was: " + response.getBody());
        }
    }

    private Map<String, Object> extractAccessTokenData(OAuth2AccessToken accessToken) {
        Map<String, Object> tokenData = new HashMap<>();

        tokenData.put(JahiaOAuthConstants.ACCESS_TOKEN, accessToken.getAccessToken());
        tokenData.put(JahiaOAuthConstants.TOKEN_EXPIRES_IN, accessToken.getExpiresIn());
        tokenData.put(JahiaOAuthConstants.REFRESH_TOKEN, accessToken.getRefreshToken());
        tokenData.put(JahiaOAuthConstants.TOKEN_SCOPE, accessToken.getScope());
        tokenData.put(JahiaOAuthConstants.TOKEN_TYPE, accessToken.getTokenType());

        return tokenData;
    }

    private Map<String, Object> getPropertiesResult(ConnectorService connectorService, JSONObject responseJson) throws JSONException {
        Map<String, Object> propertiesResult = new HashMap<>();
        List<ConnectorPropertyInfo> properties = connectorService.getAvailableProperties();
        for (ConnectorPropertyInfo entry : properties) {
            getPropertyResult(responseJson, propertiesResult, entry);
        }
        return propertiesResult;
    }

    private void getPropertyResult(JSONObject responseJson, Map<String, Object> propertiesResult, ConnectorPropertyInfo entry) throws JSONException {
        if (entry.getPropertyToRequest() == null && responseJson.has(entry.getName())) {
            propertiesResult.put(entry.getName(), responseJson.get(entry.getName()));
        } else if (entry.getPropertyToRequest() != null && responseJson.has(entry.getPropertyToRequest())) {
            if (entry.getValuePath() != null) {
                if (StringUtils.startsWith(entry.getValuePath(), "/")) {
                    extractPropertyFromJSONObject(propertiesResult, responseJson.getJSONObject(entry.getPropertyToRequest()), entry.getValuePath(), entry.getName());
                } else {
                    extractPropertyFromJSONArray(propertiesResult, responseJson.getJSONArray(entry.getPropertyToRequest()), entry.getValuePath(), entry.getName());
                }
            } else {
                propertiesResult.put(entry.getName(), responseJson.get(entry.getPropertyToRequest()));
            }
        }
    }

    private void extractPropertyFromJSONObject(Map<String, Object> propertiesResult, JSONObject jsonObject, String pathToProperty, String propertyName) throws JSONException {
        if (StringUtils.startsWith(pathToProperty, "/")) {

            String key = StringUtils.substringAfter(pathToProperty, "/");
            String potentialKey1 = StringUtils.substringBefore(key, "[");
            String potentialKey2 = StringUtils.substringBefore(key, "/");

            if (potentialKey1.length() <= potentialKey2.length()) {
                key = potentialKey1;
            } else {
                key = potentialKey2;
            }

            pathToProperty = StringUtils.substringAfter(pathToProperty, "/" + key);

            if (StringUtils.isBlank(pathToProperty) && jsonObject.has(key)) {
                propertiesResult.put(propertyName, jsonObject.get(key));
            } else {
                if (StringUtils.startsWith(pathToProperty, "/") && jsonObject.has(key)) {
                    extractPropertyFromJSONObject(propertiesResult, jsonObject.getJSONObject(key), pathToProperty, propertyName);
                } else if (jsonObject.has(key)) {
                    extractPropertyFromJSONArray(propertiesResult, jsonObject.getJSONArray(key), pathToProperty, propertyName);
                }
            }
        }
    }

    private void addTokensData(String connectorServiceName, OAuth2AccessToken accessToken, Map<String, Object> propertiesResult, String siteKey) {
        // add token to result
        propertiesResult.put(JahiaOAuthConstants.TOKEN_DATA, extractAccessTokenData(accessToken));
        propertiesResult.put(JahiaAuthConstants.CONNECTOR_SERVICE_NAME, connectorServiceName);
        propertiesResult.put(JahiaAuthConstants.CONNECTOR_NAME_AND_ID, connectorServiceName + "_" + propertiesResult.get("id"));
        propertiesResult.put(JahiaAuthConstants.PROPERTY_SITE_KEY, siteKey);
    }

    private void extractPropertyFromJSONArray(Map<String, Object> propertiesResult, JSONArray jsonArray, String pathToProperty, String propertyName) throws JSONException {
        int arrayIndex = Integer.parseInt(StringUtils.substringBetween(pathToProperty, "[", "]"));
        pathToProperty = StringUtils.substringAfter(pathToProperty, "]");
        if (StringUtils.isBlank(pathToProperty) && jsonArray.length() >= arrayIndex) {
            propertiesResult.put(propertyName, jsonArray.get(arrayIndex));
        } else {
            if (StringUtils.startsWith(pathToProperty, "/") && jsonArray.length() >= arrayIndex) {
                extractPropertyFromJSONObject(propertiesResult, jsonArray.getJSONObject(arrayIndex), pathToProperty, propertyName);
            } else if (jsonArray.length() >= arrayIndex) {
                extractPropertyFromJSONArray(propertiesResult, jsonArray.getJSONArray(arrayIndex), pathToProperty, propertyName);
            }
        }
    }

    private OAuth20Service createOAuth20Service(ConnectorConfig config) {
        String callbackUrl = config.getProperty(JahiaOAuthConstants.PROPERTY_CALLBACK_URL);

        ServiceBuilder serviceBuilder = new ServiceBuilder(config.getProperty(JahiaOAuthConstants.PROPERTY_API_KEY)).apiSecret(config.getProperty(JahiaOAuthConstants.PROPERTY_API_SECRET)).callback(callbackUrl);

        if (StringUtils.isNotBlank(config.getProperty(JahiaOAuthConstants.PROPERTY_SCOPE))) {
            serviceBuilder.withScope(config.getProperty(JahiaOAuthConstants.PROPERTY_SCOPE));
        }
        return serviceBuilder.build(oAuthDefaultApi20Map.get(config.getProperty("oauthApiName") != null ? config.getProperty("oauthApiName") : config.getConnectorName()));
    }

    @Override
    public void addOAuthDefaultApi20(String key, DefaultApi20 oAuthDefaultApi20) {
        oAuthDefaultApi20Map.put(key, oAuthDefaultApi20);
    }

    @Override
    public void removeOAuthDefaultApi20(String key) {
        if (oAuthDefaultApi20Map.containsKey(key)) {
            oAuthDefaultApi20Map.remove(key);
        } else {
            logger.warn("OAuthDefaultApi20 {} not found", key);
        }
    }

    @Override
    public void removeOAuthDefaultApi20(DefaultApi20 oAuthDefaultApi20) {
        oAuthDefaultApi20Map.entrySet().stream().filter(entry -> entry.getValue().equals(oAuthDefaultApi20)).findFirst().ifPresent(oAuthDefaultApi20Entry -> oAuthDefaultApi20Map.remove(oAuthDefaultApi20Entry.getKey()));
    }

    public void setJahiaAuthMapperService(JahiaAuthMapperService jahiaAuthMapperService) {
        this.jahiaAuthMapperService = jahiaAuthMapperService;
    }
}
