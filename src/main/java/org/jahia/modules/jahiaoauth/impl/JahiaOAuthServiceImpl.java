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

import com.github.scribejava.apis.*;
import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.jahiaauth.service.*;
import org.jahia.modules.jahiaoauth.service.*;
import org.jahia.modules.scribejava.apis.FranceConnectApi;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.osgi.BundleUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jahia.modules.jahiaoauth.impl.flow.AuthorizationFlow;
import org.jahia.modules.jahiaoauth.impl.token.IdToken;
import org.jahia.modules.jahiaoauth.impl.token.IdTokenException;
import org.jahia.modules.jahiaoauth.impl.token.IdTokenValidator;
import org.jahia.modules.jahiaoauth.impl.token.TokenExchangePolicy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * Main OAuth service implementation for Jahia OAuth module.
 * Manages OAuth 2.0 operations including authorization, token exchange, and user mapping.
 *
 * @author dgaillard
 */
@Component(service = JahiaOAuthService.class, immediate = true)
public class JahiaOAuthServiceImpl implements JahiaOAuthService {

    private static final String NONCE_PARAM = "nonce";
    /** Optional: the issuer a connector expects in the identity token. */
    private static final String EXPECTED_ISSUER_PROPERTY = "issuer";
    /** Set on a disposable test instance only: accepts a token endpoint served over http. */
    private static final String ALLOW_INSECURE_ENDPOINT_PROPERTY = "allowInsecureTokenEndpoint";
    private static final Logger logger = LoggerFactory.getLogger(JahiaOAuthServiceImpl.class);
    private static final Configuration JSONPATH_CONFIG = Configuration.builder().build();

    private final Map<String, JahiaOAuthAPIBuilder> oAuthDefaultApi20Map;

    @Reference
    private JahiaAuthMapperService jahiaAuthMapperService;

    public JahiaOAuthServiceImpl() {
        this.oAuthDefaultApi20Map = new ConcurrentHashMap<>();
    }

    @Activate
    public void activate() {
        // Simple APIs
        addOAuthDefaultApi20("LinkedInApi20", LinkedInApi20.instance());
        addOAuthDefaultApi20("VkontakteApi", VkontakteApi.instance());
        addOAuthDefaultApi20("HHApi", HHApi.instance());
        addOAuthDefaultApi20("GitHubApi", GitHubApi.instance());
        addOAuthDefaultApi20("MailruApi", MailruApi.instance());
        addOAuthDefaultApi20("GeniusApi", GeniusApi.instance());
        addOAuthDefaultApi20("Foursquare2Api", Foursquare2Api.instance());
        addOAuthDefaultApi20("RenrenApi", RenrenApi.instance());
        addOAuthDefaultApi20("KaixinApi20", KaixinApi20.instance());
        addOAuthDefaultApi20("ViadeoApi", ViadeoApi.instance());
        addOAuthDefaultApi20("GoogleApi20", GoogleApi20.instance());
        addOAuthDefaultApi20("PinterestApi", PinterestApi.instance());
        addOAuthDefaultApi20("SinaWeiboApi20", SinaWeiboApi20.instance());
        addOAuthDefaultApi20("OdnoklassnikiApi", OdnoklassnikiApi.instance());
        addOAuthDefaultApi20("TutByApi", TutByApi.instance());
        addOAuthDefaultApi20("LiveApi", LiveApi.instance());
        addOAuthDefaultApi20("DoktornaraboteApi", DoktornaraboteApi.instance());
        addOAuthDefaultApi20("NaverApi", NaverApi.instance());
        addOAuthDefaultApi20("MisfitApi", MisfitApi.instance());
        addOAuthDefaultApi20("StackExchangeApi", StackExchangeApi.instance());
        addOAuthDefaultApi20("ImgurApi", ImgurApi.instance());
        addOAuthDefaultApi20("FacebookApi", FacebookApi.customVersion("7.0"));
        // Register FranceConnect APIs with custom configuration
        addOAuthDefaultApi20("FranceConnectApi", new FranceConnectApi() {{
            setAccessTokenEndpoint("https://app.franceconnect.gouv.fr/api/v1/token");
            setAuthorizationBaseUrl("https://app.franceconnect.gouv.fr/api/v1/authorize");
        }});
        addOAuthDefaultApi20("FranceConnectApiDev", new FranceConnectApi() {{
            setAccessTokenEndpoint("https://fcp.integ01.dev-franceconnect.fr/api/v1/token");
            setAuthorizationBaseUrl("https://fcp.integ01.dev-franceconnect.fr/api/v1/authorize");
        }});

        logger.info("JahiaOAuthService activated");
    }

