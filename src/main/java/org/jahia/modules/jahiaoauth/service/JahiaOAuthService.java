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

import javax.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * Service to be used by connectors and mappers
 *
 * @author dgaillard
 */
public interface JahiaOAuthService {

    /**
     * Starts a sign-in and returns the URL the browser goes to.
     * <p>
     * The service owns the secrets of the flow. It creates them, records them on the session and
     * sends them to the identity provider, so a connector never handles a state or a nonce and cannot
     * omit one.
     *
     * @param httpRequest      the request that starts the sign-in
     * @param config           The oauth config for the connector
     * @param additionalParams additional parameter required to get the authorization URL, may be {@code null}
     * @return String authorization URL
     */
    String getAuthorizationUrl(HttpServletRequest httpRequest, ConnectorConfig config, Map<String, String> additionalParams);

    /**
     * Builds the authorization URL of a connector without starting a sign-in.
     * <p>
     * A page that renders a link, and code that only wants the endpoint of the identity provider, call
     * this. {@link #getAuthorizationUrl} records a flow on the session, so calling it to display
     * something replaces the flow of a sign-in that is under way, and that sign-in then answers a state
     * the session no longer holds.
     *
     * @param config The oauth config for the connector
     * @return the authorization URL, carrying no state and no nonce
     */
    String getAuthorizationEndpointUrl(ConnectorConfig config);

    /**
     * Finishes a sign-in: checks that the callback answers a flow this session started, exchanges the
     * code, checks the identity token and runs the mappers.
     * <p>
     * The state and the nonce are checked here, before anything else happens, so every connector gets
     * both checks by calling this method. A callback that answers no flow of this session is refused
     * and no code is exchanged.
     *
     * @param config        The oauth config for the connector
     * @param token         String token send by the OAuth API
     * @param receivedState the state the identity provider returned on the callback
     * @param httpRequest   The request the identity provider called back. What the mappers resolve is
     *                      recorded on its session, and the SSO valve reads it back from there.
     * @throws JahiaOAuthException if the callback answers no flow, if the identity token does not
     *                             hold, or if the token exchange or the mapper execution fail
     */
    void extractAccessTokenAndExecuteMappers(ConnectorConfig config, String token, String receivedState,
            HttpServletRequest httpRequest) throws JahiaOAuthException;

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
     * This method will register a new Scribe Api 2.0 implementation builder
     * It allows to dynamically build the DefaultApi20 based on the connector config.
     * Very useful in case the DefaultApi20 need to use custom properties coming from the config for example.
     *
     * @param key               api key
     * @param apiBuilder custom builder of the api connector
     */
    void addOAuthDefaultApi20(String key, JahiaOAuthAPIBuilder apiBuilder);

    /**
     * This method will unregister a scribe Api 2.0 by its key for each site
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
