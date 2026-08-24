package org.jahia.modules.jahiaoauth.impl;

import com.github.scribejava.apis.openid.OpenIdJsonTokenExtractor;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.TokenExtractor;
import com.github.scribejava.core.model.OAuth2AccessToken;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.Settings;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Drives the refusal itself, and not the rule behind it.
 * <p>
 * The endpoint the rule reads comes from the API that performs the token call, which is the same
 * object for every connector. Driving the refusal is what states that, because a test of the rule
 * alone would hold whatever the endpoint was read from.
 */
public class SecureTokenEndpointTest {

    private static final String CONNECTOR = "OidcConnector";

    private static DefaultApi20 api(String tokenEndpoint) {
        return new DefaultApi20() {
            @Override
            public String getAccessTokenEndpoint() {
                return tokenEndpoint;
            }

            @Override
            protected String getAuthorizationBaseUrl() {
                return "https://idp.example.com/authorize";
            }

            @Override
            public TokenExtractor<OAuth2AccessToken> getAccessTokenExtractor() {
                return OpenIdJsonTokenExtractor.instance();
            }
        };
    }

    private static ConnectorConfig config(boolean allowInsecure) {
        Settings settings = new Settings();
        settings.setSiteKey("digitall");
        if (allowInsecure) {
            settings.getValues(CONNECTOR).setProperty("allowInsecureTokenEndpoint", "true");
        }
        return new ConnectorConfig(settings, CONNECTOR);
    }

    @Test
    public void shouldRefuseATokenEndpointServedInClear() {
        // The identity token is accepted without a signature check, which OpenID Connect Core 3.1.3.7
        // allows over a validated TLS connection alone. The connection is therefore what every claim
        // the validator reads rests on, and this case states that the endpoint has to provide one.
        try {
            JahiaOAuthServiceImpl.requireSecureTokenEndpoint(config(false),
                    api("http://keycloak:8180/realms/lab/protocol/openid-connect/token"));
            fail("Must be refused: the token endpoint is served in clear");
        } catch (JahiaRuntimeException e) {
            // expected
        }
    }

    @Test
    public void shouldAcceptATokenEndpointServedOverTls() {
        JahiaOAuthServiceImpl.requireSecureTokenEndpoint(config(false), api("https://idp.example.com/token"));
    }

    @Test
    public void shouldAcceptATokenEndpointInClearOnADisposableTestInstance() {
        JahiaOAuthServiceImpl.requireSecureTokenEndpoint(config(true), api("http://keycloak:8180/token"));
    }
}
