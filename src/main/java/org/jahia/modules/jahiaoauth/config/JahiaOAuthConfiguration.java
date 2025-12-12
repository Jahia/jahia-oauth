/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2025 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.jahiaoauth.config;

import java.util.List;

/**
 * Configuration interface for Jahia OAuth connectors.
 * <p>
 * This interface provides configuration for various OAuth providers by defining
 * their respective user information endpoints. These endpoints are used by OAuth
 * connectors to retrieve user profile data from external OAuth providers after
 * successful authentication.
 * </p>
 *
 * @see org.jahia.modules.jahiaoauth.config.JahiaOAuthConfigurationImpl
 * @since 3.4.0
 */
public interface JahiaOAuthConfiguration {
    /**
     * Gets the list of Facebook user information endpoints.
     * <p>
     * These endpoints are used by the Facebook OAuth connector to retrieve
     * user profile information after successful authentication.
     * </p>
     *
     * @return a list of Facebook user info endpoint URLs
     */
    List<String> getFacebookUserInfoEndpoints();

    /**
     * Gets the list of GitHub user information endpoints.
     * <p>
     * These endpoints are used by the GitHub OAuth connector to retrieve
     * user profile information after successful authentication.
     * </p>
     *
     * @return a list of GitHub user info endpoint URLs
     */
    List<String> getGithubUserInfoEndpoints();

    /**
     * Gets the list of LinkedIn user information endpoints.
     * <p>
     * These endpoints are used by the LinkedIn OAuth connector to retrieve
     * user profile information after successful authentication.
     * </p>
     *
     * @return a list of LinkedIn user info endpoint URLs
     */
    List<String> getLinkedinUserInfoEndpoints();

    /**
     * Gets the list of Google user information endpoints.
     * <p>
     * These endpoints are used by the Google OAuth connector to retrieve
     * user profile information after successful authentication.
     * </p>
     *
     * @return a list of Google user info endpoint URLs
     */
    List<String> getGoogleUserInfoEndpoints();
}
