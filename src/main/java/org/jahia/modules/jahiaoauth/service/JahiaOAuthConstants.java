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

import org.jahia.modules.jahiaauth.service.JahiaAuthConstants;

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
