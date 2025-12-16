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

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

@Component(configurationPid = "org.jahia.modules.jahiaoauth", service = JahiaOAuthConfiguration.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = JahiaOAuthConfigurationImpl.Config.class)
public class JahiaOAuthConfigurationImpl implements JahiaOAuthConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(JahiaOAuthConfigurationImpl.class);

    private Config config;

    @ObjectClassDefinition(name = "%configName", description = "%configDesc", localization = "OSGI-INF/l10n/config")
    public @interface Config {
        @AttributeDefinition(name = "%facebookUserInfoEndpoints", description = "%facebookUserInfoEndpointsDesc") String facebookUserInfoEndpoints();

        @AttributeDefinition(name = "%gitHubUserInfoEndpoints", description = "%gitHubUserInfoEndpointsDesc") String gitHubUserInfoEndpoints();

        @AttributeDefinition(name = "%googleUserInfoEndpoints", description = "%googleUserInfoEndpointsDesc") String googleUserInfoEndpoints();

        @AttributeDefinition(name = "%linkedInUserInfoEndpoints", description = "%linkedInUserInfoEndpointsDesc") String linkedInUserInfoEndpoints();

    }

    @Activate
    public void activate(Config config) {
        this.config = config;
        logger.info("OAuth connectors configuration activated");
        logEndpoints(config);
    }

    @Modified
    public void modified(Config config) {
        this.config = config;
        logger.info("OAuth connectors configuration modified");
        logEndpoints(config);
    }

    private static void logEndpoints(Config config) {
        logger.debug("Facebook endpoints: {}", config.facebookUserInfoEndpoints());
        logger.debug("GitHub endpoints: {}", config.gitHubUserInfoEndpoints());
        logger.debug("Google endpoints: {}", config.googleUserInfoEndpoints());
        logger.debug("LinkedIn endpoints: {}", config.linkedInUserInfoEndpoints());
    }

    @Override
    public List<String> getFacebookUserInfoEndpoints() {
        return readConfiguration(config.facebookUserInfoEndpoints());
    }

    @Override
    public List<String> getGitHubUserInfoEndpoints() {
        return readConfiguration(config.gitHubUserInfoEndpoints());
    }

    @Override
    public List<String> getLinkedInUserInfoEndpoints() {
        return readConfiguration(config.linkedInUserInfoEndpoints());
    }

    @Override
    public List<String> getGoogleUserInfoEndpoints() {
        return readConfiguration(config.googleUserInfoEndpoints());
    }

    private List<String> readConfiguration(String configurationValue) {
        return Arrays.asList(configurationValue.split(","));
    }
}
