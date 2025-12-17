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
package org.jahia.test.jahiaoauth;

import org.jahia.modules.jahiaoauth.service.JahiaOAuthService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Test component that registers mock OAuth APIs for testing purposes.
 * This allows tests to redirect OAuth flows to a mock server instead of real OAuth providers.
 */
@Component(immediate = true, service = OAuthTestAPIRegistrar.class)
public class OAuthTestAPIRegistrar {
    private static final Logger logger = LoggerFactory.getLogger(OAuthTestAPIRegistrar.class);

    // Mock endpoints configuration
    private static final String MOCKED_BASE_URL = System.getenv("WIREMOCK_URL");

    /**
     * Map of mock OAuth API keys to their corresponding mock API suppliers.
     * Using a LinkedHashMap to maintain insertion order for logging purposes.
     */
    private static final Map<String, Supplier<MockApi20>> MOCK_API_REGISTRY = new LinkedHashMap<>();

    static {
        MOCK_API_REGISTRY.put("mockedGoogleAPI", () -> new MockApi20(MOCKED_BASE_URL + "/google-mocked"));
        MOCK_API_REGISTRY.put("mockedFacebookAPI", () -> new MockApi20(MOCKED_BASE_URL + "/facebook-mocked"));
        MOCK_API_REGISTRY.put("mockedLinkedInAPI", () -> new MockApi20(MOCKED_BASE_URL + "/linkedin-mocked"));
        MOCK_API_REGISTRY.put("mockedGitHubAPI", () -> new MockApi20(MOCKED_BASE_URL + "/github-mocked"));
    }

    private JahiaOAuthService jahiaOAuthService;

    @Reference
    protected void setJahiaOAuthService(JahiaOAuthService jahiaOAuthService) {
        this.jahiaOAuthService = jahiaOAuthService;
    }

    @Activate
    public void activate() {
        logger.info("Registering {} mock OAuth 2.0 API implementations (base URL: {})...", MOCK_API_REGISTRY.size(), MOCKED_BASE_URL);

        MOCK_API_REGISTRY.forEach((apiKey, apiSupplier) -> {
            jahiaOAuthService.addOAuthDefaultApi20(apiKey, connectorConfig -> apiSupplier.get());
            logger.debug("Registered mock OAuth API: {}", apiKey);
        });

        logger.info("Successfully registered the mock OAuth 2.0 API implementations.");
    }

    @Deactivate
    public void deactivate() {
        logger.info("Unregistering {} mock OAuth 2.0 API implementations...", MOCK_API_REGISTRY.size());

        MOCK_API_REGISTRY.keySet().forEach(apiKey -> {
            jahiaOAuthService.removeOAuthDefaultApi20(apiKey);
            logger.debug("Unregistered mock OAuth API: {}", apiKey);
        });

        logger.info("Successfully unregistered the mock OAuth 2.0 API implementations.");
    }
}

