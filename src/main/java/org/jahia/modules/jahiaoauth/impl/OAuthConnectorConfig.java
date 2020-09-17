package org.jahia.modules.jahiaoauth.impl;

import org.jahia.modules.jahiaauth.service.MapperConfig;

import java.util.ArrayList;
import java.util.List;

public class OAuthConnectorConfig {

    private String connectorName;
    private String siteKey;
    private boolean active;
    private String apiKey;
    private String apiSecret;
    private String scopes;
    private List<String> callbackUrls = new ArrayList<>();
    private List<MapperConfig> mappers = new ArrayList<>();

    public OAuthConnectorConfig(String connectorName) {
        this.connectorName = connectorName;
    }

    public String getConnectorName() {
        return connectorName;
    }

    public String getSiteKey() {
        return siteKey;
    }

    public void setSiteKey(String siteKey) {
        this.siteKey = siteKey;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setConnectorName(String connectorName) {
        this.connectorName = connectorName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public List<String> getCallbackUrls() {
        return callbackUrls;
    }

    public void setCallbackUrls(List<String> callbackUrls) {
        this.callbackUrls = callbackUrls;
    }

    public List<MapperConfig> getMappers() {
        return mappers;
    }

    public MapperConfig getMapper(String name) {
        for (MapperConfig mapper : mappers) {
            if (mapper.getMapperName().equals(name)) {
                return mapper;
            }
        }
        return null;
    }

    public void setMappers(List<MapperConfig> mappers) {
        this.mappers = mappers;
    }
}
