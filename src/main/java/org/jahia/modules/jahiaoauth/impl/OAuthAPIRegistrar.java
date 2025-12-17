/*
 * Copyright (C) 2002-2025 Jahia Solutions Group SA. All rights reserved.
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
import com.github.scribejava.core.builder.api.DefaultApi20;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthService;
import org.jahia.modules.scribejava.apis.FranceConnectApi;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * OSGi component responsible for registering all OAuth 2.0 API implementations
 * with the JahiaOAuthService at module startup.
 *
 * <p>This component acts as a bootstrap/initializer that populates the OAuth service
 * with all supported OAuth provider APIs from the ScribeJava library.</p>
 *
 * <p>The registration happens once during component activation, making all OAuth APIs
 * immediately available for use by connectors.</p>
 *
 */
@Component(immediate = true)
public class OAuthAPIRegistrar {
    private static final Logger logger = LoggerFactory.getLogger(OAuthAPIRegistrar.class);

    /**
     * Map of OAuth API keys to their corresponding API instance suppliers.
     */
    private static final Map<String, Supplier<DefaultApi20>> API_REGISTRY = new HashMap<>();

    static {
        // Register standard OAuth APIs from ScribeJava library
        API_REGISTRY.put("LinkedInApi20", LinkedInApi20::instance);
        API_REGISTRY.put("VkontakteApi", VkontakteApi::instance);
        API_REGISTRY.put("HHApi", HHApi::instance);
        API_REGISTRY.put("GitHubApi", GitHubApi::instance);
        API_REGISTRY.put("MailruApi", MailruApi::instance);
        API_REGISTRY.put("GeniusApi", GeniusApi::instance);
        API_REGISTRY.put("Foursquare2Api", Foursquare2Api::instance);
        API_REGISTRY.put("RenrenApi", RenrenApi::instance);
        API_REGISTRY.put("KaixinApi20", KaixinApi20::instance);
        API_REGISTRY.put("ViadeoApi", ViadeoApi::instance);
        API_REGISTRY.put("GoogleApi20", GoogleApi20::instance);
        API_REGISTRY.put("PinterestApi", PinterestApi::instance);
        API_REGISTRY.put("SinaWeiboApi20", SinaWeiboApi20::instance);
        API_REGISTRY.put("OdnoklassnikiApi", OdnoklassnikiApi::instance);
        API_REGISTRY.put("FacebookApi", () -> FacebookApi.customVersion("7.0"));
        API_REGISTRY.put("TutByApi", TutByApi::instance);
        API_REGISTRY.put("LiveApi", LiveApi::instance);
        API_REGISTRY.put("DoktornaraboteApi", DoktornaraboteApi::instance);
        API_REGISTRY.put("NaverApi", NaverApi::instance);
        API_REGISTRY.put("MisfitApi", MisfitApi::instance);
        API_REGISTRY.put("StackExchangeApi", StackExchangeApi::instance);
        API_REGISTRY.put("ImgurApi", ImgurApi::instance);

        // Register FranceConnect APIs with custom configuration
        API_REGISTRY.put("FranceConnectApi", () -> {
            FranceConnectApi api = FranceConnectApi.instance();
            api.setAccessTokenEndpoint("https://app.franceconnect.gouv.fr/api/v1/token");
            api.setAuthorizationBaseUrl("https://app.franceconnect.gouv.fr/api/v1/authorize");
            return api;
        });
        API_REGISTRY.put("FranceConnectApiDev", () -> {
            FranceConnectApi api = FranceConnectApi.instance();
            api.setAccessTokenEndpoint("https://fcp.integ01.dev-franceconnect.fr/api/v1/token");
            api.setAuthorizationBaseUrl("https://fcp.integ01.dev-franceconnect.fr/api/v1/authorize");
            return api;
        });
    }

    @Reference
    private JahiaOAuthService jahiaOAuthService;

    /**
     * Activates the component and registers all OAuth 2.0 API implementations.
     * This method is called by the OSGi framework when the component starts.
     *
     * <p>APIs are registered with their standard keys (e.g., "GoogleApi20", "FacebookApi")
     * which must match the connector configuration property "oauthApiName".</p>
     */
    @Activate
    public void activate() {
        logger.info("Registering {} OAuth 2.0 API implementations...", API_REGISTRY.size());

        API_REGISTRY.forEach((apiKey, apiSupplier) -> {
            registerAPI(apiKey, apiSupplier.get());
            logger.debug("Registered OAuth API: {}", apiKey);
        });

        logger.info("Successfully registered th OAuth 2.0 API implementations.");
    }

    /**
     * Deactivates the component and unregisters all OAuth 2.0 API implementations.
     * This method is called by the OSGi framework when the component stops.
     *
     * <p>This ensures clean removal of all registered APIs when the module is uninstalled
     * or the component is stopped.</p>
     */
    @Deactivate
    public void deactivate() {
        logger.info("Unregistering {} OAuth 2.0 API implementations...", API_REGISTRY.size());

        API_REGISTRY.keySet().forEach(apiKey -> {
            unregisterAPI(apiKey);
            logger.debug("Unregistered OAuth API: {}", apiKey);
        });

        logger.info("Successfully unregistered the OAuth 2.0 API implementations.");
    }

    /**
     * Helper method to register an OAuth API with the service.
     *
     * @param apiKey the unique key identifying this API (used in connector configuration)
     * @param api    the DefaultApi20 implementation to register
     */
    private void registerAPI(String apiKey, DefaultApi20 api) {
        JahiaOAuthDefaultAPIBuilder builder = new JahiaOAuthDefaultAPIBuilder();
        builder.setDefaultApi20(api);
        jahiaOAuthService.addOAuthDefaultApi20(apiKey, builder);
    }

    /**
     * Helper method to unregister an OAuth API from the service.
     *
     * @param apiKey the unique key identifying the API to unregister
     */
    private void unregisterAPI(String apiKey) {
        jahiaOAuthService.removeOAuthDefaultApi20(apiKey);
    }
}

