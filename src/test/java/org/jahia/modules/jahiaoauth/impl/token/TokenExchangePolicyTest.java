package org.jahia.modules.jahiaoauth.impl.token;

import com.github.scribejava.apis.openid.OpenIdJsonTokenExtractor;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.OAuth2AccessTokenJsonExtractor;
import com.github.scribejava.core.extractors.TokenExtractor;
import com.github.scribejava.core.model.OAuth2AccessToken;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TokenExchangePolicyTest {

    /**
     * Stands in for the API of a connector. Both questions are read from this object, so a test needs
     * no connector, no configuration and no OSGi container.
     */
    private static DefaultApi20 api(String tokenEndpoint, boolean readsIdentityToken) {
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
                return readsIdentityToken ? OpenIdJsonTokenExtractor.instance()
                        : OAuth2AccessTokenJsonExtractor.instance();
            }
        };
    }

    @Test
    public void shouldSeeThatAProviderReturnsAnIdentityToken() {
        // This is the shape of an OpenID Connect provider: the extractor of its API reads an identity
        // token, so the answer comes from the API and a nonce is sent.
        assertTrue(TokenExchangePolicy.returnsIdentityToken(api("https://idp.example.com/token", true)));
    }

    @Test
    public void shouldSeeThatAProviderReturnsNoIdentityToken() {
        assertFalse(TokenExchangePolicy.returnsIdentityToken(api("https://idp.example.com/token", false)));
    }

    @Test
    public void shouldAskForTheScopeAnIdentityTokenDependsOn() {
        // A provider issues an identity token only when the request asks for the openid scope, and the
        // framework refuses a sign-in that returns no token. An administrator who leaves the field empty
        // would therefore get a broken sign-in, so the requirement is stated here.
        assertEquals("openid", TokenExchangePolicy.scopeFor(api("https://idp/token", true), null));
        assertEquals("openid", TokenExchangePolicy.scopeFor(api("https://idp/token", true), "  "));
        assertEquals("openid email profile",
                TokenExchangePolicy.scopeFor(api("https://idp/token", true), "email profile"));
    }

    @Test
    public void shouldNotAskForTheScopeTwice() {
        assertEquals("openid email",
                TokenExchangePolicy.scopeFor(api("https://idp/token", true), "openid email"));
        assertEquals("email openid",
                TokenExchangePolicy.scopeFor(api("https://idp/token", true), "email openid"));
    }

    @Test
    public void shouldLeaveTheScopeAloneForAProviderThatReturnsNoIdentityToken() {
        // Adding a scope a provider does not know is how a working sign-in stops working.
        assertEquals("public_profile",
                TokenExchangePolicy.scopeFor(api("https://idp/token", false), "public_profile"));
        assertNull(TokenExchangePolicy.scopeFor(api("https://idp/token", false), null));
    }

    @Test
    public void shouldRefuseATokenEndpointServedInClear() {
        // An API may build its endpoint from a server url and a realm rather than hold it under a
        // property. The answer comes from the API either way, so this shape is covered like any other.
        assertFalse(TokenExchangePolicy.isSecureTokenEndpoint(api("http://keycloak:8180/realms/lab/token", true)));
        assertFalse(TokenExchangePolicy.isSecureTokenEndpoint(api("HTTP://keycloak:8180/token", true)));
    }

    @Test
    public void shouldAcceptATokenEndpointServedOverTls() {
        assertTrue(TokenExchangePolicy.isSecureTokenEndpoint(api("https://idp.example.com/token", true)));
    }
}
