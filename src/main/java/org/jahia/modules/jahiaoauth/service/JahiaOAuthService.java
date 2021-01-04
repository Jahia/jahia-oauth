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
