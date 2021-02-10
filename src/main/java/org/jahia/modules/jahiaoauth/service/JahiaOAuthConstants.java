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

/**
 * Constants use across the application
 *
 * @author dgaillard
 */
public class JahiaOAuthConstants {
    public static final String PROPERTY_API_KEY = "apiKey";
    public static final String PROPERTY_API_SECRET = "apiSecret";
    public static final String PROPERTY_CALLBACK_URL = "callbackUrl";
    public static final String PROPERTY_SCOPE = "scope";

    public static final String STATE = "state";
    public static final String TOKEN_DATA = "tokenData";
    public static final String ACCESS_TOKEN = "accessToken";
    public static final String TOKEN_EXPIRES_IN = "expiresIn";
    public static final String REFRESH_TOKEN = "refreshToken";
    public static final String TOKEN_SCOPE = "tokenScope";
    public static final String TOKEN_TYPE = "tokenType";
    public static final String AUTHORIZATION_URL = "authorizationUrl";

    private JahiaOAuthConstants() {
    }
}