    @Override
    public String getAuthorizationEndpointUrl(ConnectorConfig config) {
        return createOAuth20Service(config, defaultApi20(config)).createAuthorizationUrlBuilder().build();
    }

    @Override
    public String getAuthorizationUrl(HttpServletRequest httpRequest, ConnectorConfig config, Map<String, String> additionalParams) {
        // A nonce is sent whenever the provider returns an identity token, and the API that performs
        // the token call is what states that. No configuration reaches the answer and no connector
        // states it.
        DefaultApi20 api = defaultApi20(config);
        AuthorizationFlow flow = AuthorizationFlow.start(httpRequest, config.getConnectorName(),
                TokenExchangePolicy.returnsIdentityToken(api));

        Map<String, String> params = additionalParams == null ? new HashMap<>() : new HashMap<>(additionalParams);
        if (flow.getNonce() != null) {
            params.put(NONCE_PARAM, flow.getNonce());
        }
        return createOAuth20Service(config, api).createAuthorizationUrlBuilder()
                .additionalParams(params).state(flow.getState()).build();
    }

    @Override
    public String getResultUrl(String siteUrl, Boolean isAuthenticate) {
        return StringUtils.substringBeforeLast(siteUrl, ".html") + "/oauth-result.html?isAuthenticate=" + isAuthenticate;
    }

    @Override
    public Map<String, Object> refreshAccessToken(ConnectorConfig config, String refreshToken) throws Exception {
        DefaultApi20 api = defaultApi20(config);
        // A refreshed identity token rests on the transport exactly as the first one does.
        requireSecureTokenEndpoint(config, api);
        // The service states no scope. scribejava sends the default scope of the service when a caller
        // names none, and the scope this framework derives adds openid. RFC 6749 section 6 refuses a
        // scope the grant never carried, so a grant made without openid is answered invalid_scope.
        OAuth20Service service = createOAuth20Service(config, api, false);
        OAuth2AccessToken accessToken = service.refreshAccessToken(refreshToken);
        if (accessToken instanceof OpenIdOAuth2AccessToken) {
            try {
                IdToken idToken = IdToken.parse(((OpenIdOAuth2AccessToken) accessToken).getOpenIdToken());
                IdTokenValidator.validateRefreshed(idToken,
                        config.getProperty(JahiaOAuthConstants.PROPERTY_API_KEY),
                        config.getProperty(EXPECTED_ISSUER_PROPERTY), System.currentTimeMillis() / 1000L);
            } catch (IdTokenException e) {
                throw new JahiaOAuthException("The refreshed identity token of connector "
                        + config.getConnectorName() + " was refused: " + e.getMessage(), e);
            }
        }
        return extractAccessTokenData(accessToken);
    }

