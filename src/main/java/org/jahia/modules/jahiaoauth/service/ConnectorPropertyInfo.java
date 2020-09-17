package org.jahia.modules.jahiaoauth.service;

public class ConnectorPropertyInfo {
    private String name;
    private String valueType;
    private String format;
    private String propertyToRequest;
    private String valuePath;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getPropertyToRequest() {
        return propertyToRequest;
    }

    public void setPropertyToRequest(String propertyToRequest) {
        this.propertyToRequest = propertyToRequest;
    }

    public String getValuePath() {
        return valuePath;
    }

    public void setValuePath(String valuePath) {
        this.valuePath = valuePath;
    }
}
