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

import com.github.scribejava.core.builder.api.DefaultApi20;

/**
 * Mock OAuth API for testing purposes.
 * Redirects to a local test server instead of real OAuth servers.
 */
public class MockApi20 extends DefaultApi20 {

    private final String mockServerUrl;

    public MockApi20(String mockServerUrl) {
        this.mockServerUrl = mockServerUrl;
    }

    @Override
    public String getAccessTokenEndpoint() {
        return mockServerUrl + "/oauth/token";
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return mockServerUrl + "/oauth/authorize";
    }
}

