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
package org.jahia.modules.jahiaoauth.service;

import com.github.scribejava.core.builder.api.DefaultApi20;
import org.jahia.modules.jahiaauth.service.ConnectorConfig;

import java.util.Map;

/**
 * Service to be used by connectors and mappers
 *
 * @author dgaillard
 */
public interface JahiaOAuthService {

    /**
     * This method will get the authorization URL so a connector can display the authentication popup to the user
     *
     * @param config    The oauth config for the connector
     * @param sessionId String user session ID to be able to identify the token on the callback the session ID of the user is added to the request
     * @return String authorization URL
     */
    String getAuthorizationUrl(ConnectorConfig config, String sessionId);

    /**
     * This method will get the authorization URL so a connector can display the authentication popup to the user
     *
     * @param config           The oauth config for the connector
     * @param sessionId        String user session ID to be able to identify the token on the callback the session ID of the user is added to the request
     * @param additionalParams additional parameter required to get the authorization URL
     * @return String authorization URL
     */
    String getAuthorizationUrl(ConnectorConfig config, String sessionId, Map<String, String> additionalParams);

    /**
     * This method will extract the token and execute the mappers action
     *
     * @param config The oauth config for the connector
     * @param token  String token send by the OAuth API
     * @param state  String state send back by OAuth API in this context it's the user session ID
     * @throws Exception
     */
    void extractAccessTokenAndExecuteMappers(ConnectorConfig config, String token, String state) throws Exception;

    /**
     * This method will return the URL of the result page so the user can be inform of the succes or not of his authentication
     *
     * @param siteUrl        String current site URL
     * @param isAuthenticate Boolean will be added to the URL as parameter
     * @return String URL of the result page
     */
    String getResultUrl(String siteUrl, Boolean isAuthenticate);

    /**
     * This method will refresh the access token of the user
     *
     * @param config       The oauth config for the connector
     * @param refreshToken String the refresh token
     * @return Map containing the data of the access token
     * @throws Exception
     */
    Map<String, Object> refreshAccessToken(ConnectorConfig config, String refreshToken) throws Exception;

    /**
     * This method will register a new Scribe Api 2.0 implementation
     *
     * @param key               api key
     * @param oAuthDefaultApi20 scribe Api 2.0 implementation
     */
    void addOAuthDefaultApi20(String key, DefaultApi20 oAuthDefaultApi20);

    /**
     * This method will unregister a scribe Api 2.0 by its key
     *
     * @param key api key
     */
    void removeOAuthDefaultApi20(String key);

    /**
     * This method will unregister a scribe Api 2.0 by its implementation
     *
     * @param oAuthDefaultApi20 api implementation
     */
    void removeOAuthDefaultApi20(DefaultApi20 oAuthDefaultApi20);
}