    @Override
    @SuppressWarnings("java:S3776")
    public void extractAccessTokenAndExecuteMappers(ConnectorConfig config, String token, String receivedState,
            HttpServletRequest httpRequest) throws JahiaOAuthException {
        // Check the callback answers a flow this session started, before exchanging anything. A
        // callback answering none therefore costs one comparison and no call to the identity provider.
        AuthorizationFlow flow = AuthorizationFlow.consume(httpRequest, config.getConnectorName(), receivedState);
        if (flow == null) {
            throw new JahiaOAuthException("The callback for connector " + config.getConnectorName()
                    + " answers no sign-in this session started");
        }

        DefaultApi20 api = defaultApi20(config);
        requireSecureTokenEndpoint(config, api);
        OAuth20Service service = createOAuth20Service(config, api);
        OAuth2AccessToken accessToken = getAccessToken(service, token);
        String verifiedSubject = validateIdToken(config, api, accessToken, flow);

        OAuthConnectorService connectorService = connectorService(config);

        Map<String, Object> propertiesResult = new HashMap<>();

        List<String> urlsToProcess = connectorService.getProtectedResourceUrls(config);

        for (String url : urlsToProcess) {
            // Request all the properties available right now
            OAuthRequest request = new OAuthRequest(Verb.GET, url);
            request.addHeader("x-li-format", "json");
            service.signRequest(accessToken, request);
            Response response = executeRequest(service, request);
            String responseBody = readBody(response);

            // if we got the properties then execute mapper
            if (response.getCode() == HttpServletResponse.SC_OK) {
                JSONObject responseJson;
                try {
                    responseJson = new JSONObject(responseBody);
                    if (logger.isDebugEnabled()) {
                        logger.debug(responseJson.toString());
                    }

                    // Store in a simple map the results by properties as mapped in the connector
                    propertiesResult.putAll(getPropertiesResult(connectorService, responseJson));

                    // Enhance properties with custom mapping that may be configured in mappers
                    propertiesResult.putAll(getEnhancedPropertiesForMappers(propertiesResult, config, responseJson));
                } catch (Exception e) {
                    // The body holds the profile of the person signing in, so it is not written at error
                    // level. An operator who needs it raises this class to debug.
                    logger.error("Did not received expected json, response message was: {}", response.getMessage());
                    logger.debug("Response body was: {}", responseBody);
                    throw new JahiaOAuthException("Did not receive the expected JSON from the protected resource", e);
                }

                // Outside the block above, because that block reports a body it could not read. A
                // subject that does not match is a body this flow read and refuses to use, and reporting
                // it as unreadable JSON would name the wrong defect and log the whole profile with it.
                requireSameSubject(config.getConnectorName(), verifiedSubject, responseJson.optString("sub", null));
            } else if (urlsToProcess.size() > 1 && response.getCode() == HttpServletResponse.SC_FORBIDDEN) {
                // In case of multiple url, it is possible that not all available.
                // Do nothing in that case - we check at the end if all properties are filled
            } else {
                logger.error("Did not received expected response, response code: {}, response message: {} response body was: {}",
                        response.getCode(), response.getMessage(), responseBody);
                throw new JahiaOAuthException(
                        "Did not received expected response, response code: " + response.getCode() + ", response message: " + response
                                .getMessage() + " response body was: " + responseBody);
            }
        }

        try {
            addTokensData(config.getConnectorName(), accessToken, propertiesResult, config.getSiteKey());

            // Get Mappers
            for (MapperConfig mapperConfig : config.getMappers()) {
                if (mapperConfig.isActive()) {
                    jahiaAuthMapperService.executeMapper(httpRequest, mapperConfig, propertiesResult);
                }
            }

            // Get Post Executions
            jahiaAuthMapperService.executeConnectorResultProcessors(httpRequest, config, propertiesResult);
        } catch (Exception e) {
            throw new JahiaOAuthException("Something when wrong in OAuth with config " + config.getConnectorName(), e);
        }
    }

