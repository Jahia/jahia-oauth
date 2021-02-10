/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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
import com.github.scribejava.core.builder.api.BaseApi;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.jahiaoauth.service.*;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * @author dgaillard
 */
public class JahiaOAuthServiceImpl implements JahiaOAuthService {
    private static final Logger logger = LoggerFactory.getLogger(JahiaOAuthServiceImpl.class);

    private final Map<String, BaseApi<? extends OAuth20Service>> oAuthBase20ApiMap;
    private JahiaOAuthCacheService jahiaOAuthCacheService;

    public JahiaOAuthServiceImpl() {
        this.oAuthBase20ApiMap = new HashMap<>();
    }

    public JahiaOAuthServiceImpl(Map<String, BaseApi<? extends OAuth20Service>> oAuthBase20ApiMap) {
        this();
        if (oAuthBase20ApiMap != null && !oAuthBase20ApiMap.isEmpty()) {
            for (Map.Entry<String, BaseApi<? extends OAuth20Service>> entry : oAuthBase20ApiMap.entrySet()) {
                addOAuth20Service(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public String getAuthorizationUrl(JCRNodeWrapper jahiaOAuthNode, String connectorServiceName, String sessionId) throws RepositoryException {
        return getAuthorizationUrl(jahiaOAuthNode, connectorServiceName, sessionId, null);
    }

    @Override
    public String getAuthorizationUrl(JCRNodeWrapper jahiaOAuthNode, String connectorServiceName, String sessionId, Map<String, String> additionalParams) throws RepositoryException {
        JCRNodeWrapper connectorNode = jahiaOAuthNode.getNode(connectorServiceName);
        OAuth20Service service = createOAuth20Service(connectorNode, connectorServiceName, sessionId);

        return service.getAuthorizationUrl(additionalParams);
    }

    @Override
    public HashMap<String, Object> getMapperResults(String mapperServiceName, String sessionId) {
        return jahiaOAuthCacheService.getMapperResultsCacheEntry(mapperServiceName + "_" + sessionId);
    }

    @Override
    public String getResultUrl(String siteUrl, Boolean isAuthenticate) {
        return StringUtils.substringBeforeLast(siteUrl, ".html") + "/oauth-result.html?isAuthenticate=" + isAuthenticate;
    }

    @Override
    public Map<String, Object> refreshAccessToken(JCRNodeWrapper jahiaOAuthNode, String connectorServiceName, String refreshToken) throws Exception {
        JCRNodeWrapper connectorNode = jahiaOAuthNode.getNode(connectorServiceName);
        OAuth20Service service = createOAuth20Service(connectorNode, connectorServiceName, null);
        OAuth2AccessToken accessToken = service.refreshAccessToken(refreshToken);
        return extractAccessTokenData(accessToken);
    }

    @Override
    public Map<String, Object> requestUserData(JCRNodeWrapper jahiaOAuthNode, String connectorServiceName, String mapperServiceName, String refreshToken) throws Exception {
        JCRNodeWrapper connectorNode = jahiaOAuthNode.getNode(connectorServiceName);
        OAuth20Service service = createOAuth20Service(connectorNode, connectorServiceName, null);
        OAuth2AccessToken accessToken = service.refreshAccessToken(refreshToken);

        ConnectorService connectorService = BundleUtils.getOsgiService(ConnectorService.class, "(" + JahiaOAuthConstants.CONNECTOR_SERVICE_NAME + "=" + connectorServiceName + ")");
        if (connectorService == null) {
            logger.error("Connector service was null for service name: " + connectorServiceName);
            throw new JahiaOAuthException("Connector service was null for service name: " + connectorServiceName);
        }

        // Request all the properties available right now
        OAuthRequest request = new OAuthRequest(Verb.GET, connectorService.getProtectedResourceUrl());
        request.addHeader("x-li-format", "json");
        service.signRequest(accessToken, request);
        Response response = service.execute(request);

        // if we got the properties then execute mapper
        if (response.getCode() == HttpServletResponse.SC_OK) {
            try {
                JSONObject responseJson = new JSONObject(response.getBody());
                logger.debug(responseJson.toString());

                // Store in a simple map the results by properties as mapped in the connector
                HashMap<String, Object> propertiesResult = getPropertiesResult(connectorService, responseJson);

                HashMap<String, Object> mapperResult = getMapperResults(connectorServiceName, accessToken,
                        responseJson, propertiesResult, connectorNode.getNode(JahiaOAuthConstants.MAPPERS_NODE_NAME).getNode(mapperServiceName));

                return mapperResult;
            } catch (Exception e) {
                logger.error("Did not received expected json, response message was: " + response.getMessage() + " and response body was: " + response.getBody());
                throw e;
            }
        } else {
            logger.error("Did not received expected response, response code: " + response.getCode() + ", response message: " + response.getMessage() + " response body was: ", response.getBody());
            throw new JahiaOAuthException("Did not received expected response, response code: " + response.getCode() + ", response message: " + response.getMessage() + " response body was: " + response.getBody());
        }
    }

    @Override
    public void extractAccessTokenAndExecuteMappers(JCRNodeWrapper jahiaOAuthNode, String connectorServiceName, String token, String state) throws Exception {
        JCRNodeWrapper connectorNode = jahiaOAuthNode.getNode(connectorServiceName);
        OAuth20Service service = createOAuth20Service(connectorNode, connectorServiceName, state);
        OAuth2AccessToken accessToken = service.getAccessToken(token);

        ConnectorService connectorService = BundleUtils.getOsgiService(ConnectorService.class, "(" + JahiaOAuthConstants.CONNECTOR_SERVICE_NAME + "=" + connectorServiceName + ")");
        if (connectorService == null) {
            logger.error("Connector service was null for service name: " + connectorServiceName);
            throw new JahiaOAuthException("Connector service was null for service name: " + connectorServiceName);
        }

        // Request all the properties available right now
        OAuthRequest request = new OAuthRequest(Verb.GET, connectorService.getProtectedResourceUrl());
        request.addHeader("x-li-format", "json");
        service.signRequest(accessToken, request);
        Response response = service.execute(request);

        // if we got the properties then execute mapper
        if (response.getCode() == HttpServletResponse.SC_OK) {
            try {
                JSONObject responseJson = new JSONObject(response.getBody());
                logger.debug(responseJson.toString());

                // Store in a simple map the results by properties as mapped in the connector
                HashMap<String, Object> propertiesResult = getPropertiesResult(connectorService, responseJson);

                // Get Mappers node
                JCRNodeIteratorWrapper mappersNi = connectorNode.getNode(JahiaOAuthConstants.MAPPERS_NODE_NAME).getNodes();
                while (mappersNi.hasNext()) {
                    JCRNodeWrapper mapperNode = (JCRNodeWrapper) mappersNi.nextNode();
                    // make sure mappers is activate
                    if (mapperNode.getProperty(JahiaOAuthConstants.PROPERTY_IS_ACTIVATE).getBoolean()) {
                        HashMap<String, Object> mapperResult = getMapperResults(connectorServiceName, accessToken,
                                responseJson, propertiesResult, mapperNode);

                        jahiaOAuthCacheService.cacheMapperResults(mapperNode.getName() + "_" + state, mapperResult);
                        MapperService mapperService = BundleUtils.getOsgiService(MapperService.class,
                                "(" + JahiaOAuthConstants.MAPPER_SERVICE_NAME + "=" + mapperNode.getName() + ")");
                        if (mapperService != null) {
                            mapperService.executeMapper(mapperResult);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Did not received expected json, response message was: " + response.getMessage() + " and response body was: " + response.getBody());
                throw e;
            }
        } else {
            logger.error("Did not received expected response, response code: " + response.getCode() + ", response message: " + response.getMessage() + " response body was: ", response.getBody());
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

    private HashMap<String, Object> getPropertiesResult(ConnectorService connectorService, JSONObject responseJson) throws JSONException {
        HashMap<String, Object> propertiesResult = new HashMap<>();
        List<Map<String, Object>> properties = connectorService.getAvailableProperties();
        for (Map<String, Object> entry : properties) {
            String propertyName = (String) entry.get(JahiaOAuthConstants.PROPERTY_NAME);
            if ((boolean) entry.get(JahiaOAuthConstants.CAN_BE_REQUESTED) && responseJson.has(propertyName)) {
                propertiesResult.put(propertyName, responseJson.get(propertyName));
            } else if (entry.containsKey(JahiaOAuthConstants.PROPERTY_TO_REQUEST)) {
                String propertyToRequest = (String) entry.get(JahiaOAuthConstants.PROPERTY_TO_REQUEST);
                if (responseJson.has(propertyToRequest)) {
                    if (entry.containsKey(JahiaOAuthConstants.VALUE_PATH)) {
                        String pathToProperty = (String) entry.get(JahiaOAuthConstants.VALUE_PATH);
                        if (StringUtils.startsWith(pathToProperty, "/")) {
                            extractPropertyFromJSON(propertiesResult, responseJson.getJSONObject(propertyToRequest), null, pathToProperty, propertyName);
                        } else {
                            extractPropertyFromJSON(propertiesResult, null, responseJson.getJSONArray(propertyToRequest), pathToProperty, propertyName);
                        }
                    } else {
                        propertiesResult.put(propertyName, responseJson.get(propertyToRequest));
                    }
                }
            }
        }
        return propertiesResult;
    }

    private HashMap<String, Object> getMapperResults(String connectorServiceName, OAuth2AccessToken accessToken,
                                                     JSONObject responseJson, HashMap<String, Object> propertiesResult, JCRNodeWrapper mapperNode) throws JSONException, RepositoryException {
        HashMap<String, Object> mapperResult = new HashMap<>();
        // add token to result
        mapperResult.put(JahiaOAuthConstants.TOKEN_DATA, extractAccessTokenData(accessToken));
        mapperResult.put(JahiaOAuthConstants.CONNECTOR_SERVICE_NAME, connectorServiceName);
        mapperResult.put(JahiaOAuthConstants.CONNECTOR_NAME_AND_ID, connectorServiceName + "_" + propertiesResult.get("id"));
        mapperResult.put(JahiaOAuthConstants.PROPERTY_SITE_KEY, mapperNode.getResolveSite().getSiteKey());

        JSONArray mapping = new JSONArray(mapperNode.getPropertyAsString(JahiaOAuthConstants.PROPERTY_MAPPING));
        for (int i = 0; i < mapping.length(); i++) {
            JSONObject jsonObject = mapping.getJSONObject(i);
            JSONObject mapper = jsonObject.getJSONObject(JahiaOAuthConstants.MAPPER);
            JSONObject connector = jsonObject.getJSONObject(JahiaOAuthConstants.CONNECTOR);
            if (mapper.getBoolean(JahiaOAuthConstants.PROPERTY_MANDATORY) && !propertiesResult.containsKey(connector.getString(JahiaOAuthConstants.PROPERTY_NAME))) {
                logger.error("JSON response was: " + responseJson.toString());
                throw new RepositoryException("Could not execute mapper: missing mandatory property");
            }
            if (propertiesResult.containsKey(connector.getString(JahiaOAuthConstants.PROPERTY_NAME))) {
                Map<String, Object> propertyInfo = new HashMap<>();
                propertyInfo.put(JahiaOAuthConstants.PROPERTY_VALUE, propertiesResult.get(connector.getString(JahiaOAuthConstants.PROPERTY_NAME)));
                propertyInfo.put(JahiaOAuthConstants.PROPERTY_VALUE_TYPE, connector.getString(JahiaOAuthConstants.PROPERTY_VALUE_TYPE));
                if (connector.has(JahiaOAuthConstants.PROPERTY_VALUE_FORMAT)) {
                    propertyInfo.put(JahiaOAuthConstants.PROPERTY_VALUE_FORMAT, connector.getString(JahiaOAuthConstants.PROPERTY_VALUE_FORMAT));
                }
                mapperResult.put(mapper.getString(JahiaOAuthConstants.PROPERTY_NAME), propertyInfo);
            }
        }
        return mapperResult;
    }

    private void extractPropertyFromJSON(HashMap<String, Object> propertiesResult, JSONObject jsonObject, JSONArray jsonArray, String pathToProperty, String propertyName) throws JSONException {
        if (StringUtils.startsWith(pathToProperty, "/")) {

            String key = StringUtils.substringAfter(pathToProperty, "/");
            String potentialKey1 = StringUtils.substringBefore(key, "[");
            String potentialKey2 = StringUtils.substringBefore(key, "/");

            if (potentialKey1.length() <= potentialKey2.length()) {
                key = potentialKey1;
            } else if (potentialKey1.length() > potentialKey2.length()) {
                key = potentialKey2;
            }

            pathToProperty = StringUtils.substringAfter(pathToProperty, "/" + key);

            if (StringUtils.isBlank(pathToProperty) && jsonObject.has(key)) {
                propertiesResult.put(propertyName, jsonObject.get(key));
            } else {
                if (StringUtils.startsWith(pathToProperty, "/") && jsonObject.has(key)) {
                    extractPropertyFromJSON(propertiesResult, jsonObject.getJSONObject(key), null, pathToProperty, propertyName);
                } else if (jsonObject.has(key)) {
                    extractPropertyFromJSON(propertiesResult, null, jsonObject.getJSONArray(key), pathToProperty, propertyName);
                }
            }
        } else {
            int arrayIndex = new Integer(StringUtils.substringBetween(pathToProperty, "[", "]"));
            pathToProperty = StringUtils.substringAfter(pathToProperty, "]");
            if (StringUtils.isBlank(pathToProperty) && jsonArray.length() >= arrayIndex) {
                propertiesResult.put(propertyName, jsonArray.get(arrayIndex));
            } else {
                if (StringUtils.startsWith(pathToProperty, "/") && jsonArray.length() >= arrayIndex) {
                    extractPropertyFromJSON(propertiesResult, jsonArray.getJSONObject(arrayIndex), null, pathToProperty, propertyName);
                } else if (jsonArray.length() >= arrayIndex) {
                    extractPropertyFromJSON(propertiesResult, null, jsonArray.getJSONArray(arrayIndex), pathToProperty, propertyName);
                }
            }
        }
    }

    private OAuth20Service createOAuth20Service(JCRNodeWrapper connectorNode, String serviceName, String state) throws RepositoryException {
        List<String> callbackUrls = new ArrayList<>();
        String callbackUrl;
        for (JCRValueWrapper wrapper : connectorNode.getProperty(JahiaOAuthConstants.PROPERTY_CALLBACK_URLS).getValues()) {
            callbackUrls.add(wrapper.getString());
        }
        callbackUrl = callbackUrls.get(new Random().nextInt(callbackUrls.size()));

        ServiceBuilder serviceBuilder = new ServiceBuilder(connectorNode.getPropertyAsString(JahiaOAuthConstants.PROPERTY_API_KEY))
                .apiSecret(connectorNode.getPropertyAsString(JahiaOAuthConstants.PROPERTY_API_SECRET))
                .callback(callbackUrl);

        if (state != null) {
            serviceBuilder.state(state);
        }

        if (connectorNode.hasProperty(JahiaOAuthConstants.PROPERTY_SCOPE) && StringUtils.isNotBlank(connectorNode.getPropertyAsString(JahiaOAuthConstants.PROPERTY_SCOPE))) {
            serviceBuilder.scope(connectorNode.getPropertyAsString(JahiaOAuthConstants.PROPERTY_SCOPE));
        }

        return serviceBuilder.build(oAuthBase20ApiMap.get(serviceName));
    }

    @Override
    public void addOAuth20Service(String key, BaseApi<? extends OAuth20Service> oAuth20Service) {
        oAuthBase20ApiMap.put(key, oAuth20Service);
    }

    @Override
    public void removeOAuth20Service(String key) {
        if (oAuthBase20ApiMap.containsKey(key)) {
            oAuthBase20ApiMap.remove(key);
        }
    }

    @Override
    public void removeOAuth20Service(BaseApi<? extends OAuth20Service> oAuth20Service) {
        String key = null;
        Iterator<Map.Entry<String, BaseApi<? extends OAuth20Service>>> it = oAuthBase20ApiMap.entrySet().iterator();
        Map.Entry<String, BaseApi<? extends OAuth20Service>> entry;
        while (key == null && it.hasNext()) {
            entry = it.next();
            if (entry.getValue().equals(oAuth20Service)) {
                key = entry.getKey();
            }
        }
        if (key != null) {
            oAuthBase20ApiMap.remove(key);
        }
    }

    public void setJahiaOAuthCacheService(JahiaOAuthCacheService jahiaOAuthCacheService) {
        this.jahiaOAuthCacheService = jahiaOAuthCacheService;
    }
}
