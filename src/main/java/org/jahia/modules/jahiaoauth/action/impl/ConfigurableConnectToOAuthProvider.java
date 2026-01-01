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
import org.jahia.modules.jahiaoauth.action.ConnectToOAuthProvider;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthService;
import org.osgi.service.component.annotations.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component(
    service = Action.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    configurationPid = "org.jahia.modules.jahiaoauth.connector.actions"
)
public class ConfigurableConnectToOAuthProvider extends ConnectToOAuthProvider {

    private static final String ADDITIONAL_PARAMS_PREFIX = "additionalParams_";

    private boolean randomNonceAdditionalParam;
    private Map<String, String> additionalParams;

    @Activate
    public void activate(Map<String, Object> config) {
        setName((String) config.get("connectToActionName"));
        setRequireAuthenticatedUser(false);
        setRequiredMethods("GET");
        setConnectorName((String) config.get("connectorName"));

        this.additionalParams = extractAdditionalParams(config);
        this.randomNonceAdditionalParam = Boolean.parseBoolean(
            String.valueOf(config.getOrDefault("randomNonceAdditionalParam", "false"))
        );
    }

    @Override
    public Map<String, String> getAdditionalParams() {
        if (!randomNonceAdditionalParam) {
            return additionalParams;
        }

        Map<String, String> params = new HashMap<>(additionalParams);
        params.put("nonce", UUID.randomUUID().toString());
        return params;
    }

    private Map<String, String> extractAdditionalParams(Map<String, Object> config) {
        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            if (entry.getKey().startsWith(ADDITIONAL_PARAMS_PREFIX)) {
                String paramName = entry.getKey().substring(ADDITIONAL_PARAMS_PREFIX.length());
                params.put(paramName, String.valueOf(entry.getValue()));
            }
        }
        return params;
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