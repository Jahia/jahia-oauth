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