    private OAuth2AccessToken getAccessToken(OAuth20Service service, String token) throws JahiaOAuthException {
        try {
            return service.getAccessToken(token);
        } catch (IOException | ExecutionException e) {
            throw new JahiaOAuthException("Unable to retrieve the OAuth access token", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JahiaOAuthException("Interrupted while retrieving the OAuth access token", e);
        }
    }

    private Response executeRequest(OAuth20Service service, OAuthRequest request) throws JahiaOAuthException {
        try {
            return service.execute(request);
        } catch (IOException | ExecutionException e) {
            throw new JahiaOAuthException("Unable to query the OAuth protected resource", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JahiaOAuthException("Interrupted while querying the OAuth protected resource", e);
        }
    }

    private String readBody(Response response) throws JahiaOAuthException {
        try {
            return response.getBody();
        } catch (IOException e) {
            throw new JahiaOAuthException("Unable to read the OAuth protected resource response body", e);
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

    /**
     * Iterates over mappers to find and extract properties that are available in the JSON response but weren't explicitly requested by the connector.
     */
    private Map<String, Object> getEnhancedPropertiesForMappers(Map<String, Object> propertiesResult, ConnectorConfig config,
            JSONObject responseJson) {

        Map<String, Object> enhancedProperties = new HashMap<>();
        for (MapperConfig mapperConfig : config.getMappers()) {
            if (mapperConfig.isActive()) {
                mapperConfig.getMappings().forEach(mapping -> {
                    Map.Entry<String, Object> entry = extractEnhancedPropertyEntryForMapping(propertiesResult, responseJson, mapping);
                    if (entry != null) {
                        Object previousValue = enhancedProperties.put(entry.getKey(), entry.getValue());
                        if (previousValue != null) {
                            logger.debug("Property '{}' got overwritten - old value: '{}', new value: '{}'", entry.getKey(), previousValue,
                                    entry.getValue());
                        }
                    }
                });
            }
        }
        return enhancedProperties;
    }

    /**
     * Extracts a single property for a mapping if it is present in the JSON response and has not
     * already been extracted via the connector's {@code availableProperties}.
     * <p>
     * Connector property values starting with {@code $} are evaluated as JSONPath expressions via
     * the Jayway JsonPath library. Plain property names (no {@code $} prefix) fall back to a
     * top-level {@code has}/{@code opt} lookup.
     * <p>
     * All value types are returned as-is. Note that complex types (e.g. {@code JSONObject}) will
     * be converted to their string representation by {@code JahiaAuthMapperServiceImpl.executeMapper}
     * when stored in the cache; they are only usable as-is in a custom {@link ConnectorResultProcessor}.
     * <p>
     * Jayway note: filter expressions always return a list, even when only one node matches
     * (e.g. {@code $.groups[?(@.name == 'admin')]} returns {@code ["admin"]}, not {@code "admin"}).
     * Additionally, chaining an index selector after a filter — e.g. {@code [?(@.x == 'y')][1]} —
     * does <em>not</em> index into the filtered nodelist; {@code [1]} is applied to each matched
     * node individually and produces nothing since matched nodes are objects, not arrays.
     */
    private static Map.Entry<String, Object> extractEnhancedPropertyEntryForMapping(Map<String, Object> propertiesResult, JSONObject responseJson, Mapping mapping) {
        String connectorProperty = mapping.getConnectorProperty();
        logger.debug("Enhancing property '{}' requested by mapper", connectorProperty);

        // Skip if already extracted via connector's availableProperties
        if (propertiesResult.containsKey(connectorProperty)) {
            logger.debug("Property '{}' already extracted via connector's availableProperties", connectorProperty);
            return null;
        }

        Object matchingProperty;

        if (connectorProperty.startsWith("$")) {
            // RFC 9535 JSONPath expression
            logger.debug("Evaluating the connector property {} as a JSONPath expression", connectorProperty);
            matchingProperty = evaluateJsonPath(responseJson, connectorProperty);
            if (matchingProperty == null) {
                logger.debug("JSONPath expression '{}' returned no result in JSON response", connectorProperty);
                return null;
            }
        } else {
            logger.debug("Evaluating the connector property {} as a plain property name lookup", connectorProperty);
            if (!responseJson.has(connectorProperty)) {
                logger.debug("Property '{}' not found in JSON response", connectorProperty);
                return null;
            }
            matchingProperty = responseJson.opt(connectorProperty);
            if (JSONObject.NULL.equals(matchingProperty)) {
                matchingProperty = null;
            }
        }

        logger.debug("Property '{}' found in JSON response with value '{}'", connectorProperty, matchingProperty);
        // NB: the complex types (JSONObject, or any other unexpected types) can only be used in a custom ConnectorResultProcessor
        // in JahiaAuthMapperServiceImpl.executeMapper(...), the property result is stored in the cache as a string: String.valueOf(...)
        return new AbstractMap.SimpleEntry<>(connectorProperty, matchingProperty);
    }

    /**
     * Evaluates a JSONPath expression (RFC 9535) against the given JSON object.
     * Returns {@code null} if the path does not exist or evaluation fails.
     */
    private static Object evaluateJsonPath(JSONObject responseJson, String jsonPathExpression) {
        try {
            return JsonPath.using(JSONPATH_CONFIG).parse(responseJson.toString()).read(jsonPathExpression);
        } catch (Exception e) {
            logger.warn("Failed to evaluate JSONPath expression '{}': {}", jsonPathExpression, e.getMessage());
            logger.debug("Stacktrace", e);
            return null;
        }
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

    /**
     * Refuses a protected-resource response that does not describe the subject of the identity token.
     * <p>
     * The claims a mapper reads come from this response, and not from the identity token, so the four
     * checks on the token say nothing about them on their own. OpenID Connect Core 5.3.2 asks for the
     * {@code sub} of the response to match the {@code sub} of the token, and forbids using the response
     * when it does not. That comparison is what ties the checked token to the values that decide which
     * account is signed in.
     * <p>
     * A response that states no subject is refused as well. The specification allows no exception, and
     * accepting one would let whoever writes the response drop the claim to skip the comparison, which
     * is the whole of what this method does.
     *
     * @param connectorName the connector that read the response
     * @param verifiedSubject the subject of the identity token, {@code null} when this flow received no
     *        identity token and therefore has nothing to tie the response to
     * @param responseSubject the subject the response states, {@code null} when it states none
     */
    static void requireSameSubject(String connectorName, String verifiedSubject, String responseSubject)
            throws JahiaOAuthException {
        if (verifiedSubject == null) {
            return;
        }
        if (!verifiedSubject.equals(responseSubject)) {
            throw new JahiaOAuthException("Connector " + connectorName + " read a protected resource that"
                    + " does not describe the subject of the identity token, so its values are not used");
        }
    }

    /**
     * @return the subject the identity token carries, or {@code null} when there is no token to check
     */
    private String validateIdToken(ConnectorConfig config, DefaultApi20 api, OAuth2AccessToken accessToken,
            AuthorizationFlow flow) throws JahiaOAuthException {
        if (!(accessToken instanceof OpenIdOAuth2AccessToken)) {
            if (TokenExchangePolicy.returnsIdentityToken(api)) {
                throw new JahiaOAuthException("Connector " + config.getConnectorName()
                        + " reads identity tokens, and the identity provider returned none");
            }
            return null;
        }
        // Every identity token that arrives is checked, whether or not this flow asked for one. The
        // validator refuses a token this flow cannot bind, so a flow that sent no nonce fails here
        // rather than signing a user in with a token that answers another sign-in.
        try {
            IdToken idToken = IdToken.parse(((OpenIdOAuth2AccessToken) accessToken).getOpenIdToken());
            IdTokenValidator.validate(idToken, config.getProperty(JahiaOAuthConstants.PROPERTY_API_KEY),
                    config.getProperty(EXPECTED_ISSUER_PROPERTY), flow.getNonce(),
                    System.currentTimeMillis() / 1000L);
            return idToken.getClaim("sub");
        } catch (IdTokenException e) {
            throw new JahiaOAuthException("The identity token of connector " + config.getConnectorName()
                    + " was refused: " + e.getMessage(), e);
        }
    }

    private static OAuthConnectorService connectorService(ConnectorConfig config) {
        OAuthConnectorService connectorService = BundleUtils.getOsgiService(OAuthConnectorService.class,
                "(" + JahiaAuthConstants.CONNECTOR_SERVICE_NAME + "=" + config.getConnectorName() + ")");
        if (connectorService == null) {
            throw new JahiaRuntimeException("Connector service was null for service name: " + config.getConnectorName());
        }
        return connectorService;
    }

    /**
     * Refuses a token endpoint that is not served over TLS.
     * <p>
     * The identity token is accepted on the strength of the transport rather than of a signature, and
     * OpenID Connect Core 3.1.3.7 permits that for a token the client receives over a validated TLS
     * connection. This method holds that condition, so the exemption rests on something checked.
     * <p>
     * The endpoint is read from the API object that performs the token call. A connector stores the
     * endpoint under a property name of its own choosing, and one connector builds it from two other
     * properties and stores it nowhere, so the API object is the one place that always states it.
     */
    static void requireSecureTokenEndpoint(ConnectorConfig config, DefaultApi20 api) {
        if (!TokenExchangePolicy.isSecureTokenEndpoint(api)
                && !Boolean.parseBoolean(config.getProperty(ALLOW_INSECURE_ENDPOINT_PROPERTY))) {
            throw new JahiaRuntimeException("Connector " + config.getConnectorName() + " reads its token"
                    + " endpoint over http, and the identity token is accepted without a signature check"
                    + " because the call is expected to run over TLS. Use https, or set "
                    + ALLOW_INSECURE_ENDPOINT_PROPERTY + "=true for a disposable test instance.");
        }
    }

    /**
     * @return the API that talks to this connector's provider. Two questions are read from it rather
     *         than from the connector, so a connector cannot omit either answer. See
     *         {@link TokenExchangePolicy}.
     */
    private DefaultApi20 defaultApi20(ConnectorConfig config) {
        String apiName = config.getProperty("oauthApiName") != null
                ? config.getProperty("oauthApiName") : config.getConnectorName();
        return oAuthDefaultApi20Map.get(apiName).build(config);
    }

    private static OAuth20Service createOAuth20Service(ConnectorConfig config, DefaultApi20 api) {
        return createOAuth20Service(config, api, true);
    }

    /**
     * @param askForScope whether the service states a scope. A sign-in asks for the scope an identity
     *                    token depends on. A refresh names a grant that carries its scope already, and a
     *                    service stating one would make scribejava send it.
     */
    private static OAuth20Service createOAuth20Service(ConnectorConfig config, DefaultApi20 api,
            boolean askForScope) {
        String callbackUrl = config.getProperty(JahiaOAuthConstants.PROPERTY_CALLBACK_URL);

        ServiceBuilder serviceBuilder = new ServiceBuilder(config.getProperty(JahiaOAuthConstants.PROPERTY_API_KEY))
                .apiSecret(config.getProperty(JahiaOAuthConstants.PROPERTY_API_SECRET)).callback(callbackUrl);

        String scope = scopeOfServiceFor(askForScope,
                TokenExchangePolicy.scopeFor(api, config.getProperty(JahiaOAuthConstants.PROPERTY_SCOPE)));
        if (scope != null) {
            serviceBuilder.withScope(scope);
        }

        return serviceBuilder.build(api);
    }

    /**
     * @return the scope a service states, or {@code null} when it states none
     */
    static String scopeOfServiceFor(boolean askForScope, String derivedScope) {
        return askForScope && StringUtils.isNotBlank(derivedScope) ? derivedScope : null;
    }

    @Override
    public void addOAuthDefaultApi20(String key, DefaultApi20 oAuthDefaultApi20) {
        oAuthDefaultApi20Map.put(key, new JahiaOAuthDefaultAPIBuilder(oAuthDefaultApi20));
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
}
