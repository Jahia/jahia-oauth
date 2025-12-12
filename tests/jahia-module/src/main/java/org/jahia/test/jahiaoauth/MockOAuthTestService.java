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

/**
 * Test component that registers mock OAuth APIs for testing purposes.
 * This allows tests to redirect OAuth flows to a mock server instead of real OAuth providers.
 */
@Component(immediate = true, service = MockOAuthTestService.class)
public class MockOAuthTestService {
    private static final Logger logger = LoggerFactory.getLogger(MockOAuthTestService.class);

    private JahiaOAuthService jahiaOAuthService;
    private static final String MOCKED_BASE_URL = System.getenv("WIREMOCK_URL");
    private static final String GOOGLE_MOCKED_URL = MOCKED_BASE_URL + "/google-mocked";
    private static final String GOOGLE_MOCKED = "mockedGoogleAPI";
    private static final String FACEBOOK_MOCKED_URL = MOCKED_BASE_URL + "/facebook-mocked";
    private static final String FACEBOOK_MOCKED = "mockedFacebookAPI";
    private static final String LINKEDIN_MOCKED_URL = MOCKED_BASE_URL + "/linkedin-mocked";
    private static final String LINKEDIN_MOCKED = "mockedLinkedInAPI";
    private static final String GITHUB_MOCKED_URL = MOCKED_BASE_URL + "/github-mocked";
    private static final String GITHUB_MOCKED = "mockedGitHubAPI";

    @Reference
    protected void setJahiaOAuthService(JahiaOAuthService jahiaOAuthService) {
        this.jahiaOAuthService = jahiaOAuthService;
    }

    @Activate
    public void activate() {
        logger.info("Activating Mock OAuth Test Service with mock base URL {} ...", MOCKED_BASE_URL);
        jahiaOAuthService.addOAuthDefaultApi20(GOOGLE_MOCKED, connectorConfig -> new MockApi20(GOOGLE_MOCKED_URL));
        jahiaOAuthService.addOAuthDefaultApi20(FACEBOOK_MOCKED, connectorConfig -> new MockApi20(FACEBOOK_MOCKED_URL));
        jahiaOAuthService.addOAuthDefaultApi20(LINKEDIN_MOCKED, connectorConfig -> new MockApi20(LINKEDIN_MOCKED_URL));
        jahiaOAuthService.addOAuthDefaultApi20(GITHUB_MOCKED, connectorConfig -> new MockApi20(GITHUB_MOCKED_URL));

        logger.info("=== Mock OAuth APIs registered successfully ===");
    }

    @Deactivate
    public void deactivate() {
        logger.info("Deactivating Mock OAuth Test Service...");
        jahiaOAuthService.removeOAuthDefaultApi20(GOOGLE_MOCKED);
        jahiaOAuthService.removeOAuthDefaultApi20(FACEBOOK_MOCKED);
        jahiaOAuthService.removeOAuthDefaultApi20(LINKEDIN_MOCKED);
        jahiaOAuthService.removeOAuthDefaultApi20(GITHUB_MOCKED);
        logger.info("=== Mock OAuth APIs unregistered ===");
    }
}

