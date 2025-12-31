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
package org.jahia.modules.jahiaoauth.action.impl;

import org.jahia.bin.Action;
import org.jahia.modules.jahiaauth.service.SettingsService;
import org.jahia.modules.jahiaoauth.action.OAuthCallback;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = Action.class, immediate = true)
public class FacebookOAuthCallback extends OAuthCallback {

    @Activate
    public void activate() {
        setName("facebookOAuthCallbackAction");
        setRequireAuthenticatedUser(false);
        setRequiredMethods("GET");
        setConnectorName("FacebookApi");
    }

    @Reference
    @Override
    public void setJahiaOAuthService(JahiaOAuthService jahiaOAuthService) {
        super.setJahiaOAuthService(jahiaOAuthService);
    }

    @Reference
    @Override
    public void setSettingsService(SettingsService settingsService) {
        super.setSettingsService(settingsService);
    }
}

