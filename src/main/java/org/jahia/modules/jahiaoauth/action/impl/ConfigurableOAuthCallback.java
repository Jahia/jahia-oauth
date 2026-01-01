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
package org.jahia.modules.jahiaoauth.action.impl;

import org.jahia.bin.Action;
import org.jahia.modules.jahiaauth.service.SettingsService;
import org.jahia.modules.jahiaoauth.action.OAuthCallback;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthService;
import org.osgi.service.component.annotations.*;

import java.util.Map;

@Component(
    service = Action.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    configurationPid = "org.jahia.modules.jahiaoauth.connector.actions"
)
public class ConfigurableOAuthCallback extends OAuthCallback {

    @Activate
    public void activate(Map<String, Object> config) {
        setName((String) config.get("callbackActionName"));
        setRequireAuthenticatedUser(false);
        setRequiredMethods("GET");
        setConnectorName((String) config.get("connectorName"));
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