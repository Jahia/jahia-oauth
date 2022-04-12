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
package org.jahia.modules.jahiaoauth.impl;

import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.ConnectorPropertyInfo;
import org.jahia.modules.jahiaauth.service.ConnectorService;
import org.jahia.modules.jahiaauth.service.JahiaAuthConstants;
import org.jahia.modules.jahiaauth.service.JahiaAuthMapperService;
import org.jahia.modules.jahiaauth.service.MapperConfig;
import org.jahia.modules.jahiaoauth.service.*;
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

    private final Map<String, JahiaOAuthAPIBuilder> oAuthDefaultApi20Map;
    private JahiaAuthMapperService jahiaAuthMapperService;

    public JahiaOAuthServiceImpl() {
        this.oAuthDefaultApi20Map = new HashMap<>();
    }

    public JahiaOAuthServiceImpl(Map<String, JahiaOAuthAPIBuilder> oAuthDefaultApi20Map) {
        this();
        if (oAuthDefaultApi20Map != null && !oAuthDefaultApi20Map.isEmpty()) {
            oAuthDefaultApi20Map.forEach(this::addOAuthDefaultApi20);
        }
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
    public String getAuthorizationUrl(ConnectorConfig config, String sessionId) {
        return getAuthorizationUrl(config, sessionId, null);
    }

    @Override
    @SuppressWarnings("java:S3776")
    public void extractAccessTokenAndExecuteMappers(ConnectorConfig config, String token, String state) throws Exception {
        OAuth20Service service = createOAuth20Service(config);
        OAuth2AccessToken accessToken = service.getAccessToken(token);

        OAuthConnectorService connectorService = BundleUtils.getOsgiService(OAuthConnectorService.class,
                "(" + JahiaAuthConstants.CONNECTOR_SERVICE_NAME + "=" + config.getConnectorName() + ")");
        if (connectorService == null) {
            logger.error("Connector service was null for service name: {}", config.getConnectorName());
            throw new JahiaOAuthException("Connector service was null for service name: " + config.getConnectorName());
        }

        Map<String, Object> propertiesResult = new HashMap<>();

        List<String> urlsToProcess = connectorService.getProtectedResourceUrls(config);

        for (String url : urlsToProcess) {
            // Request all the properties available right now
            OAuthRequest request = new OAuthRequest(Verb.GET, url);
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
                    propertiesResult.putAll(getPropertiesResult(connectorService, responseJson));
                } catch (Exception e) {
                    logger.error("Did not received expected json, response message was: {} and response body was: {}",
                            response.getMessage(), response.getBody());
                    throw e;
                }
            } else if (urlsToProcess.size() > 1 && response.getCode() == HttpServletResponse.SC_FORBIDDEN) {
                // In case of multiple url, it is possible that not all available.
                // Do nothing in that case - we check at the end if all properties are filled
            } else {
                logger.error("Did not received expected response, response code: {}, response message: {} response body was: {}",
                        response.getCode(), response.getMessage(), response.getBody());
                throw new JahiaOAuthException(
                        "Did not received expected response, response code: " + response.getCode() + ", response message: " + response
                                .getMessage() + " response body was: " + response.getBody());
            }
        }

        try {
            addTokensData(config.getConnectorName(), accessToken, propertiesResult, config.getSiteKey());

            // Get Mappers
            for (MapperConfig mapperConfig : config.getMappers()) {
                if (mapperConfig.isActive()) {
                    // Check that all props are found
                    mapperConfig.getMappings().forEach(mapping -> {
                        if (!propertiesResult.containsKey(mapping.getConnectorProperty())) {
                            logger.warn(
                                    "Connector property {} mapped to jcr property {} was not found in the received properties, please check your configuration",
                                    mapping.getConnectorProperty(), mapping.getMappedProperty());
                        }
                    });
                    jahiaAuthMapperService.executeMapper(state, mapperConfig, propertiesResult);
                }
            }

            // Get Post Executions
            jahiaAuthMapperService.executeConnectorResultProcessors(config, propertiesResult);
        } catch (Exception e) {
            throw new JahiaOAuthException("Something when wrong in OAuth with config " + config.getConnectorName(), e);
        }
    }

    private Map<String, Object> extractAccessTokenData(OAuth2AccessToken accessToken) {
        Map<String, Object> tokenData = new HashMap<>();

        tokenData.put(JahiaOAuthConstants.ACCESS_TOKEN, accessToken.getAccessToken());
        tokenData.put(JahiaOAuthConstants.TOKEN_EXPIRES_IN, accessToken.getExpiresIn());
        tokenData.put(JahiaOAuthConstants.REFRESH_TOKEN, accessToken.getRefreshToken());
        tokenData.put(JahiaOAuthConstants.TOKEN_SCOPE, accessToken.getScope());
        tokenData.put(JahiaOAuthConstants.TOKEN_TYPE, accessToken.getTokenType());
        if (accessToken instanceof OpenIdOAuth2AccessToken) {
            tokenData.put(JahiaOAuthConstants.OPEN_ID_TOKEN, ((OpenIdOAuth2AccessToken) accessToken).getOpenIdToken());
        }
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

    private void getPropertyResult(JSONObject responseJson, Map<String, Object> propertiesResult, ConnectorPropertyInfo entry)
            throws JSONException {
        if (entry.getPropertyToRequest() == null && responseJson.has(entry.getName())) {
            propertiesResult.put(entry.getName(), responseJson.get(entry.getName()));
        } else if (entry.getPropertyToRequest() != null && responseJson.has(entry.getPropertyToRequest())) {
            if (entry.getValuePath() != null) {
                if (StringUtils.startsWith(entry.getValuePath(), "/")) {
                    extractPropertyFromJSONObject(propertiesResult, responseJson.getJSONObject(entry.getPropertyToRequest()),
                            entry.getValuePath(), entry.getName());
                } else {
                    extractPropertyFromJSONArray(propertiesResult, responseJson.getJSONArray(entry.getPropertyToRequest()),
                            entry.getValuePath(), entry.getName());
                }
            } else {
                propertiesResult.put(entry.getName(), responseJson.get(entry.getPropertyToRequest()));
            }
        } else {
            final String keyFromPath = StringUtils.substringBetween(entry.getValuePath(), "/");
            if (keyFromPath != null && responseJson.has(keyFromPath)) {
                @SuppressWarnings("java:S1075") String propertyPath = StringUtils.substringAfter(entry.getValuePath(), "/" + keyFromPath);
                extractPropertyFromJSONObject(propertiesResult, responseJson.getJSONObject(keyFromPath), propertyPath, entry.getName());
            }
        }
    }

    private void extractPropertyFromJSONObject(Map<String, Object> propertiesResult, JSONObject jsonObject, String pathToProperty,
            String propertyName) throws JSONException {
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

    private void addTokensData(String connectorServiceName, OAuth2AccessToken accessToken, Map<String, Object> propertiesResult,
            String siteKey) {
        // add token to result
        propertiesResult.put(JahiaOAuthConstants.TOKEN_DATA, extractAccessTokenData(accessToken));
        propertiesResult.put(JahiaAuthConstants.CONNECTOR_SERVICE_NAME, connectorServiceName);
        propertiesResult.put(JahiaAuthConstants.CONNECTOR_NAME_AND_ID, connectorServiceName + "_" + propertiesResult.get("id"));
        propertiesResult.put(JahiaAuthConstants.PROPERTY_SITE_KEY, siteKey);
    }

    private void extractPropertyFromJSONArray(Map<String, Object> propertiesResult, JSONArray jsonArray, String pathToProperty,
            String propertyName) throws JSONException {
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

        ServiceBuilder serviceBuilder = new ServiceBuilder(config.getProperty(JahiaOAuthConstants.PROPERTY_API_KEY))
                .apiSecret(config.getProperty(JahiaOAuthConstants.PROPERTY_API_SECRET)).callback(callbackUrl);

        if (StringUtils.isNotBlank(config.getProperty(JahiaOAuthConstants.PROPERTY_SCOPE))) {
            serviceBuilder.withScope(config.getProperty(JahiaOAuthConstants.PROPERTY_SCOPE));
        }

        return serviceBuilder.build(oAuthDefaultApi20Map
                .get(config.getProperty("oauthApiName") != null ? config.getProperty("oauthApiName") : config.getConnectorName())
                .build(config));
    }

    @Override
    public void addOAuthDefaultApi20(String key, DefaultApi20 oAuthDefaultApi20) {
        JahiaOAuthDefaultAPIBuilder api = new JahiaOAuthDefaultAPIBuilder();
        api.setDefaultApi20(oAuthDefaultApi20);
        oAuthDefaultApi20Map.put(key, api);
    }

    @Override
    public void addOAuthDefaultApi20(String key, JahiaOAuthAPIBuilder apiBuilder) {
        oAuthDefaultApi20Map.put(key, apiBuilder);
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
        oAuthDefaultApi20Map.entrySet().stream().filter(entry -> entry.getValue().equals(oAuthDefaultApi20)).findFirst()
                .ifPresent(oAuthDefaultApi20Entry -> oAuthDefaultApi20Map.remove(oAuthDefaultApi20Entry.getKey()));
    }

    public void setJahiaAuthMapperService(JahiaAuthMapperService jahiaAuthMapperService) {
        this.jahiaAuthMapperService = jahiaAuthMapperService;
    }
}
